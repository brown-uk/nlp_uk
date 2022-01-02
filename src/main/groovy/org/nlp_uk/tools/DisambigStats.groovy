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
import org.nlp_uk.tools.TagText.TagOptions.DisambigModule

import groovy.transform.CompileStatic;

public class DisambigStats {
    private static final Pattern UPPERCASED_PATTERN = Pattern.compile(/[А-ЯІЇЄҐ][а-яіїєґ'-]+/)
    
    static boolean dbg = false
    
    TagOptions options
    
    Map<String, Map<WordReading, Integer>> statsByWord = new HashMap<>()
    Map<String, Map<WordReading, Map<WordContext, Integer>>> statsByWordContext = new HashMap<>()
    Map<String, Map<WordReading, MutableInt>> statsByWordEnding = new HashMap<>()
    Map<String, Map<WordReading, Map<WordContext, Integer>>> statsByWordEndingContext = new HashMap<>()
    Map<String, Integer> statsByTag = new HashMap<>().withDefault { 0 }
    Map<String, Map<WordContext, MutableInt>> statsByTagContext = new HashMap<>()
    Map<String, MutableInt> statsForLemmaXp = new HashMap<>()
    
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
    static void debug(boolean dbg, String s) {
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
            debug("found stats for $cleanToken / $readings")
            stats.inStats ++
            
            Map<WordReading, Integer> statsByWord = statsByWord[cleanToken]
            Map<WordReading, Map<WordContext, Integer>> statsC = statsByWordContext[cleanToken]

            readings.sort { r1, r2 -> 
                def rate2 = getRate(r2, cleanToken, statsByWord, statsC, tokens, idx, false)
                def rate1 = getRate(r1, cleanToken, statsByWord, statsC, tokens, idx, false)
                int cmp = rate2.compareTo(rate1)
                boolean withXp = r2.getPOSTag().contains(":xp") && r1.getPOSTag().contains(":xp")
                cmp ?: getPostagRate(r2, tokens, idx, withXp, dbg).compareTo(getPostagRate(r1, tokens, idx, withXp, dbg))  
            }
            readings.collect { r -> 
                getRate(r, cleanToken, statsByWord, statsC, tokens, idx, dbg) 
            }
        }
        else {
            debug("trying stats for ending $cleanToken / $readings")
            stats.offStats ++
            boolean offTags
            
            readings.sort { r1, r2 ->
                int cmp
                if( DisambigModule.wordEnding in options.disambiguate ) {
                    int rate2 = getRate(r2, cleanToken, null, null, tokens, idx, false)
                    int rate1 = getRate(r1, cleanToken, null, null, tokens, idx, false)
                    if( rate1 == 0 && rate2 == 0 ) offTags = true
                    cmp = rate2.compareTo(rate1)
                }
                boolean withXp = r2.getPOSTag().contains(":xp") && r1.getPOSTag().contains(":xp")
                cmp ?: getPostagRate(r2, tokens, idx, withXp, false).compareTo(getPostagRate(r1, tokens, idx, withXp, false))
            }
            if( offTags ) stats.offTags ++
            readings.collect { r -> 
//                getRate(r, cleanToken, null, null, tokens, idx, dbg)
                getPostagRate(r, tokens, idx, true, dbg)
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
        new WordContext(contextToken, pos)
    }
    
    @CompileStatic
    private int getRate(AnalyzedToken at, String cleanToken, Map<WordReading, Integer> statsForWordReading, Map<WordReading, Map<WordContext, Integer>> statsC, 
            AnalyzedTokenReadings[] tokens, int idx, boolean dbg) {

        debug(dbg, ":: $at, idx: $idx")

        int total = 0
        
        WordReading wordReading = new WordReading(at.getLemma(), at.getPOSTag())
        Integer vF = statsForWordReading ? statsForWordReading[wordReading] : 0
        
        if( vF ) { 

            vF = adjustByContext(vF, wordReading, statsC, tokens, idx)
            
            total = vF
        }
        else if( ! statsForWordReading ) {
            String postag = at.getPOSTag()
            String wordEnding = wordEnding(cleanToken, postag)
            debug(dbg, ":: using word ending $wordEnding")
            Map<WordReading, MutableInt> statsForWordEnding = statsByWordEnding[wordEnding]
            if( statsForWordEnding ) {
                WordReading wordReading2 = new WordReading('', normalizePostagForRate(postag))
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
        
        debug(dbg, ":: $total")
        return total
    }

    @CompileStatic
    private static String normalizePostagForRate(String postag) {
        postag.replaceAll(/:(xp[1-9]|ua_[0-9]{4})/, '')
    }
    
    @CompileStatic
    private int adjustByContext(Integer vF, WordReading wordReading, Map<WordReading, Map<WordContext, Integer>> statsC, AnalyzedTokenReadings[] tokens, int idx) {
        if( DisambigModule.context in options.disambiguate ) {
            Map<WordContext, Integer> ctx = statsC[wordReading]
            WordContext wordContext = createWordContext(tokens, idx, -1)

            debug(dbg, "  wr: $wordReading, v: $vF")

            Integer fitCnt = (Integer)ctx.findAll { WordContext wc, Integer v2 ->
                wc.offset == wordContext.offset \
                && wc.contextToken.word == wordContext.contextToken.word
            }
            .collect {
                WordContext wc, Integer v2 -> v2
            }
            .sum()

            if( fitCnt ) {
                Integer allCnt = (Integer)ctx
                .collect {
                    WordContext wc, Integer v2 -> v2
                }
                .sum()
                
                debug( dbg, "==  $fitCnt / $allCnt")
                vF = (vF * 3 * 100 * fitCnt).intdiv(allCnt) 
            }
        }
        vF
    }
    
    @CompileStatic
    private int getPostagRate(AnalyzedToken reading, AnalyzedTokenReadings[] tokens, int idx, boolean withXp, boolean dbg) {
        String postag = reading.getPOSTag()
        String normPostag = normalizePostagForRate(postag)
        int rate = statsByTag[normPostag]
        
        if( ! rate ) {
//            println "INFO: no stats for tag: $normPostag"
            
//            normPostag = normPostag.replaceAll(/:(nv|alt|&adjp:(pasv|actv):(im)?perf|comp.)/, '')
            rate = statsByTag[normPostag]
        }
        if( ! rate ) {
//            println "WARN: no stats for tag: $normPostag"
            return 0
        }

        debug(dbg, ":: norm tag: $normPostag, rate: $rate")
        
        if( DisambigModule.context in options.disambiguate ) {
            Map<WordContext, MutableInt> ctx = statsByTagContext[normPostag]
            WordContext wordContext = createWordContext(tokens, idx, -1)

            if( reading.getToken() == "досліджуваного" ) {
                println "  postag: $normPostag, v: $rate"
            }
            
            debug(dbg, "  postag: $normPostag, v: $rate")

            Integer fitCnt = (Integer)ctx.findAll { WordContext wc, MutableInt v2 ->
                wc.offset == wordContext.offset \
                && wc.contextToken.word == wordContext.contextToken.word
            }
            .collect {
                WordContext wc, MutableInt v2 -> v2
            }
            .sum()

             if( fitCnt ) {
                Integer allCnt = (Integer)ctx
                .collect {
                    WordContext wc, MutableInt v2 -> v2
                }
                .sum()

                debug( dbg, "==  $fitCnt / $allCnt")
                rate = (rate * 3 * 100 * fitCnt).intdiv(allCnt) 
//                    rate = rate * 3
            }
        }
        
        if( postag.contains(":xp") ) {
            String xp = (postag =~ /xp[0-9]/)[0]
            MutableInt mi = statsForLemmaXp.get("${reading.getLemma()}_${xp}".toString())
            if( mi != null ) {
                rate += (mi.getValue() * 3).intdiv(2)
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
            def p = line.split(/\h+/)

            if( ! line.startsWith('\t') ) {
    
                word = p[0]
                String lemma = p[1], postag = p[2]
                int cnt = p[3] as int
    
                wordReading = new WordReading(lemma, postag)
    
                statsByWord.computeIfAbsent(word, {s -> new HashMap<>()})
                    .put(wordReading, cnt as int)

                postagNorm = normalizePostagForRate(postag)
                statsByTag[postagNorm] += cnt

                if( postag.contains(":xp") ) {
                    String xp = (postag =~ /xp[0-9]/)[0]
                    statsForLemmaXp.computeIfAbsent("${lemma}_${xp}".toString(), {s -> new MutableInt()}).add(cnt)
                }

                if( true || DisambigModule.wordEnding in options.disambiguate ) {
                    String wordEnding = wordEnding(word, postagNorm)
                    if( wordEnding ) {
                        WordReading wordReading2 = new WordReading('', postagNorm)
                        statsByWordEnding.computeIfAbsent(wordEnding, {s -> new HashMap<>()})
                            .computeIfAbsent(wordReading2, {s -> new MutableInt()}).add(cnt)
//                        if( wordEnding == "ежі" )
//                            println ":: ежі / $wordReading2 = " + statsByWordEnding[wordEnding][wordReading2]
                    }
                }
    
                return
            }

//            println "= $line"
            int pos = p[1] as int
            String ctxWord=p[2], ctxLemma=p[3], ctxPostag=p[4]
            int ctxCnt=p[5] as int

            ctxWord = ContextToken.unsafeguard(ctxWord)
            ctxLemma = ContextToken.unsafeguard(ctxLemma)
            
            ContextToken contextToken = new ContextToken(ctxWord, ctxLemma, ctxPostag)
            WordContext wordContext = new WordContext(contextToken, pos)
            
            statsByWordContext.computeIfAbsent(word, {s -> new HashMap<>()})
                .computeIfAbsent(wordReading, {x -> new HashMap<>()})
                .put(wordContext, ctxCnt)

            statsByTagContext.computeIfAbsent(postagNorm, {s -> new HashMap<>()})
                .computeIfAbsent(wordContext, {s -> new MutableInt()}).add(ctxCnt)

        }

        long tm2 = System.currentTimeMillis()
        System.err.println("Loaded ${statsByWord.size()} disambiguation stats, ${statsByTag.size()} tags, ${statsByWordEnding.size()} endings, ${statsForLemmaXp.size()} xps in ${tm2-tm1} ms")
        
        if( dbg ) {
            File tff = new File("tag_freqs.txt")
            tff.text = ''
            statsByTag
                    .sort{ a, b -> a.key.compareTo(b.key) }
                    .each { k,v ->
                        tff << "$k\t$v\n"
                        tff << "\t" << statsByTagContext[k].collect { k2, v2 -> k2.toString() }.join("\n\t") << "\n"
                    }
            tff = new File("tag_ending_freqs.txt")
            tff.text = ''
            statsByWordEnding
                    .sort{ a, b -> a.key.compareTo(b.key) }
                    .each { k,v ->
                        String vvv = v.collect { k2,v2 -> "$k2\t$v2" }.join("\n\t")
                        tff << "$k\n\t$vvv\n"
                    }
        }
        
//        System.err.println(":: " + statsByTagContext['noun:inanim:f:v_mis'].findAll{ wc, i -> wc.contextToken.word == 'в'})
//        System.err.println(":: " + statsByTagContext['noun:inanim:f:v_rod'].findAll{ wc, i -> wc.contextToken.word == 'в'})
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
