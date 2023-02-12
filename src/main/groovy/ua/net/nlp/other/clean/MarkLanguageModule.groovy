package ua.net.nlp.other.clean

import java.nio.charset.StandardCharsets
import java.util.regex.Matcher
import java.util.regex.Pattern

import org.languagetool.AnalyzedToken
import org.languagetool.tagging.ru.RussianTagger

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import ua.net.nlp.other.clean.CleanOptions.MarkOption
import ua.net.nlp.other.clean.CleanOptions.ParagraphDelimiter
import ua.net.nlp.other.clean.CleanTextCore.CleanRequest

@PackageScope
class MarkLanguageModule {
    
    static final Pattern pattern = ~/(?s)<span lang="ru"( rate="[0-9.]+")?>(?!---<\/span>)(.*?)<\/span>/

    CleanOptions options
    OutputTrait out
    LtModule ltModule
    
    @CompileStatic
    class ParaIterator implements Iterator<String> {
        String text;
        String delim;
        int from = 0
        
        @Override
        public boolean hasNext() {
            return from < text.length()
        }
        
        @Override
        public String next() {
            if( from == text.length()) throw new IllegalArgumentException()
            
            int pos = text.indexOf(delim, from)
            if( pos == -1 ) pos = text.length()
            def ret = pos == from ? delim : text[from..<pos];
            from = pos
//            debug "new pos: $from (total: ${text.length()} ret: $ret"
            if( ret == delim ) from += delim.length()
            return ret
        }
    }
    
    @CompileStatic
    String markRussian(CleanRequest request, String outDirName) {
        // clean previous marks unless they are cut
        def text = pattern.matcher(request.text).replaceAll('$2')
        
        // by paragraphs now
//      String[] chunks = text.split(/\n\n/) // ukSentTokenizer.tokenize(text)
        String delim
        if( options.paragraphDelimiter == ParagraphDelimiter.auto ) {
            delim = (text =~ /[^\s]\n\n+[^\s]/) ? "\n\n" : "\n"
            int nls = delim.length()
            out.println "\tdetected paragraph type: $nls new lines"
        }
        else {
            delim = options.paragraphDelimiter == ParagraphDelimiter.single_nl ? "\n" : "\n\n"
        }
        def chunks = new ParaIterator(text: text, delim: delim)

        def ruChunks = []
        
        text = chunks
            .collect { String sent ->
                if( ! (sent =~ /[а-яіїєґёА-ЯІЇЄҐЁ]/) ) {
                    return sent
                }
                
                List<Double> vals = evalChunk(sent)
                Double ukRate = vals[0], ruRate = vals[1]
                
                if( ukRate < ruRate ) {
                    ruRate = Math.round(ruRate * 100d)/100d
                    String marked = "<span lang=\"ru\" rate=\"${ruRate}\">$sent</span>".replaceFirst(/(?s)([\h\v]+)(<\/span>)$/, '$2$1')
                    if( options.markLanguages == MarkOption.mark ) {
                        marked
                    }
                    else {
                        ruChunks << marked
                        '<span lang="ru">---</span>'
                    }
                }
                else {
                    sent
                }
        
            }
            .join("")
        
        if( options.markLanguages == MarkOption.cut ) {
            if( ruChunks && request.file ) {
                String ruText = ruChunks.join("\n\n")
                def ruFile
                if( outDirName ) {
                    def ruFilename = request.file.name.replaceFirst(/\.txt/, '.ru.txt')

                    def parentDir = request.outFile.getParentFile()
                    def ruDir = new File(parentDir, "ru")
                    ruDir.mkdirs()

                    ruFile = new File(ruDir, ruFilename)
                }
                else {
                    ruFile = new File(request.outFile.absolutePath.replaceFirst(/\.txt/, '.ru.txt'))
                }
                ruFile.setText(ruText, StandardCharsets.UTF_8.name())
            }
        }

        text
    }

    @CompileStatic
    List<Double> evalChunk(String text) {
        
//        double ukCnt = 0
        int ruCnt = 0
        int totalCnt = 0
        int ruCharCnt = 0

        def chunks = ltModule.ukWordTokenizer.tokenize(text)
            .findAll{ it && it ==~ /(?ius)[а-яіїєґё'\u2019\u02BC\u0301-]+/ }
            .collect { it.replaceAll(/^['\u2019\u02BC]+|['\u2019\u02BC]+$/, '') }
            .findAll { it ==~ /(?ius)[а-яіїєґё'\u2019\u02BC\u0301-]+/ }

        if( chunks.isEmpty() )
            return [(double)1.0, (double)0.0]
        
        // Лариса ГУТОРОВА
        if( chunks.size() < 10 ) {
            if( ! (text =~ /[ыэъё]/) || text =~ /[ієїґ]/ ) {
                return [(double)0.5, (double)0.1]
            }
        }
                
        int ukSum = 0
        int ruSum = 0
        int ukSum10 = 0
        int ruSum10 = 0
        for(String w: chunks) {
            
            int ukWeight = getUkWordRating(w)
            ukSum += ukWeight
            if( ukWeight == 10 ) {
                ukSum10 += ukWeight
            }
            def debugStr = "$w: uk: $ukWeight"
            if( ukWeight < 10 ) {
                int ruWeight = w =~ /(?iu)[ыэёъ]/ ? 10 : ltModule.knownWordRu(w) ? 8 : 0
                
                // workaround for 1-letter abbreviations that Russian tagger claims to know
                if( w ==~ /(?iu)[бвгдежзклмнпрстуфхцчшщю]/ ) {
                    ruWeight = 0
                }
                 
                ruSum += ruWeight

                if( ruWeight == 10 ) {
                    ruSum10 += ruWeight
                }
                debugStr += ", ru: $ruWeight"
            }
//            println debugStr
        }

        double ukRate = (double)ukSum / chunks.size() / 10
        double ruRate = (double)ruSum / chunks.size() / 10
        
        if( ruSum10 > 0 && ukSum10 == 0 ) {
            ruRate = 1
        }

        //println ":: uk: $ukRate ru: $ruRate words: ${chunks.size()}"

        if( ukRate == 0 && ruRate > 0.1 ) {
            ruRate = 1
        }

//        println "check:: '${text.trim()}' : $ukRate, $ruRate"
        
        [ukRate, ruRate]
    }

    @CompileStatic
    private static List<String> splitWithDelimiters(String str, Pattern delimPattern) {
        List<String> parts = new ArrayList<String>();
    
        Matcher matcher = delimPattern.matcher(str);
    
        int lastEnd = 0;
        while (matcher.find()) {
          int start = matcher.start();
    
          if (lastEnd != start) {
            String nonDelim = str.substring(lastEnd, start);
            parts.add(nonDelim);
          }
    
          String delim = matcher.group();
          parts.add(delim);
    
          lastEnd = matcher.end();
        }
    
        if (lastEnd != str.length()) {
          String nonDelim = str.substring(lastEnd);
          parts.add(nonDelim);
        }
    
        return parts;
    }

    
    int getUkWordRating(String word) {
        if( word =~ /(?iu)[іїєґ'\u2019\u02BC]|^й$/ )
            return 10
        
        try {
            List<AnalyzedToken> tokenReadings = ltModule.ukTagger.getAnalyzedTokens(word)
            if( tokenReadings[0].hasNoTag() )
                return 0

            def badToken = tokenReadings.find { AnalyzedToken t ->
                    t.getPOSTag().contains(":bad") && ! t.getPOSTag().contains("&adjp:actv") && ! (t.getLemma() =~ /(ння|ий)$/) }
            if( badToken ) {
                def nonBadToken = badToken = tokenReadings.find { AnalyzedToken t -> ! t.getPOSTag().contains(":bad") }
                if( ! nonBadToken )
                    return 2
            }
//            if( token.find { AnalyzedToken t -> t.getPOSTag() =~ /:prop:geo|noun:inanim:.:v_kly/ } )
//                return 5
            return 8
        }
        catch (Exception e) {
            System.err.println("Failed on word: " + word)
            throw e
        }
    }

}
