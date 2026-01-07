package ua.net.nlp.tools.stress

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@CompileStatic
@PackageScope
class Util {
    
    static String getTagKey(String tag) {
        if( tag.contains('lname') )
            return 'lname'
        
        tag.replace(':inanim', '') \
            .replace(':rev', "")
            .replaceFirst(/(noun(:(un)?anim)?:[mnfps]|(noun(:(un)?anim)?).*pron|verb(:perf|:imperf)+|adj|[a-z]+).*/, '$1')
    }
    
    static int getSyllCount(String word) {
        int cnt = 0
        word.getChars().each { char ch ->
            if( isWovel(ch) )
                cnt += 1
        }
        cnt
    }
    
    static boolean isWovel(char ch) {
        "аеєиіїоуюяАЕЄИІЇОУЮЯ".indexOf((int)ch) >= 0
    }
    
    static String stripAccent(String word) {
        word.replace("\u0301", "")
    }
    
    static List<Integer> getAccentSyllIdxs(String word) {
        int syllIdx = 0
        List<Integer> idxs = []
        word.getChars().each { char it ->
            if( it == '\u0301' ) {
                idxs << syllIdx
            }
            else if( "аеєиіїоуюяАЕЄИІЇОУЮЯ".indexOf((int)it) >= 0 ) {
                syllIdx += 1
            }
        }
        idxs
    }
    
    @CompileStatic
    static String restoreAccent(String lemma, String word, int offset) {
        List<Integer> accents = getAccentSyllIdxs(lemma)
        if( offset ) {
            accents.eachWithIndex{ int a, int i -> accents[i]+=offset }
        }
        println "restore for: $lemma: $accents"
        applyAccents(word, accents)
    }

    static String applyAccents(String word, List<Integer> accents) {
        def sb = new StringBuilder()
        int syll = 0
        word.getChars().eachWithIndex { char ch, int idx ->
            sb.append(ch)
            if( isWovel(ch) ) {
                syll += 1
                if( syll in accents ) sb.append('\u0301')
            }
        }
        sb.toString()
    }
}
