package ua.net.nlp.tools.tag;

import java.util.regex.Pattern

import org.languagetool.AnalyzedTokenReadings
import org.languagetool.tools.StringTools

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import ua.net.nlp.bruk.WordReading
import ua.net.nlp.tools.tag.TagTextCore.TaggedToken


@CompileStatic
public class TagUnknown {
    private static final String statsFile = "/ua/net/nlp/tools/stats/lemma_suffix_freqs.txt"

    Map<String, Map<WordReading, Integer>> lemmaSuffixStatsF = [:].withDefault { [:].withDefault { 0 } }
    int lemmaSuffixLenB = 4
        
    TagUnknown() {
    }

    @CompileDynamic
    void loadStats() {
        if( lemmaSuffixStatsF.size() > 0 )
            return
        
        def statsFileRes = getClass().getResource(statsFile)
        assert statsFileRes, "Disambig stats not found :$statsFile"

        statsFileRes.eachLine { String line ->
            def (suffix, rs, postag, cnt) = line.split("\t+")
            def wr = new WordReading(rs, postag)
            lemmaSuffixStatsF[suffix] << [ (wr) : cnt as Integer]
        }
    }
        
//    private static Pattern DASHED = ~/(?iu)([а-яіїєґ']{4,})-([а-яіїєґ']{4,})/
    
    List<TaggedToken> tag(String token, int idx, AnalyzedTokenReadings[] tokens) {
//        def m = DASHED.matcher(token)
//        m.find()
//        if( m ) {
//            String part1 = m.group(1)
//            String part2 = m.group(2)
//            
//            return tagInternal(part1, idx, tokens)
//        }
        try {
            return tagInternal(token, idx, tokens)
        }
        catch(Exception e) {
            System.err.println "Failed to find unknown for \"$token\""
            e.printStackTrace()
            return null
        }
    }

    // НС-фільтрів
    static final Pattern PREFIXED = Pattern.compile(/([А-ЯІЇЄҐA-Z0-9]+[-\u2013])([а-яіїєґ].*)/)
    
    List<TaggedToken> tagInternal(String token, int idx, AnalyzedTokenReadings[] tokens) {
        if( token ==~ /[А-ЯІЇЄҐ]+-[0-9]+[а-яіїєґА-ЯІЇЄҐ]*/ ) // ФАТ-10
            return [new TaggedToken(value: token, lemma: token, tags: 'noninfl', confidence: -0.7)]
        if( token ==~ /[А-ЯІЇЄҐ]{2,6}/ )
            return [new TaggedToken(value: token, lemma: token, tags: 'noninfl:abbr', confidence: -0.7)]

        def m = PREFIXED.matcher(token)
        if( m.matches() ) {
            String left = m.group(1)
            String right = m.group(2)
            
            def tagged = tagInternal(right, idx, tokens)
            tagged.each { tt ->
                tt.lemma = "$left${tt.lemma}" 
                tt.value = "$left${tt.value}" 
            }
            return tagged
        }

        int lemmaSuffixLen = token.endsWith("ться") ? lemmaSuffixLenB + 2 : lemmaSuffixLenB
                
        if( token.length() < lemmaSuffixLen + 2 )
            return []
        
        def last3 = token[-lemmaSuffixLen..-1]
        
        List<TaggedToken> retTokens
        
        Map<WordReading, Integer> opToTagMap = lemmaSuffixStatsF[last3]
        opToTagMap = opToTagMap.findAll { e -> getCoeff(e, token, idx, tokens) > 0 }
        
        if( opToTagMap ) {
            opToTagMap = opToTagMap.toSorted { e -> - getCoeff(e, token, idx, tokens) }
            
            retTokens = opToTagMap.collect { Map.Entry<WordReading, Integer> e ->
                def wr = e.key 
                def parts = wr.lemma.split("/")
                int del = parts[1] as int
                
                if( del + 2 > token.length() )
                    return (TaggedToken)null
                
                String add = parts[0]
                def lemma = token[0..-del-1] + add

                def q = opToTagMap.size() > 1 ? -0.5 : -0.6

                if( ! wr.postag.contains(":prop") ) {
                    lemma = lemma.toLowerCase()
                }

                return new TaggedToken(value: token, lemma: lemma, tags: wr.postag, confidence: q)
            }
            .findResults() as List
        }
        else {
            retTokens = []
        }

        return retTokens
    }
    
    static Pattern mascPrefix = ~/пан|містер|гер|сеньйор|монсеньйор|добродій|князь/
    static Pattern femPrefix = ~/пані|міс|місіс|княгиня|фрау|сеньора|сеньйоріта|мадам|маде?муазель|добродійка/

    private static String gen(String postag) {
        def m = postag =~ /:[mf]:/
        return m ? m[0] : null
    }

        
    private static int getCoeff(Map.Entry<WordReading, Integer> e, String token, int idx, AnalyzedTokenReadings[] tokens) {
        if( e.key.postag.contains("prop") ) {
            if( ! StringTools.isCapitalizedWord(token) ) {
                return 0
            }
            
            if( idx > 0 ) {
                if( e.key.postag.contains("lname") ) {
                    def eGen = gen(e.key.postag)
                    if( eGen ) { 
                        if( tokens[idx-1].getReadings().find {
                            def tag = it.getPOSTag()
                            if( tag == null ) return false
                            if( tag.contains("fname") && gen(tag) == eGen ) return true
                            
                            it.getLemma() != null &&
                            ((eGen == ":m:" && mascPrefix.matcher(it.getLemma()).matches()) ||
                            (eGen == ":f:" && femPrefix.matcher(it.getLemma()).matches()))
                        } ) {
                            return e.value * 10000
                        }
                    }
                }
                return e.value * 100
            }
            // Зів-Кріспель
            else if( token =~ /[а-яіїєґ]-[А-ЯІЇЄҐ][а-яіїєґ']/ ) {
                return e.value * 100
            }
        }
        return e.value
    }
    
}
