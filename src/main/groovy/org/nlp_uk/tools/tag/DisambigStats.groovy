package org.nlp_uk.tools.tag;

import java.math.RoundingMode
import java.util.HashMap.EntrySet
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
    
    static class Stat {
        double rate = 0.0
        Map<WordContext, Double> ctxRates = [:]
    }
    
    // word -> wordReading -> rate/ctxRates
    Map<String, Map<WordReading, Stat>> statsByWord = new HashMap<>(16*1024)
    // wordEnding -> tag -> rate/ctxRates
    Map<String, Map<String, Stat>> statsByWordEnding = new HashMap<>(8*1024)
    // tag -> rate/ctxRates
    Map<String, Stat> statsByTag = new HashMap<>(1*1024)
    // lemma_xpN -> rate
    Map<String, Map<String, Double>> statsForLemmaXp = new HashMap<>(1*1024).withDefault{ new HashMap<>() }
    
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
    static BigDecimal rnd(double d) {
        BigDecimal.valueOf(d).setScale(4, RoundingMode.CEILING)
    }

        
    @CompileStatic
    List<Double> orderByStats(List<AnalyzedToken> readings, String cleanToken, AnalyzedTokenReadings[] tokens, int idx, TagStats stats) {

        if( ! statsByWord.containsKey(cleanToken) ) {
            // if no stats and there's no-prop readings try lowercase
            if( UPPERCASED_PATTERN.matcher(cleanToken).matches() 
                    && readings.size() > readings.count{ r -> r.getPOSTag() == null || r.getPOSTag().contains(":prop") }  ) {
                cleanToken = cleanToken.toLowerCase()
            }
        }

        Map<WordReading, Stat> statsForWord = statsByWord[cleanToken]

        stats.disambigMap['total'] ++
        
        if( statsForWord != null ) {
            debug("found word stats for $cleanToken / $readings")
            stats.disambigMap['word'] ++
        }
        else {
            stats.disambigMap['noWord'] ++
            
            for(AnalyzedToken r: readings) {
                String wordEnding = getWordEnding(cleanToken, r.getPOSTag())
                if( wordEnding in statsByWordEnding ) {
                    stats.disambigMap['wordEnd'] ++
                    break;
                }
            }
        }

        double postagRateSum = (double) readings.collect { anToken -> getPostagRate(anToken, tokens, idx, true, false, 0) }.sum()
        debug("${readings.size()} readings for $cleanToken, tag total: ${rnd(postagRateSum)}")
        
        int i=0
        def rates0 = readings.collect { anToken ->
            
            double rate = getRate(anToken, cleanToken, statsForWord, tokens, idx, dbg)
            
            if( DisambigModule.wordEnding in options.disambiguate ) {
                double wordEndingRate = getRateByEnding(anToken, cleanToken, tokens, idx, dbg)
                rate += wordEndingRate / 100.0
            }

            double postagRate = postagRateSum ? getPostagRate(anToken, tokens, idx, true, dbg, postagRateSum) : 0
            rate += postagRate / 10.0

            [idx: i++, rate: rate, reading: anToken]
        }

        rates0.sort { r1, r2 -> Double.compare((double)r2.rate, (double)r1.rate) } 

        def readingsSorted = rates0.collect { (AnalyzedToken)it.reading }
        
        readings.clear()
        readings.addAll( readingsSorted )
        
//        if( offTags ) stats.disambigMap['tag'] ++
        
        if( options.showDisambigRate ) {
            return rates0.collect { (Double)it.rate }
        }

        return null
    }
    
    @CompileStatic
    private Double getRate(AnalyzedToken at, String cleanToken, Map<WordReading, Stat> stats, 
            AnalyzedTokenReadings[] tokens, int idx, boolean dbg) {
            
        if( stats == null )
            return 0

        WordReading wordReading = new WordReading(at.getLemma(), at.getPOSTag())
        Stat r = stats[wordReading]
        Double rate = r ? r.rate : null
        
        if( rate ) {
            rate = adjustByContext(rate, wordReading, r.ctxRates, tokens, idx, dbg, 10000)
        }
        
        debug(dbg, "word rate: $at, idx: $idx : $rate")
        return rate ?: 0
    }

    @CompileStatic
    private double getRateByEnding(AnalyzedToken at, String cleanToken, AnalyzedTokenReadings[] tokens, int idx, boolean dbg) {

        double rate = 0
        String postag = at.getPOSTag()
        String wordEnding = getWordEnding(cleanToken, postag)
        debug(dbg, ":: using word ending $wordEnding")
        Map<String, Stat> statsForWordEnding = statsByWordEnding[wordEnding]
        if( statsForWordEnding != null ) {
            String normPostag = normalizePostagForRate(postag)
            Stat st = statsForWordEnding[normPostag]
            if( st ) {
                rate = st.rate
            }
            debug(dbg, ":: using word ending $wordEnding, normPostag: ${postag} = rate: $rate")
        }
        
        return rate
    }

    @CompileStatic
    private <T> double adjustByContext(double rate, T key, Map<WordContext, Double> ctxStats, AnalyzedTokenReadings[] tokens, int idx, boolean dbg, double ctxCoeff) {
        if( ! (DisambigModule.context in options.disambiguate) )
            return rate
            
//        Map<WordContext, Double> ctxToRateMap = stats[key].ctxRates
        Set<WordContext> currContexts = createWordContext(tokens, idx, -1)

        // TODO: limit previous tokens by ratings already applied?
        def matchedContexts = ctxStats
            .findAll {WordContext wc, Double v2 -> v2
                boolean found = currContexts.find { currContext ->
                    wc.contextToken.word == currContext.contextToken.word
                }
            }
            
        // if any (previous) readings match the context add its context rate
        Double matchRateSum = (Double) matchedContexts.collect{k,v -> v}.sum(0) /// (double)matchedContexts.size()

        if( matchRateSum ) {
//            matchRateSum /= matchedContexts.size() // get rate average
            // normalize context rate to main rate and give it a weight, max boost (single context) is x50
            double adjust = (matchRateSum / rate) * ctxCoeff + 1
            assert adjust >= 1.0, "ctxSum: $matchRateSum, rate: $rate, adjust: $adjust, key: $key"
            double oldRate = rate
            rate *= adjust 
            debug(dbg, "ctxRate: key: $key, ${matchedContexts.size()} ctxs, x: ${rnd(adjust)}")//, old: ${rnd(oldRate)} -> withCtx: ${rnd(rate)}")
        }

        rate
    }
    
    @CompileStatic
    private double getPostagRate(AnalyzedToken reading, AnalyzedTokenReadings[] tokens, int idx, boolean withXp, boolean dbg, double total) {
        String postag = reading.getPOSTag()
        String normPostag = normalizePostagForRate(postag)
        def stat = statsByTag[normPostag]
        
        if( ! stat ) {
//            println "WARN: no stats for tag: $normPostag"
            return 0
        }
        
        double rate = stat.rate
        if( total ) rate /= total
        assert rate <= 1.0
        
        double oldRate = rate
        
        if( rate && total ) {
            rate = adjustByContext(rate, normPostag, stat.ctxRates, tokens, idx, dbg, 1000000)
        }

        debug(dbg, "tag rate: $normPostag, idx: $idx: oldRate: ${rnd(oldRate)}, rate: ${rnd(rate)}")
        
        if( postag.contains(":xp") ) {
            String xp = (postag =~ /xp[0-9]/)[0]
            Double mi = statsForLemmaXp[reading.getLemma()][xp]
            if( mi != null ) {
                debug("Adjusted for xp for ${reading.getLemma()}, $xp with ($mi) by ${mi * 1.5}")
                rate += mi * 1.5
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
        String wordEnding
        WordReading wordReading
        String postagNorm

//        Map<String, List<Double>> statsByTagList = [:].withDefault { [] }
//        Map<String, Map<WordReading, List<Double>>> statsByWordEndingList = [:].withDefault { [:].withDefault{ [] } }
        
        new File(statDir, "lemma_freqs_hom.txt").eachLine { String line ->
            def p = line.split(/\h+/)

            if( ! line.startsWith('\t') ) {
    
                word = p[0]
                String lemma = p[1], postag = p[2]
                double rate = p[3] as double
    
                wordReading = new WordReading(lemma, postag)
    
                statsByWord.computeIfAbsent(word, {s -> new HashMap<>()})
                    .put(wordReading, new Stat(rate: rate))

                postagNorm = normalizePostagForRate(postag)

                // aggregate multiple word context into tag context
                statsByTag.computeIfAbsent(postagNorm, {s -> new Stat()})
                statsByTag[postagNorm].rate += rate
                
                if( postag.contains(":xp") ) {
                    String xp = (postag =~ /xp[0-9]/)[0]
                    //TODO: xp needs rather an absolute count
//                    String key = "${lemma}_${xp}".toString()
//                    statsForLemmaXp.computeIfAbsent(key, {k -> 0})
                    if( ! (xp in statsForLemmaXp[lemma] ) ) {
                        statsForLemmaXp[lemma][xp] = Double.valueOf(0)
                    }
                    statsForLemmaXp[lemma][xp] += rate
                }

                if( true || DisambigModule.wordEnding in options.disambiguate ) {
                    wordEnding = getWordEnding(word, postagNorm)
                    if( wordEnding ) {
                        def sbweStat = statsByWordEnding.computeIfAbsent(wordEnding, {s -> new HashMap<>()})
                            .computeIfAbsent(postagNorm, {x -> new Stat()})
                        sbweStat.rate += rate
                    }
                }
    
                return
            }

            // context line
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
            
            // 1 row per word context
            def sbwc = statsByWord[word][wordReading]
            sbwc.ctxRates.computeIfAbsent(wordContext, {Double.valueOf(0)})
            sbwc.ctxRates[wordContext] = ctxCnt

            // aggregate word ending contexts
            if( wordEnding ) {
//                def sbweStat = statsByWordEnding.computeIfAbsent(wordEnding, {s -> new HashMap<>()})
//                    .computeIfAbsent(postagNorm, {x -> new Stat()})
                statsByWordEnding[wordEnding][postagNorm].ctxRates.computeIfAbsent(wordContext, {Double.valueOf(0)})
                statsByWordEnding[wordEnding][postagNorm].ctxRates[wordContext] += ctxCnt
            }

            // aggregate postag contexts
            statsByTag[postagNorm].ctxRates.computeIfAbsent(wordContext, {Double.valueOf(0)})
            statsByTag[postagNorm].ctxRates[wordContext] += ctxCnt
        }

        double statsByTagTotal = (Double) statsByTag.collect { k,v -> v.rate }.sum()

        // normalize tag rates
        statsByTag.each { String tag, Stat v ->
            v.rate /= statsByTagTotal
            v.ctxRates.entrySet().each { e -> e.value /= (double)statsByTagTotal } 
        }

        double statsByWordEndingTotal = (Double) statsByWordEnding.collect { k,v -> v.collect{ k2, v2 -> v2.rate}.sum() }.sum()

        println ":: tagTotal: $statsByTagTotal, wordEndingTotal: $statsByWordEndingTotal"

        // normalize word ending rates
        statsByWordEnding.each { String wordEnding2, Map<String, Stat> v ->
            v.each { String tag, Stat s ->
                s.rate /= statsByWordEndingTotal
                s.ctxRates.entrySet().each { e -> e.value /= (double)statsByWordEndingTotal } 
            }
        }
        
        // normalize xp rate by lemma
        statsForLemmaXp.each { String k, Map<String, Double> v ->  
            double sum = (double)v.collect{ k2, v2 -> v2 }.sum()
            v.entrySet().each { Map.Entry<String, Double> e -> e.value /= (double)sum }
        }
        

        long tm2 = System.currentTimeMillis()
        System.err.println("Loaded ${statsByWord.size()} disambiguation stats, ${statsByTag.size()} tags, ${statsByWordEnding.size()} endings, ${statsForLemmaXp.size()} xps in ${tm2-tm1} ms")
        
        if( dbg ) {
            File tff = new File("stats/tag_freqs.txt")
            tff.text = ''
            statsByTag
                    .sort{ a, b -> a.key.compareTo(b.key) }
                    .each { k,v ->
                        tff << "$k\t${v.rate}\n"
                        def ctxTotal = v
                        tff << "\t" << v.ctxRates.collect { k2, v2 -> 
                            k2.toString() + "\t" + v2 
                        }
                        .join("\n\t") << "\n"
                    }

            tff = new File("stats/tag_ending_freqs.txt")
            tff.text = ''
            statsByWordEnding
                    .sort{ a, b -> a.key.compareTo(b.key) }
                    .each { String wordEnding2, Map<String, Stat> v ->
                        String vvv = v.collect { tag, v2 -> 
                            def ctx = v2.ctxRates.collect { k3, v3 -> 
                                k3.toString() + "\t" + v3 
                            }
                            .join("\n\t")
                            "$wordEnding2\t$tag\t${v2.rate}\n\t$ctx" 
                        }.join("\n")
                        tff << "$vvv\n"
                    }
        }
        
    }

    @CompileStatic
    static String getWordEnding(String word, String postag) {
        int endLength = 3
        if( postag != null
                && ! word.endsWith('.') && word.length() >= endLength + 1 
                && ! (word =~ /[А-ЯІЇЄҐ]$/) ) {
            String wordEnding = word
            if( postag.startsWith("verb:rev") || postag.startsWith("advp:rev") ) { 
                endLength = 5 
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
