package ua.net.nlp.other.clean

import java.util.function.Function
import java.util.regex.MatchResult
import java.util.regex.Matcher
import java.util.regex.Pattern

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@PackageScope
@CompileStatic
class HyphenModule {
    
    private final Pattern SOFT_HYPHEN_PATTERN1 = Pattern.compile(/([а-яіїєґА-ЯІЇЄҐa-zA-Z'ʼ’]\]\)]?)\u00AD+(\n[ \t]*)([а-яіїєґА-ЯІЇЄҐa-zA-Z'ʼ’-]+)([,;.!?])?/)
    private final Pattern SOFT_HYPHEN_PATTERN2 = Pattern.compile(/([а-яіїєґА-ЯІЇЄҐa-zA-Z'ʼ’:. ])\u00AD+([а-яіїєґА-ЯІЇЄҐa-zA-Z'ʼ’ -])/)
    
    OutputTrait out
    LtModule ltModule
    
    @CompileStatic
    String removeSoftHyphens(String text) {
        if( text.contains("\u00AD") ) {
            out.println "\tremoving soft hyphens: "
//            text = text.replaceAll(/[ \t]*\u00AD[ \t]*([а-яіїєґА-ЯІЇЄҐ'ʼ’-]+)([,;.!?])?/, '$1$2')
//            text = text.replaceAll(/\u00AD(?!\n {10,}[А-ЯІЇЄҐ])(\n?[ \t]*)([а-яіїєґА-ЯІЇЄҐ'ʼ’-]+)([,;.!?])?/, '$2$3$1')
            text = SOFT_HYPHEN_PATTERN1.matcher(text).replaceAll('$1$3$4$2')
            text = SOFT_HYPHEN_PATTERN2.matcher(text).replaceAll('$1$2')
//            text = text.replaceAll(/(?i)([А-ЯІЇЄҐ:. ])\u00AD+([А-ЯІЇЄҐ'ʼ’ -])/, '$1$2')
//            text = text.replaceAll(/([А-ЯІЇЄҐA-Z])\u00AD(\n[ \t]*)([А-ЯІЇЄҐA-Z'ʼ’-]+)([,;.!?])?/, '$1$3$4$2')
           // text = text.replace('\u00AD', '-')
        }
        text = remove00ACHyphens(text)
        return text
    }

    private final Pattern AC_HYPHEN_PATTERN1 = Pattern.compile(/([а-яіїєґА-ЯІЇЄҐ'ʼ’-]*[а-яіїєґА-ЯІЇЄҐ])\u00AC([а-яіїєґА-ЯІЇЄҐ][а-яіїєґА-ЯІЇЄҐ'ʼ’-]*)/)
    
    @CompileStatic
    String remove00ACHyphens(String text) {
        def t0 = text
        if( t0.contains("\u00AC") ) { // ¬
            out.println "\tremoving U+00AC hyphens: "
            def t1 = t0.replaceAll(/([0-9])\u00AC([а-яіїєґА-ЯІЇЄҐ0-9])/, '$1-$2')
// t0 = null // ml
            def m2 = AC_HYPHEN_PATTERN1.matcher(t1)
// t1 = null // ml
            def t2 = m2.replaceAll( new Function<MatchResult, String>() { String apply(MatchResult mr) {
//            text = AC_HYPHEN_PATTERN1.matcher(text).replaceAll({ String all, w1, w2 ->
                def w1 = mr.group(1)
                def w2 = mr.group(2)
                def fix = "$w1-$w2"
                if( ltModule.knownWord(fix) ) return fix
                fix = "$w1$w2"
                if( ltModule.knownWord(fix) ) return fix
                return mr.group(0)
            } } )
            t0 = t2
// t2 = null // ml
        }
        return t0
    }


    @CompileStatic
    String fixDanglingHyphens(String text) {
        if( text.contains("-\n") && text =~ /[а-яіїєґА-ЯІЇЄҐ]-\n/ ) {
            out.println "\tsuspect word wraps"
            def cnt = 0
            int cntWithHyphen = 0

            // e.g.: депутат-\n«мажоритарник»
            text = text.replaceAll(/([а-яіїєґА-ЯІЇЄҐ-]+)-\n([ \t]*)([«„"][а-яіїєґ'ʼ’-]+[»“"])([,;.!?])?/, { List<String> it ->
                cntWithHyphen += 1
                it[1] + "-" + it[3] + (it[4] ?: "") + "\n" + it[2]
            })

            def first = null
            text = text.replaceAll(/([а-яіїєґА-ЯІЇЄҐ'ʼ’-]+)-\n([ \t]*)([а-яіїєґА-ЯІЇЄҐ'ʼ’-]+)([,;.!?])?/, { List<String> it ->
                if( ! first )
                    first = it[0] ? it[0].replace('\n', "\\n") : it[0]
                //            println "== " + (it[1] + "-" + it[3]) + ", known: " + knownWord(it[1] + "-" + it[3])
                // consider words with two or more hyphens with one of them before end of line to be rare
                boolean knownWithHyphen = ltModule.knownWord(it[1] + "-" + it[3]) && ! isHyphenBadLemma(it[3])
                if( knownWithHyphen )
                    return it[1] + "-" + it[3] + (it[4] ?: "") + "\n" + it[2]
                
                if( ltModule.knownWord(it[1] + it[3]) ) {
                    cnt += 1
                    //                print "."
                    it[1] + it[3] + (it[4] ?: "") + "\n" + it[2]
                }
                else {
                    it[0]
                }
            })

            out.println "\t\t$cnt word wraps removed, $cntWithHyphen newlines after hyphen removed"
            if( cnt == 0 && cntWithHyphen == 0 ) {
                out.println "\t\tfirst match: \"$first\""
            }
        }

        if( text =~ /¬ *\n/ ) {
            out.println "\tsuspect word wraps with ¬:"
            text = text.replaceAll(/([а-яіїєґА-ЯІЇЄҐ'ʼ’-]+)¬ *\n([ \t]*)([а-яіїєґ'ʼ’-]+)/, '$1$3\n$2')
            out.println "\t\t¬ word wraps removed"
        }

        return text
    }

    
    static final List<String> ignoreHypLemmas = ["дійний", "ленський"]
    @CompileStatic
    boolean isHyphenBadLemma(String word) {
        try {
            List<String> lemmas = ltModule.getLemmas(word)
            return lemmas.intersect(ignoreHypLemmas)
        }
        catch (Exception e) {
            System.err.println("Failed on word: " + word)
            throw e
        }
    }

    @CompileStatic
    String separateLeadingHyphens(String text) {
        def regex = /(?m)^([-\u2013\u2014])([А-ЯІЇЄҐ][а-яіїєґ'ʼ’-]+|[а-яіїєґ'ʼ’-]{4,})/
        
        def converted = 0
        def t1 = text.replaceAll(regex, { all, hyph, word ->
            if( ltModule.knownWord(word) ) {
                converted += 1
                "$hyph $word"
            }
            else {
                all
            }
        })
        
        if( converted ) {
            out.println "\tConverted leading hyphens: ${converted}"
        }

        def regex2 = ~/ -[а-яіїєґ]{4,}/
        if( regex2.matcher(t1) ) {
            int cnt = 0
            def first = null
            
            t1.readLines()
                .each { line ->
                    def matcher = regex2.matcher(line)
                    while( matcher.find() ) {
                        cnt += 1
                        if( ! first )
                            first = matcher[0]
                    }
            }
            if( cnt ) {
                out.println "\tWARNING: found $cnt suspicious hypens after space, e.g. \"$first\""
            }
        }
        
        t1
    }
}
