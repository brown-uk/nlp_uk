package ua.net.nlp.tools.tag;

import org.codehaus.groovy.util.StringUtil
import org.languagetool.AnalyzedTokenReadings
import org.languagetool.tools.StringTools

import groovy.transform.CompileStatic
import ua.net.nlp.bruk.WordReading
import ua.net.nlp.tools.tag.TagTextCore.TaggedToken

public class TagUnknown {
    private static final String statsFile = "/ua/net/nlp/tools/stats/lemma_suffix_freqs.txt"

    Map<String, Map<WordReading, Integer>> lemmaSuffixStatsF = [:].withDefault { [:].withDefault { 0 } }
    int lemmaSuffixLenB = 4
        
    TagUnknown() {
    }

    void loadStats() {
        if( lemmaSuffixStatsF.size() > 0 )
            return
        
        def statsFileRes = getClass().getResource(statsFile)
        if( statsFileRes == null ) {
            System.err.println "Lemma stats not found, run with --download to download it from github"
            System.exit 1
        }

        statsFileRes.eachLine { String line ->
            def (suffix, rs, postag, cnt) = line.split("\t+")
            def wr = new WordReading(rs, postag)
            lemmaSuffixStatsF[suffix] << [ (wr) : cnt as Integer]
        }
    }
        
    @CompileStatic
    TaggedToken tag(String token, int idx, AnalyzedTokenReadings[] tokens) {
        
        if( token ==~ /[А-ЯІЇЄҐ]{2,6}/ )
            return new TaggedToken(value: token, lemma: token, tags: 'noninfl:abbr', q: -0.7)
    
        int lemmaSuffixLen = token.endsWith("ться") ? lemmaSuffixLenB + 2 : lemmaSuffixLenB
                
        if( token.length() < lemmaSuffixLen + 2 )
            return null
        
        def last3 = token[-lemmaSuffixLen..-1]
        
        def opToTagMap = lemmaSuffixStatsF[last3]
        opToTagMap = opToTagMap.findAll { e -> getCoeff(e, token, idx, tokens) > 0 }
        if( opToTagMap ) {
            def wr = opToTagMap.max{ e -> getCoeff(e, token, idx, tokens) }.key
//            println ":: ${opToTagMap}"
//            println ":: max: ${wr}"
            def parts = wr.lemma.split("/")
            int del = parts[1] as int
            String add = parts[0]
            def lemma = token[0..-del-1] + add 
            
            def q = opToTagMap.size() > 1 ? -0.5 : -0.6
            
            return new TaggedToken(value: token, lemma: lemma, tags: wr.postag, q: q)
        }
        return null
    }
    
    @CompileStatic
    private static int getCoeff(Map.Entry<WordReading, Integer> e, String token, int idx, AnalyzedTokenReadings[] tokens) {
        if( e.key.postag.contains("prop") ) {
            if( ! StringTools.isCapitalizedWord(token) ) {
                return 0
            }
            else if( idx > 0 ) {
                if( e.key.postag.contains("lname")
                        && tokens[idx-1].getReadings().find{ it.getPOSTag() != null && it.getPOSTag().contains("fname")} ) {
                    return e.value * 500
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
