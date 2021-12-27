package org.nlp_uk.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern

import org.apache.commons.lang3.mutable.MutableInt;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.nlp_uk.bruk.ContextToken
import org.nlp_uk.bruk.WordContext;
import org.nlp_uk.bruk.WordReading;
import org.nlp_uk.tools.TagText.Stats
import org.nlp_uk.tools.TagText.TagOptions

import groovy.transform.CompileStatic;

public class DisambigStats {
    private static final Pattern UPPERCASED_PATTERN = Pattern.compile(/[А-ЯІЇЄҐ][а-яіїєґ'-]+/)
    
    private static final boolean dbg = false
    
    TagOptions options
    
    Map<String, Map<WordReading, Integer>> statsByWord = new HashMap<>()
    Map<String, Map<WordReading, Map<WordContext, Integer>>> statsByWordContext = new HashMap<>()
    Map<String, Map<WordReading, MutableInt>> statsByWordEnding = new HashMap<>()
    Map<String, Map<WordReading, Map<WordContext, Integer>>> statsByWordEndingContext = new HashMap<>()
    Map<String, Integer> statsByTag = new HashMap<>().withDefault { 0 }
    Map<String, Map<WordContext, MutableInt>> statsByTagContext = new HashMap<>()

    @groovy.transform.SourceURI
    static SOURCE_URI
    // if this script is called from GroovyScriptEngine SourceURI is data: and does not work for File()
    static SCRIPT_DIR = SOURCE_URI.scheme == "data"
        ? new File("src/main/groovy/org/nlp_uk/tools")
        : new File(SOURCE_URI).parent

    @CompileStatic
    static void debug(String s) {
        if( dbg ) println ":: $s"
    }
        
        
    @CompileStatic
    List<Integer> orderByStats(List<AnalyzedToken> readings, String cleanToken, AnalyzedTokenReadings[] tokens, int idx, Stats stats) {

        if( ! statsByWord.containsKey(cleanToken) ) {
            // if no stats and there's no prop readings try lowercase
            if( UPPERCASED_PATTERN.matcher(cleanToken).matches() 
                    /* && ! readings.find{ r -> r.getPOSTag().contains(":prop") } */ ) {
                cleanToken = cleanToken.toLowerCase()
            }
        }
        
        if( statsByWord.containsKey(cleanToken) ) {
            if( dbg ) 
                System.err.println "found stats for $cleanToken / $readings"
            stats.inStats ++
            
            Map<WordReading, Integer> statsByWord = statsByWord[cleanToken]
            Map<WordReading, Map<WordContext, Integer>> statsC = statsByWordContext[cleanToken]

            readings.sort { r1, r2 -> 
                def rate2 = getRate(r2, cleanToken, statsByWord, statsC, tokens, idx, false)
                def rate1 = getRate(r1, cleanToken, statsByWord, statsC, tokens, idx, false)
                int cmp = rate2.compareTo(rate1)
                cmp ?: getPostagRate(r2.getPOSTag(), tokens, idx, dbg).compareTo(getPostagRate(r1.getPOSTag(), tokens, idx, dbg))  
            }
            readings.collect { r -> 
                getRate(r, cleanToken, statsByWord, statsC, tokens, idx, dbg) 
            }
        }
        else {
            if( dbg )
                System.err.println "trying stats for ending $cleanToken / $readings"
            stats.offStats ++
            boolean offTags
            
            readings.sort { r1, r2 ->
                int rate2 = getRate(r2, cleanToken, null, null, tokens, idx, false) 
                int rate1 = getRate(r1, cleanToken, null, null, tokens, idx, false)
                if( rate1 == 0 && rate2 == 0 ) offTags = true 
                int cmp = rate2.compareTo(rate1)
//                cmp = 0; offTags = true
                cmp ?: getPostagRate(r2.getPOSTag(), tokens, idx, dbg).compareTo(getPostagRate(r1.getPOSTag(), tokens, idx, dbg))  
            }
            if( offTags ) stats.offTags ++
            readings.collect { r -> 
                getRate(r, cleanToken, null, null, tokens, idx, dbg)
//                rt ?: getPostagRate(r.getPOSTag(), tokens, idx, dbg)
            }
        }
//        else { 
//            System.err.println "no stats for $cleanToken / $readings"
//            stats.offStats ++
//            
//            readings.sort { r1, r2 -> getPostagRate(r2.getPOSTag(), tokens, idx, dbg).compareTo(getPostagRate(r1.getPOSTag(), tokens, idx, dbg)) }
//            readings.collect { r -> getPostagRate(r.getPOSTag(), tokens, idx, dbg) }
//        }
    }

    @CompileStatic
    private static WordContext createWordContext(AnalyzedTokenReadings[] tokens, int idx, int pos) {
        ContextToken contextToken = idx + pos < 0 
            ? new ContextToken('', '', 'BEG')
            : idx + pos >= tokens.length 
                ? new ContextToken('', '', 'END')
                : new ContextToken(tokens[idx+pos].getCleanToken(), '', '')  
        new WordContext(contextToken: contextToken, offset: pos)
    }
    
    @CompileStatic
    private int getRate(AnalyzedToken at, String cleanToken, Map<WordReading, Integer> statsForWordReading, Map<WordReading, Map<WordContext, Integer>> statsC, AnalyzedTokenReadings[] tokens, int idx, boolean dbg) {
        if( dbg )
            println ":: $at, idx: $idx"

        int total = 0
        
        WordReading wordReading = new WordReading(lemma: at.getLemma(), postag: at.getPOSTag())
        Integer vF = statsForWordReading ? statsForWordReading[wordReading] : 0
        
        if( vF ) { 

            if( options.statsByContext ) {
                Map<WordContext, Integer> ctx = statsC[wordReading]
                WordContext wordContext = createWordContext(tokens, idx, -1)

                if( dbg )
                    println "  wr: $wordReading, v: $vF"

                def fitCtx = ctx.findAll { WordContext wc, Integer v2 ->
                    wc.offset == wordContext.offset \
                    && wc.contextToken.word == wordContext.contextToken.word
                }
                if( fitCtx ) {
                    if( dbg )
                        println "  $fitCtx"
                    vF = vF * 3
                }
            }

            total = vF
        }
        else if( ! statsForWordReading ) {
            String postag = at.getPOSTag()
            String wordEnding = wordEnding(cleanToken, postag)
            if( dbg ) println ":: using word ending $wordEnding"
            Map<WordReading, MutableInt> statsForWordEnding = statsByWordEnding[wordEnding]
            if( statsForWordEnding ) {
                WordReading wordReading2 = new WordReading(lemma: '', postag: normalizePostagForRate(postag))
                def st = statsForWordEnding[wordReading2]
                if( st ) {
                    vF = st.intValue()
                }
                if( dbg ) println ":: using word ending $wordEnding, postag: ${wordReading2.postag} = vF: $vF"
            }
            
            if( vF ) {
                total = vF
            }
        }
        
        if( dbg )        
            println ":: $total"
        return total
    }

    @CompileStatic
    private static String normalizePostagForRate(String postag) {
        postag.replaceAll(/:xp[1-9]/, '')
    }
    
    @CompileStatic
    private int getPostagRate(String postag, AnalyzedTokenReadings[] tokens, int idx, boolean dbg) {
        postag = normalizePostagForRate(postag)
        int rate = statsByTag[postag]
                if( dbg )
        println ":: norm tag: $postag, rate: $rate"
        
        if( rate ) {
            if( false && options.statsByContext ) {
                Map<WordContext, MutableInt> ctx = statsByTagContext[postag]
                WordContext wordContext = createWordContext(tokens, idx, -1)

                if( dbg )
                    println "  postag: $postag, v: $rate"

                def fitCtx = ctx.findAll { WordContext wc, MutableInt v2 ->
                    wc.offset == wordContext.offset \
                    && wc.contextToken.word == wordContext.contextToken.word
                }
                if( fitCtx ) {
                    if( dbg )
                        println "  $fitCtx"
                    rate = rate * 3
                }
            }
        }
            
        return rate
    }

    
    @CompileStatic
    def loadDisambigStats() {
        if( statsByWord.size() > 0 )
            return

        long tm1 = System.currentTimeMillis()
        
        def statDir = new File(new File((String)SCRIPT_DIR + "/../../../../../.."), "stats")
        assert statDir.isDirectory(), "Disambiguation stats not found in ${statDir.name}"
        
        String word
        WordReading wordReading
        String postagNorm

        new File(statDir, "lemma_freqs_hom.txt").eachLine { String line ->
            def p = line.split(/\s*,\s*/)

            if( ! line.startsWith(' ') ) {
    
                word = p[0]
                String lemma = p[1], postag = p[2]
                int cnt = p[3] as int
    
                wordReading = new WordReading(lemma:lemma, postag:postag)
    
                statsByWord.computeIfAbsent(word, {s -> new HashMap<>()})
                    .put(wordReading, cnt as int)

                postagNorm = normalizePostagForRate(postag)
                statsByTag[postagNorm] += cnt
                
                String wordEnding = wordEnding(word, postagNorm)
                if( wordEnding ) {
                    WordReading wordReading2 = new WordReading(lemma:'', postag:postagNorm)
                    statsByWordEnding.computeIfAbsent(wordEnding, {s -> new HashMap<>()})
                        .computeIfAbsent(wordReading2, {s -> new MutableInt()}).add(cnt)
                            
//                        if( wordEnding == "ежі" )
//                            println ":: ежі / $wordReading2 = " + statsByWordEnding[wordEnding][wordReading2] 
                }
    
                return
            }

            int pos = p[1] as int
            String ctxWord=p[2], ctxLemma=p[3], ctxPostag=p[4]
            int ctxCnt=p[5] as int

            if( ctxWord.indexOf('^') >= 0 ) ctxWord = ctxWord.replace('^', ',')
            if( ctxLemma.indexOf('^') >= 0 ) ctxLemma = ctxLemma.replace('^', ',')
            
            ContextToken contextToken = new ContextToken(ctxWord, ctxLemma, ctxPostag)
            WordContext wordContext = new WordContext(contextToken: contextToken, offset: pos)
            
            statsByWordContext.computeIfAbsent(word, {s -> new HashMap<>()})
                .computeIfAbsent(wordReading, {x -> new HashMap<>()})
                .put(wordContext, ctxCnt)

            statsByTagContext.computeIfAbsent(postagNorm, {s -> new HashMap<>()})
                .computeIfAbsent(wordContext, {s -> new MutableInt()}).add(ctxCnt)

        }

        long tm2 = System.currentTimeMillis()
        System.err.println("Loaded ${statsByWord.size()} disambiguation stats, ${statsByTag.size()} tags, ${statsByWordEnding.size()} endings in ${tm2-tm1} ms")
        
//        System.err.println(statsByWordEnding['ики'])
//        System.err.println(statsByWordEnding['ори'])
    }

    @CompileStatic
    static String wordEnding(String word, String postag) {
        int endLength = 3
        if( word.length() >= endLength + 1 ) {
            String wordEnding = word
            if( postag.startsWith("verb:rev") ) { 
                wordEnding = wordEnding[0..-3] 
            }

            if( wordEnding.length() >= endLength + 1 ) {
                wordEnding = wordEnding[-endLength..-1]

                return wordEnding
            }
        }
        
        null
    }
}
