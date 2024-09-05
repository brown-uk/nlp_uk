package ua.net.nlp.other.clean

import java.util.regex.Pattern
import java.util.stream.Collectors

import org.languagetool.AnalyzedToken
import org.languagetool.AnalyzedTokenReadings
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.ToString
import ua.net.nlp.bruk.WordReading
import ua.net.nlp.tools.tag.DisambigStats
import ua.net.nlp.tools.tag.DisambigStats.Stat

@PackageScope
@CompileStatic
class SpacingModule {

    private static final String MONTHS = /січня|лютого|березня|квітня|травня|червня|липня|серпня|вересня|жовтня|листопада|грудня/
    private static final Pattern SPACED_MONTHS_REGEX = Pattern.compile(MONTHS.replaceAll(/([а-яіїє])(?=[а-яіїє])/, '$1 '))
    private static final Pattern SPACING_PATTERN = ~ /([а-яіїєґА-ЯІЇЄҐ] ){5,}/

    OutputTrait out
    LtModule ltModule
    DisambigStats disambigStats = new DisambigStats()
    boolean fullSpacing = false
    
    SpacingModule() {
//        disambigStats.loadDisambigStats()
    }

    String cleanupSpacing(String text) {
        text = text.replace("У к р а ї н и", "України")
        text = text.replaceAll(/([0-9])\h+р о к у\b/, '$1 року')
        text = text.replaceAll(/([0-9])\h+г о д и н а\b/, '$1 година')
        text = text.replaceAll(SPACED_MONTHS_REGEX, { String w1 ->
            w1.replace(' ', '')
        })
        
        // stenograms from Rada
        text = text.replaceAll(/[СсCc] е с і й н и й\h+з а л\h+В е р х о в н о ї\h+Р а д и/, "Сесійний зал Верховної Ради")
        text = text.replace("О п л е с к и", "Оплески")
        text = text.replaceAll(/П і с л я\h+п е р е р в и/, "Після перерви")
        text = text.replaceAll(/Л у н а є\h+Г і м н/, "Лунає Гімн")
        text = text.replaceAll(/Л у н а є\h+Д е р ж а в н и й\h+Г і м н/, "Лунає Державний Гімн")
        text = text.replace("п о с т а н о в л я є", "постановляє")
        text = text.replaceAll(/Х в и л и н а\h+м о в ч а н н я/, "Хвилина мовчання")
        text = text.replaceAll(/Й д е\h+р е є с т р а ц і я/, "Йде реєстрація")
        text = text.replaceAll(/Ш у м\h+у\h+з а л і/, "Шум у залі")


        text = text.replaceAll(/\b([гГ])\h+([а-яіїєґ'\u2019\u02bc-]{3,})/, { all, g, rest ->
            if( ! ltModule.knownWord(rest) ) {
                def newWord = "$g$rest"
                if( ltModule.knownWord(newWord) ) {
                    return newWord
                }
            }
            all
        })
        
        def m = SPACING_PATTERN.matcher(text)
        if( m.find() ) {
            
            def lines = text.lines()
            long cnt = lines.filter{l -> SPACING_PATTERN.matcher(l).find()}.count()
            
            out.println "\tWARNING: Possible spacing in words, e.g \"${m.group(0)}\": ${cnt} of ${text.lines().count()} lines"

            if( fullSpacing ) {
                text = removeSpacing(text)
            }
        }
        text
    }
    
    String removeSpacing(String text) {
        text = text.replaceAll(/\. (?=\.)/, '.')
        
//        def t = text.readLines()
//            .parallelStream()
//            .map{ removeSpacingLine(it) }
//            .collect(Collectors.joining("\n"))

        def t = new StringBuilder(text.length())
        text.readLines().eachWithIndex { l, idx ->
            debug "line $idx :: $l"
            t.append( removeSpacingLine(l) ).append('\n')
        }
                
        if( ! text.endsWith("\n") ) {
            t.deleteCharAt(t.length()-1)
//            t = "$t\n"
        }
        
        t.toString()
    }
    
    String removeSpacingLine(String text) { 
        
        List<String> chunks = MarkLanguageModule.splitWithDelimiters(text, ~ /(?iu)[^ .а-яіїєґa-z0-9\u0301'\u2019\u02BC\u2013\u2011-]+/)

        chunks = (List<String>)chunks.collect{ it.split(/  +/) as List }.flatten()
        
        debug "chunks size: ${chunks.size()}"
        
        List<String> chunks2 = (List<String>)chunks.collect { String cnk ->
            List<String> newChunks = []
            def sb = new StringBuilder(512)
            cnk.eachWithIndex { String ss, int idx ->
                char ch = ss.charAt(0)
                sb.append(ch)
                if( (ch == '.') // && ! (cnk[idx..-1] =~ /^ ?\./)) 
                        || cnk[idx..-1] =~ /^[а-яіїєґ] ?[А-ЯІЇЄҐ]/ ) {
                    newChunks << sb.toString()
                    sb = new StringBuilder(512)
                }
            }
            if( sb ) {
                newChunks << sb.toString()
            }
            newChunks
        }
        .flatten()

        debug "chunks2 size: ${chunks2.size()}"
        
        def dags = chunks2.parallelStream().map { c ->
            
            if( c.trim() && c =~ /(?iu)[а-яіїєґa-z0-9]/ && ! (c =~ /- ?$/)) {
                debug "chunk: $c"
                def noSpaces = c.replace(' ', '')
                def dags = getDag(noSpaces, '', [])
//                debug "Got dags: ${dags}"
                
                if( dags.size() == 0 ) {
                    println "\tFailed to merge: $c"
                    return c
                }
                
                return pickTheDag(dags)
            }
            else {
                if( c == '\n' ) {
                    debug "-- NL --"
                }
                else {
                    debug c
                }
                c
            }
        }
        .toList()
        debug "-- $dags"
     
        def sb = new StringBuilder(1024)
        dags.each { 
            if( sb && ! Character.isWhitespace(sb.charAt(sb.length()-1)) && ! noSpace(it.charAt(0)) ) {
                sb.append(" ")
            }
            sb.append(it)
        }
        
        return sb
    }
    
    static boolean noSpace(char ch) {
        ".,:;»)] \n\t".indexOf((int)ch) >= 0
    } 
    
    List<Node> getDag(String text, String indent, List<String> prevs) {
        debug "$indent got: $text"
        
        List<Node> nodes = []
        
        assert text
        
        for(int ii=text.length()-1; ii>=0; ii--) {
            String curr = text[0..ii]
            
            if( curr == "." ) {
                nodes << new Node(word: curr, children: [])
                return nodes
            }
            
            // 4 1-letter words is not good
            if( curr.length() == 1 
                    && prevs.size() > 3
                    && prevs[-1].length() == 1
                    && prevs[-2].length() == 1
                    && prevs[-3].length() == 1 )
                return nodes
            
            if( goodWord(curr) ) {
//                println "$indent$curr"
                if( ii < text.length()-1 ) {
                    def others = getDag(text[ii+1..-1], "$indent  ", prevs + curr)
//                    def status = others == null ? "<dead>" : ""
//                    debug "$indent  :${ others == null || others.size() == 0 ? '<--' : others}"
                    if( ! others /*== null*/ )
                        continue
                        
                    nodes << new Node(word: curr, children: others)
                }
                else { // end of the chunk
                    nodes << new Node(word: curr, children: [])
                    debug "$indent  +++"
                    return nodes
                    // this DAG is done
                }
            }
            else {
                if( curr.length() == 1 )
                    return nodes
            }
            
        }
        
        nodes
    }

    boolean goodWord(String word) {
        
        int maxLen = word.contains("-") ? 45 : 30
        if( word.length() >= maxLen )
            return false
        
        List<AnalyzedToken> tokens = ltModule.tagWord(word)
        for(AnalyzedToken token: tokens) {
            if( ! token.hasNoTag() ) {
                if( (word.length() > 1 || ! token.getPOSTag().contains("abbr")) 
                        && ! (token.getPOSTag() =~ /^noun:inanim:.:v_kly/) )
                    return true
            }
        }
        return false
    }
    
    static class St {
        String dag
        double rate
        String toString() {
            "$rate ${dag.replaceAll(/[\n\r]+/, '\n')}"
        }
    }

    int nested = 0
    
    List<String> getText(Node node, String parentBase) {
        if( parentBase.length() > nested ) {
            nested = parentBase.length()
            debug "Nested: $nested"
        }
        
        List<String> out = []
        String currBase = "$parentBase${node.word}"
        if( node.children ) {
            node.children.each { ch ->
                out += getText(ch, "${currBase} ")
            }
        }
        else {
            out << currBase
        }
        
        return out.collect{ it.replaceAll(/ +([,.:;»“)\]\n\t])/, '$1') }
    }
    
    String pickTheDag(List<Node> dags) {
//        if( dags.size() == 1 && ! dags[0].children )
//            return getText(dags[0].word, "")
        
        List<String> dagTexts = (List<String>)dags.collect{ getText(it, "") }.flatten()
        def sb = new StringBuilder(256)
        
        def rated = dagTexts.collect { txt ->
            def tokens = ltModule.tagSent(txt)
            def sum = tokens.sum{ t -> /*rate(t)*/ Math.pow(t.cleanToken.length(), 3) } as Double
            def rate = sum / Math.pow(tokens.size(), 3)
            new St(rate: rate, dag: txt)
        }
//        println "rated: $rated"
        rated.max { it.rate }.dag
    }
        
    double rate(AnalyzedTokenReadings readings) {
        def cleanToken = readings.getCleanToken()
        
        if( ! disambigStats.statsByWord.containsKey(cleanToken) ) {
            // if no stats and there are non-prop readings, then try lowercase
            if( disambigStats.UPPERCASED_PATTERN.matcher(cleanToken).matches()
                    && readings.size() > readings.count{ r -> r.getPOSTag() == null || r.getPOSTag().contains(":prop") }  ) {
                cleanToken = cleanToken.toLowerCase()
            }
        }
        
        Map<WordReading, Stat> statsForWord = disambigStats.statsByWord[cleanToken]
        
//        assert statsForWord, "no status for $cleanToken"
        
        return statsForWord ? statsForWord.max { it.value.rate }.value.rate : 0
    }
    
    static class Node {
        String word
        List<Node> children
        
        String toString() {
            "$word $children"
        }
        
        String toText() {
            def next = children ? " ${children[0].toText()}"  : ""
            "$word$next"
        }
    }

    
    private static void debug(String text) {
        //println "DBG: $text"
    }    
}
