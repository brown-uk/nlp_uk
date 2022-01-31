package org.nlp_uk.tools.tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.mutable.MutableInt;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.rules.uk.LemmaHelper
import org.nlp_uk.bruk.ContextToken
import org.nlp_uk.bruk.WordContext;
import org.nlp_uk.bruk.WordReading;
import org.nlp_uk.tools.TagText.TagOptions
import org.nlp_uk.tools.TagText.TagOptions.DisambigModule

import groovy.transform.CompileStatic;

public class SemTags {

    TagOptions options
    Map<String, Map<String,List<String>>> semanticTags = new HashMap<>()
    
    @CompileStatic
    def loadSemTags() {
        if( semanticTags.size() > 0 )
            return

        // def base = System.getProperty("user.home") + "/work/ukr/spelling/dict_uk/data/sem"
        String base = "https://raw.githubusercontent.com/brown-uk/dict_uk/master/data/sem"
        def semDir = new File("sem")
        if( semDir.isDirectory() ) {
            base = "${semDir.path}"
            System.err.println("Loading semantic tags from ./sem")
        }
        else {
            System.err.println("Loading semantic tags from $base")
        }

        long tm1 = System.currentTimeMillis()
        int semtagCount = 0
        ["noun", "adj", "adv", "verb", "numr"].each { cat ->
            String text = base.startsWith("http")
                ? "$base/${cat}.csv".toURL().getText("UTF-8")
                : new File(semDir, "${cat}.csv").getText("UTF-8")

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

    @CompileStatic
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

    
    @CompileStatic
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
