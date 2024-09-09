package ua.net.nlp.tools.tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.mutable.MutableInt;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.rules.uk.LemmaHelper
import ua.net.nlp.bruk.ContextToken
import ua.net.nlp.bruk.WordContext;
import ua.net.nlp.bruk.WordReading;
import ua.net.nlp.tools.tag.TagOptions

import groovy.transform.CompileStatic;


@CompileStatic
public class SemTags {
    static final String baseDir = "/ua/net/nlp/dict_uk/semtags"

    def categories = ["noun", "adj", "adv", "verb", "numr"]
        
    TagOptions options
    Map<String, Map<String,List<String>>> semanticTags = new HashMap<>()
    

    def loadSemTags() {
        if( semanticTags.size() > 0 )
            return

        long tm1 = System.currentTimeMillis()
        int semtagCount = 0
        
        categories.each { cat ->

            def fileName = "$baseDir/${cat}.csv"
            def csvFile = getClass().getResource(fileName)
            assert csvFile, "$fileName not found"
            
            String text = csvFile.getText('UTF-8')
            
            if( text.startsWith("\uFEFF") ) {
                text = text.substring(1)
            }

            text.eachLine { line ->
                line = line.trim().replaceFirst(/\s*#.*/, '')
                if( ! line )
                    return

                def parts = line.split(',')
                if( parts.length < 2 ) {
                    System.err.println("skipping invalid semantic tag for: \"$line\"")
                    return
                }

                def add = ""
                if( parts.length >= 3 && parts[2].trim().startsWith(':') ) {
                   add = parts[2].trim()
                }
                def semtags = parts[1]
                def key = parts[0] + " " + cat
                
                if( ! (key in semanticTags) ) {
                    semanticTags[key] = [:]
                }
                if( ! (add in semanticTags[key]) ) {
                    semanticTags[key][add] = []
                }

                // semtags sometimes have duplicate lines
                if( ! (semtags in semanticTags[key][add]) ) {
                    semanticTags[key][add] << semtags
                    semtagCount += 1
                }
            }
        }

        if( ! options.quiet ) {
            long tm2 = System.currentTimeMillis()
            System.err.println("Loaded $semtagCount semantic tags for ${semanticTags.size()} lemmas in ${tm2-tm1} ms")
        }
    }


    String getSemTags(AnalyzedToken tkn, String posTag) {
        if( options.semanticTags && tkn.getLemma() != null && posTag != null ) {
            def lemma = tkn.getLemma()
            String posTagKey = posTag.replaceFirst(/:.*/, '')
            String key = "$lemma $posTagKey"

            def potentialSemTags = semanticTags.get(key)
            if( potentialSemTags ) {
                Map<String, List<String>> potentialSemTags2 = semanticTags.get(key)
                List<String> potentialSemTags3 = null
                if( potentialSemTags2 ) {
                    potentialSemTags2 = potentialSemTags2.findAll { k,v -> !k || posTag.contains(k) }
                    List<String> values = (java.util.List<java.lang.String>) potentialSemTags2.values().flatten()
                    potentialSemTags3 = values.findAll { filterSemtag(lemma, posTag, it) }
                    return potentialSemTags3 ? potentialSemTags3.join(';') : null
                }
            }
        }
        return null
    }

    
    private static boolean filterSemtag(String lemma, String posTag, String semtag) {
        if( posTag.contains("pron") )
            return semtag =~ ":deictic|:quantif"

        if( posTag.startsWith("noun") ) {
            
            if( Character.isUpperCase(lemma.charAt(0)) && posTag.contains(":geo") ) {
                return semtag.contains(":loc")
            }

            if( posTag.contains(":anim") ) {
                if( posTag.contains("name") )
                    return semtag =~ ":hum|:supernat"
                else
                    return semtag =~ ":hum|:supernat|:animal"
            }

            if( posTag.contains(":unanim") )
                return semtag.contains(":animal")

            return ! (semtag =~ /:hum|:supernat|:animal/)
        }
        true
    }
    
}
