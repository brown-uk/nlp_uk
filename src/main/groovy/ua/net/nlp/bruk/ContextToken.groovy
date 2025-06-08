package ua.net.nlp.bruk

import java.util.regex.Matcher
import java.util.regex.Pattern
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.languagetool.rules.uk.LemmaHelper

@CompileStatic
@Canonical
class ContextToken {
//    static final Pattern POSTAG_KEY_PATTERN = Pattern.compile("^(noun:(anim|[iu]nanim)|verb(:rev)?:(perf|imperf)|adj|adv(p:(imperf:perf))?|part|prep|numr|conj:(coord|subord)|intj|onomat|punct|symb|noninfl|unclass|number|unknown|time|date|hashtag|BEG|END)")
    static final Pattern POSTAG_CORE_REMOVE_PATTERN = Pattern.compile(/:(arch|coll|slang|bad|vulg|up[0-9]{2}|ns)/)
    static final ContextToken BEG = new ContextToken('__BEG', '', 'BEG')
    static final ContextToken END = new ContextToken('__END', '', 'END')
    static final String[] IGNORE_TOKENS = [] //['б', 'би', 'ж', 'же', 'бодай'] 
    
    String word
    String lemma
    String postag
    
    @CompileStatic
    ContextToken(String word, String lemma, String postag) {
        this.word = word
        this.lemma = lemma
//        assert postag, "Empty postag for $word/$lemma"
        this.postag = getPostagCore(postag)
    }
    
    @CompileStatic
    static ContextToken normalized(String word, String lemma, String postag) {
        new ContextToken(normalizeContextString(word, lemma, postag),
            normalizeContextString(lemma, '', postag),
            postag)
    }

    @CompileStatic
    String toString() {
        def w = safeguard(word)
        def l = safeguard(lemma)
        "$w\t$l\t$postag"
    }
    
    @CompileStatic
    static String getPostagCore(String postag) {
        postag != null ? POSTAG_CORE_REMOVE_PATTERN.matcher(postag).replaceAll('') : postag
    }

    @CompileStatic
    static String safeguard(String w) {
        if( w == '' ) return '^'

        w //w.indexOf(' ') >= 0 ? w.replace(' ', '\u2009') : w
    }

    @CompileStatic
    static String unsafeguard(String w) {
        w //w = w.indexOf('\u2009') >= 0 ? w.replace('\u2009', ' ') : w
    }

    @CompileStatic
    static String normalizeContextString(String w, String lemma, String postag) {
        if( ! w ) // possible for lemmas from AnalyzedToken
            return w
        
        if( postag == "number" ) {
            def m0 = Pattern.compile(/((1[6789]|20)[0-9]{2}[-–—])?(1[6789]|20)([0-9]{2})/).matcher(w) // preserve a year - often works as adj
            if( m0.matches() )
                return "YY" + m0.group(4)

            // normalize 10 000
            w = w.replace(" ", "")
                
            def m1 = Pattern.compile(/([0-9]+[-—–])?([0-9]+)/).matcher(w) // we only care about last two digits
            if( m1.matches() ) {
                if( w =~ /[05-9]$/ || w =~ /1[0-9]$/ )
                    return 0
                if( w =~ /[234]$/ )
                    return 2
                if( w =~ /1$/ )
                    return 1
                // should not happen
                return m1.group(2)
            }

            def m2 = Pattern.compile(/([0-9,]+[–—-])?[0-9]+([,.])[0-9]+/).matcher(w) // we only care that it's decimal
            if( m2.matches() )
                return '0,0'
        }

        String w1 = normalizeWord(w, lemma, postag)
        if( w1 != w )
            return w1
        
        if( postag == "punct" ) {
            if( w == "..." )
                return '…'

            if( w.length() == 1 )
                return w.replaceAll(/^[\u2013\u2014]$/, '-')
                        .replace('„', '«')
                        .replace('“', '»')

            if( w.indexOf(".") > 0 )
                return w.replaceAll(/^([?!])\.+$/, '$1')
        }

        boolean hasLowerCaseLemma = lemma && lemma =~ /^[а-яіїєґ]/
        w = hasLowerCaseLemma ? w.toLowerCase() : w
        
//        if( postag == "prep" ) {
//            if( w=="із" || w=="зо" )
//                return "з"
//            if( w=="у" )
//                return "в"
//        }
//        else if( postag == "conj:coord" ) {
//            if( w=="й" )
//                return "і"
//        }
            
        return w
    }
    
    @CompileStatic
    static String normalizeWord(String w, String lemma, String postag) {
        w = w.replace('\u2013', '-')
        // 2000-го -> 0-го
        // 101-річчя -> 101-річчя
        if( w.indexOf('-') > 0 && postag =~ /^(adj|noun)/ ) {
            def m1 = Pattern.compile(/[0-9-]*([0-9])-([а-яіїєґ]+)/).matcher(w)
            if( m1.matches() )
                return m1.replaceFirst('$1-$2')
        }
        return w
    }

    // його|що
    private static final USE_RIGHT_CTX_PATTERN = ~(/є|її|це|саме|[ву]с[еі]|за/
         + /|всередині|відповідно|перед|протягом|близько|навколо|довкола|наприкінці|неподалік|[ву]глиб|поза/
         + /|брати|(українськ|англійськ)(а|у|ою|ій)|рівні|доросл.*|майбутн(є|ього|ім|ому)/
         + /|більше|добре|геть|тільки/)

    static boolean useRightContext(String token) {
        token.toLowerCase() ==~ USE_RIGHT_CTX_PATTERN
    }
    
}
