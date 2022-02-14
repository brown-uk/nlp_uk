package org.nlp_uk.tools.tag;

import java.util.regex.Pattern

import org.apache.commons.lang3.mutable.MutableInt;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.nlp_uk.bruk.ContextToken
import org.nlp_uk.bruk.WordContext;
import org.nlp_uk.bruk.WordReading;
import org.nlp_uk.tools.TagText
import org.nlp_uk.tools.TagText.TagOptions
import org.nlp_uk.tools.TagText.TagOptions.DisambigModule

import groovy.transform.CompileStatic;

public class DisambigStats {
    private static final Pattern UPPERCASED_PATTERN = Pattern.compile(/[А-ЯІЇЄҐ][а-яіїєґ'-]+/)
    
    static boolean dbg = false
    
    TagOptions options
    
    Map<String, Map<WordReading, Double>> statsByWord = new HashMap<>()
    Map<String, Map<WordReading, Map<WordContext, Double>>> statsByWordContext = new HashMap<>()
    Map<String, Map<WordReading, Double>> statsByWordEnding = new HashMap<>()
    Map<String, Map<WordReading, Map<WordContext, Double>>> statsByWordEndingContext = new HashMap<>()
    Map<String, Double> statsByTag = new HashMap<>().withDefault { 0 }
    Map<String, Map<WordContext, Double>> statsByTagContext = new HashMap<>()
    Map<String, Double> statsForLemmaXp = new HashMap<>()
    
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
    List<BigDecimal> orderByStats(List<AnalyzedToken> readings, String cleanToken, AnalyzedTokenReadings[] tokens, int idx, TagStats stats) {

        if( ! statsByWord.containsKey(cleanToken) ) {
            // if no stats and there's no-prop readings try lowercase
            if( UPPERCASED_PATTERN.matcher(cleanToken).matches() 
                    && readings.size() > readings.count{ r -> r.getPOSTag() == null || r.getPOSTag().contains(":prop") }  ) {
                cleanToken = cleanToken.toLowerCase()
            }
        }

        Map<WordReading, Double> statsForWord = null
        Map<WordReading, Map<WordContext, Double>> statsForWordCtx = null

        stats.disambigMap['total'] ++
        
        if( statsByWord.containsKey(cleanToken) ) {
            debug("found stats for $cleanToken / $readings")
            stats.disambigMap['word'] ++
            
            statsForWord = statsByWord[cleanToken]
            statsForWordCtx = statsByWordContext[cleanToken]
        }
        else {
            stats.disambigMap['noWord'] ++
            
            for(AnalyzedToken r: readings) {
                String wordEnding = wordEnding(cleanToken, r.getPOSTag())
                if( wordEnding in statsByWordEnding ) {
                    stats.disambigMap['wordEnd'] ++
                    break;
                }
            }
        }

        int i=0
        def rates0 = readings.collect { r ->
            BigDecimal rate = 0
            if( statsForWord != null ) {
                rate += getRate(r, cleanToken, statsForWord, statsForWordCtx, tokens, idx, dbg)
            }
            
            if( DisambigModule.wordEnding in options.disambiguate ) {
                BigDecimal re = getRateByEnding(r, cleanToken, null, null, tokens, idx, dbg) / 100.0
                rate += re
            }

            rate += getPostagRate(r, tokens, idx, true, dbg) / 1000.0

            [idx: i++, rate: rate, reading: r]
        }

        rates0.sort { r1, r2 -> ((BigDecimal)r2.rate).compareTo((BigDecimal)r1.rate) } 

        def readingsSorted = rates0.collect { (AnalyzedToken)it.reading }
        
        readings.clear()
        readings.addAll( readingsSorted )
        
//        if( offTags ) stats.disambigMap['tag'] ++
        
        if( options.showDisambigRate ) {
            return rates0.collect { (BigDecimal)it.rate }
        }
        return null
    }
    
    @CompileStatic
    private BigDecimal getRate(AnalyzedToken at, String cleanToken, Map<WordReading, Double> statsForWordReading, Map<WordReading, Map<WordContext, Double>> statsC, 
            AnalyzedTokenReadings[] tokens, int idx, boolean dbg) {

        WordReading wordReading = new WordReading(at.getLemma(), at.getPOSTag())
        Double r = statsForWordReading[wordReading]
        BigDecimal rate = r ?: null
        
        if( rate ) {
            rate = adjustByContext(rate, wordReading, statsC, tokens, idx)
        }
        
        debug(dbg, "::rate: $at, idx: $idx : $rate")
        return rate ?: 0
    }

    @CompileStatic
    private BigDecimal getRateByEnding(AnalyzedToken at, String cleanToken, Map<WordReading, Double> statsForWordReading, Map<WordReading, Map<WordContext, Double>> statsC,
            AnalyzedTokenReadings[] tokens, int idx, boolean dbg) {

        BigDecimal rate = 0
        String postag = at.getPOSTag()
        String wordEnding = wordEnding(cleanToken, postag)
//        debug(dbg, ":: using word ending $wordEnding")
        Map<WordReading, Double> statsForWordEnding = statsByWordEnding[wordEnding]
        if( statsForWordEnding ) {
            WordReading wordReading2 = new WordReading('', normalizePostagForRate(postag))
            Double st = statsForWordEnding[wordReading2]
            if( st ) {
                rate = BigDecimal.valueOf(st)
            }
            debug(dbg, ":: using word ending $wordEnding, postag: ${wordReading2.postag} = rate: $rate")
        }
        
        return rate
    }

    @CompileStatic
    private <T> BigDecimal adjustByContext(BigDecimal rate, T key, Map<T, Map<WordContext, Double>> statsCtx, AnalyzedTokenReadings[] tokens, int idx) {
        if( ! (DisambigModule.context in options.disambiguate) )
            return rate
            
        Map<WordContext, Double> ctxToRateMap = statsCtx[key]
        Set<WordContext> currContexts = createWordContext(tokens, idx, -1)

        // TODO: limit previous tokens by ratings already applied?
        def matchedContexts = ctxToRateMap
            .findAll {WordContext wc, Double v2 -> v2
                // if any (previous) readings match the context add the context rate
                boolean found = currContexts.find { currContext ->
                    wc.contextToken.word == currContext.contextToken.word
                }
            }
            
        Double matchRateSum = (Double) matchedContexts.collect{k,v -> v}.sum()

        if( matchRateSum ) {
//            matchRateSum /= matchedContexts.size() // get rate average
            // give heave weight to context
            BigDecimal adjust = matchRateSum * 100
            BigDecimal oldRate = rate
            rate += adjust 
            debug(dbg, "ctxRate: key: $key, for ${matchedContexts.size()} matched ctx, adj: $adjust -> old rate: $oldRate, new rate: $rate")
        }

        rate
    }
    
    @CompileStatic
    private BigDecimal getPostagRate(AnalyzedToken reading, AnalyzedTokenReadings[] tokens, int idx, boolean withXp, boolean dbg) {
        String postag = reading.getPOSTag()
        String normPostag = normalizePostagForRate(postag)
        BigDecimal rate = statsByTag[normPostag]
        
        if( ! rate ) {
//            println "WARN: no stats for tag: $normPostag"
            return 0
        }

        debug(dbg, "norm tag: $normPostag, rate: $rate")

        if( rate ) {
            rate = adjustByContext(rate, normPostag, statsByTagContext, tokens, idx)
        }

        if( postag.contains(":xp") ) {
            String xp = (postag =~ /xp[0-9]/)[0]
            String key = "${reading.getLemma()}_${xp}".toString()
            Double mi = statsForLemmaXp[key]
            if( mi != null ) {
                debug("Adjusted for xp for $key with ($mi) by ${(mi * 3) / 2}")
                rate += (mi * 3) / 2
            }
        }
        
        return rate
    }

    private static final Pattern POSTAG_NORM_PATTERN = ~ /:(xp[1-9]|ua_[0-9]{4}|comp.|&predic|&insert|coll|rare|vulg)/
    
    @CompileStatic
    private static String normalizePostagForRate(String postag) {
        POSTAG_NORM_PATTERN.matcher(postag).replaceAll('')
    }
    
    @CompileStatic
    static String getTag(String theToken, String tag) {
        if( TagText.PUNCT_PATTERN.matcher(theToken).matches() ) {
            return 'punct'
        }
        else if( TagText.SYMBOL_PATTERN.matcher(theToken).matches() ) {
            return 'symb'
        }
        else if( TagText.XML_TAG_PATTERN.matcher(theToken).matches() ) {
            return 'xmltag'
        }
        tag
    }
    
    @CompileStatic
    private static Set<WordContext> createWordContext(AnalyzedTokenReadings[] tokens, int idx, int offset) {
        ContextToken contextToken
        if( idx + offset < 0 ) {
            contextToken = new ContextToken('__BEG', '', 'BEG')
        }
        else if ( idx + offset >= tokens.length ) {
            contextToken = new ContextToken('__END', '', 'END')
        }
        else {
            def token = tokens[idx+offset]
            return token.getReadings()
                .findAll{ AnalyzedToken tokenReading -> ! tokenReading.getPOSTag() != null }
                .collect { AnalyzedToken tokenReading ->
                    String postag = getTag(token.getCleanToken(), tokenReading.getPOSTag())
                    def normalizedTokenValue = ContextToken.normalizeContextString(token.getCleanToken(), tokenReading.getLemma(), postag)
                    contextToken = new ContextToken(normalizedTokenValue, '', postag)
                    new WordContext(contextToken, offset)
                } as Set
        }

        [new WordContext(contextToken, offset)] as Set
    }

    
    @CompileStatic
    def loadDisambigStats() {
        if( statsByWord.size() > 0 )
            return

        long tm1 = System.currentTimeMillis()
        
        def statDir = new File(new File((String)SCRIPT_DIR + "/../../../../../../.."), "stats")
        assert statDir.isDirectory(), "Disambiguation stats not found in ${statDir.name}"
        
        String word
        WordReading wordReading
        String postagNorm

        Map<String, List<Double>> statsByTagList = [:].withDefault { [] }
        
        new File(statDir, "lemma_freqs_hom.txt").eachLine { String line ->
            def p = line.split(/\h+/)

            if( ! line.startsWith('\t') ) {
    
                word = p[0]
                String lemma = p[1], postag = p[2]
                double cnt = p[3] as double
    
                wordReading = new WordReading(lemma, postag)
    
                statsByWord.computeIfAbsent(word, {s -> new HashMap<>()})
                    .put(wordReading, cnt)

                postagNorm = normalizePostagForRate(postag)
//                statsByTag[postagNorm] += cnt
                statsByTagList[postagNorm] << cnt
                
                if( postag.contains(":xp") ) {
                    String xp = (postag =~ /xp[0-9]/)[0]
                    //TODO: xp needs rather an absolute count
                    String key = "${lemma}_${xp}".toString()
                    statsForLemmaXp.computeIfAbsent(key, {k -> 0})
                    statsForLemmaXp[key] += cnt
                }

                if( true || DisambigModule.wordEnding in options.disambiguate ) {
                    String wordEnding = wordEnding(word, postagNorm)
                    if( wordEnding ) {
                        WordReading wordReading2 = new WordReading('', postagNorm)
                        def map = statsByWordEnding.computeIfAbsent(wordEnding, {s -> new HashMap<>()})
                        Double cnt_ = map[wordReading2] ?: 0
                        cnt_ += cnt
                        map[wordReading2] = cnt_
                    }
                }
    
                return
            }

            int pos = p[1] as int
            String ctxWord=p[2], ctxLemma=p[3], ctxPostag=p[4]
            double ctxCnt=p[5] as double

            ContextToken contextToken
            if( ctxPostag == "BEG" ) {
                contextToken = ContextToken.BEG
            }
            else if( ctxPostag == "END" ) {
                contextToken = ContextToken.END
            }
            else {
                ctxWord = ContextToken.unsafeguard(ctxWord)
                ctxLemma = ContextToken.unsafeguard(ctxLemma)
                contextToken = new ContextToken(ctxWord, ctxLemma, ctxPostag)
            }
            WordContext wordContext = new WordContext(contextToken, pos)
            
            statsByWordContext.computeIfAbsent(word, {s -> new HashMap<>()})
                .computeIfAbsent(wordReading, {x -> new HashMap<>()})
                .put(wordContext, ctxCnt)

            def map = statsByTagContext.computeIfAbsent(postagNorm, {s -> new HashMap<>()})
            Double cnt = map[wordContext] ?: 0
            cnt += ctxCnt
            map[wordContext] = cnt
        }

        double statsByTagTotal = (Double) statsByTagList.collect { k,v -> v.sum() }.sum()

        statsByTagList.each { String k, List<Double> v ->
            statsByTag[k] = (double)v.sum() / statsByTagTotal
        }
        
        long tm2 = System.currentTimeMillis()
        System.err.println("Loaded ${statsByWord.size()} disambiguation stats, ${statsByTag.size()} tags, ${statsByWordEnding.size()} endings, ${statsForLemmaXp.size()} xps in ${tm2-tm1} ms")
        
        if( dbg ) {
            File tff = new File("stats/tag_freqs.txt")
            tff.text = ''
            statsByTag
                    .sort{ a, b -> a.key.compareTo(b.key) }
                    .each { k,v ->
                        tff << "$k\t$v\n"
                        def ctxTotal = v
                        tff << "\t" << statsByTagContext[k].collect { k2, v2 -> 
                            k2.toString() + "\t" + (v2 / statsByTagTotal) 
                        }
                        .join("\n\t") << "\n"
                    }
            tff = new File("stats/tag_ending_freqs.txt")
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
        if( ! word.endsWith('.') && word.length() >= endLength + 1 
                && ! (word =~ /[А-ЯІЇЄҐ]$/) ) {
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
    
    void printStats(disambigMap) {
        BigDecimal unknown = disambigMap['total'] ? disambigMap['noWord'] * 100 / disambigMap['total'] : 0
        unknown = unknown.round(0)
        println "Disambig stats: ${disambigMap}: unknown: ${unknown}%"
    }
}
