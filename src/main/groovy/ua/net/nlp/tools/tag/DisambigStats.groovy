package ua.net.nlp.tools.tag;

import java.math.RoundingMode
import java.util.HashMap.EntrySet
import java.util.regex.Pattern

import org.apache.commons.lang3.mutable.MutableInt;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import ua.net.nlp.bruk.ContextToken
import ua.net.nlp.bruk.WordContext;
import ua.net.nlp.bruk.WordReading;
import ua.net.nlp.tools.TagText
import ua.net.nlp.tools.TagText.TagOptions
import ua.net.nlp.tools.TagText.TagOptions.DisambigModule

import groovy.transform.CompileStatic;
import groovy.xml.slurpersupport.Node

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
    Map<String, Map<String, Stat>> statsByWordEnding = new HashMap<>(3*1024)
    // tag -> rate/ctxRates
    Map<String, Stat> statsByTag = new HashMap<>(1*1024)
    // lemma_xpN -> rate
    Map<String, Map<String, Double>> statsForLemmaXp = new HashMap<>(256).withDefault{ new HashMap<>() }
    
    @groovy.transform.SourceURI
    static SOURCE_URI
    // if this script is called from GroovyScriptEngine SourceURI is data: and does not work for File()
    static SCRIPT_DIR = SOURCE_URI.scheme == "data"
        ? new File("src/main/groovy/ua/net/nlp/tools")
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

        boolean withXp = true
        double postagRateSum = (double) readings.collect { anToken -> getPostagRate(anToken, tokens, idx, withXp, false, 0, 0) }.sum()
        double wordEndingRateSum = (double) readings.collect { anToken -> getRateByEnding(anToken, cleanToken, tokens, idx, false, 0, 0) }.sum()
        debug("${readings.size()} readings for $cleanToken, tag total: ${rnd(postagRateSum)}, wordEnding total: ${rnd(wordEndingRateSum)}")

        int i=0
        def rates0 = readings.collect { anToken ->
            
            double rate = getRate(anToken, cleanToken, statsForWord, tokens, idx, dbg)
            
            if( DisambigModule.wordEnding in options.disambiguate 
                    && wordEndingRateSum > 0
                    && rate == 0.0 ) {
                double ctxQ = /*rate ? 100000 :*/ 1.2*1000000
                double wordEndingRate = getRateByEnding(anToken, cleanToken, tokens, idx, dbg, wordEndingRateSum, ctxQ)
                rate += wordEndingRate / 80.0
            }
            
            if( postagRateSum ) {
                double ctxQ = /*rate ? 100000 :*/ 1.2*1000000
                double postagRate = getPostagRate(anToken, tokens, idx, withXp, dbg, postagRateSum, ctxQ)
                rate += postagRate / 60.0
            }

            debug(" RATE: ${rnd(rate)}")
            
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
    private double getRate(AnalyzedToken at, String cleanToken, Map<WordReading, Stat> stats, 
            AnalyzedTokenReadings[] tokens, int idx, boolean dbg) {
            
        if( stats == null )
            return 0

        WordReading wordReading = new WordReading(at.getLemma(), at.getPOSTag())
        Stat r = stats[wordReading]
        Double rate = r ? r.rate : 0
        
        if( rate ) {
            rate = adjustByContext(rate, wordReading, r.ctxRates, tokens, idx, dbg, 10000, ContextMode.WORD)
        }
        
        debug(dbg, "word rate: $at, idx: $idx : ${rnd(rate)}")
        return rate
    }

    @CompileStatic
    private double getRateByEnding(AnalyzedToken at, String cleanToken, AnalyzedTokenReadings[] tokens, int idx, boolean dbg, double total, double ctxQ) {

        double rate = 0
        String postag = at.getPOSTag()
        String wordEnding = getWordEnding(cleanToken, postag)
        if( ! wordEnding )
            return 0
        
        Map<String, Stat> statsForWordEnding = statsByWordEnding[wordEnding]
        if( statsForWordEnding != null ) {
            String normPostag = normalizePostagForRate(postag)
            Stat stat = statsForWordEnding[normPostag]
            
            if( ! stat ) {
//            println "WARN: no stats for tag: $wordEnding"
                return 0
            }
                
            rate = stat.rate
            if( total ) rate /= total
            assert rate <= 1.0
            
            if( rate && total ) {
                rate = adjustByContext(rate, wordEnding, stat.ctxRates, tokens, idx, dbg, ctxQ, ContextMode.WORD)
            }
    
            debug(dbg, "  word ending rate: $wordEnding, normPostag: ${normPostag} = rate: ${rnd(rate)}")
        }
        
        return rate
    }
    
    @CompileStatic
    private double getPostagRate(AnalyzedToken reading, AnalyzedTokenReadings[] tokens, int idx, boolean withXp, boolean dbg, double total, double ctxQ) {
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
            rate = adjustByContext(rate, normPostag, stat.ctxRates, tokens, idx, dbg, ctxQ, ContextMode.TAG)
        }

        if( rate && withXp ) {
            rate = adjustByXp(rate, postag, reading, dbg)
        }        
        
        debug(dbg, "  tag rate: $normPostag, idx: $idx: oldRate: ${rnd(oldRate)}, rate: ${rnd(rate)}")

        return rate
    }

    @CompileStatic
    double adjustByXp(double rate, String postag, AnalyzedToken reading, boolean dbg) {
        if( postag.contains(":xp") ) {
            String xp = (postag =~ /xp[0-9]/)[0]
            String lemma = reading.getLemma()
            Double mi = statsForLemmaXp[lemma][xp]
            if( mi != null ) {
                mi = mi/(double)10.0 + 1
                debug(dbg, "    xp for $lemma, $xp with q=$mi")
                rate *= mi
            }
        }
        rate
    } 
    
    private static final Pattern POSTAG_NORM_PATTERN = ~ /:(xp[1-9]|ua_[0-9]{4}|comp.|&predic|&insert|coll|rare|vulg)/
    
    @CompileStatic
    private static String normalizePostagForRate(String postag) {
        POSTAG_NORM_PATTERN.matcher(postag).replaceAll('')
    }

    @CompileStatic
    private static boolean contextMatches(WordContext wctx1, WordContext currContext, ContextMode ctxMode) {
                    wctx1.offset == currContext.offset \
                    && ((wctx1.contextToken.word == currContext.contextToken.word) 
                    || (ctxMode == ContextMode.TAG
                        && (wctx1.contextToken.postag == currContext.contextToken.postag 
                        && currContext.contextToken.postag =~ /^(adj|verb|noun|advp)/ ) 
                        ))
    }

    enum ContextMode { WORD, TAG }
    
    @CompileStatic
    private <T> double adjustByContext(double rate, T key, Map<WordContext, Double> ctxStats, AnalyzedTokenReadings[] tokens, int idx, boolean dbg, double ctxCoeff, ContextMode ctxMode) {
        if( ! (DisambigModule.context in options.disambiguate) )
            return rate
        
        boolean useRightContext = false 
                    
        Set<WordContext> currContextsPrev = createWordContext(tokens, idx, -1)
        Set<WordContext> currContextsNext = useRightContext ? createWordContext(tokens, idx, +1) : null
        
        // TODO: limit previous tokens by ratings already applied?
        def matchedContexts = ctxStats
            .findAll {WordContext wc, Double v2 -> v2
                currContextsPrev.find { currContext -> contextMatches(wc, currContext, ctxMode) } \
                    || (useRightContext && currContextsNext.find { currContext -> contextMatches(wc, currContext, ctxMode) } )
            }
        
//        debug(dbg, "  matched ctx: $matchedContexts")
            
        // if any (previous) readings match the context add its context rate
        Double matchRateSum = (Double) matchedContexts.collect{k,v -> v}.sum(0.0) /// (double)matchedContexts.size()
        Set<Integer> matchedOffsets = useRightContext ? matchedContexts.collect{k,v -> k.offset} as Set : [-1] as Set

        if( matchRateSum ) {
            // normalize context rate to main rate and give it a weight
            matchRateSum /= matchedOffsets.size()
            double adjust = (matchRateSum / rate) * ctxCoeff + 1
            double oldRate = rate
            rate *= adjust 
            debug(dbg, "    ctx for : $key, ${matchedContexts.size()} ctxs, x: ${rnd(adjust)}")//, old: ${rnd(oldRate)} -> withCtx: ${rnd(rate)}")
        }

        rate
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
    static boolean isIgnorableCtx(AnalyzedTokenReadings token) {
        token.getCleanToken() in ContextToken.IGNORE_TOKENS
    }

    @CompileStatic
    static AnalyzedTokenReadings findCtx(AnalyzedTokenReadings[] tokensXml, int pos, int offset) {
        for( ; pos+offset >= 0 && pos+offset < tokensXml.size()-1; offset++) {
            if( ! isIgnorableCtx(tokensXml[pos+offset]) ) // TODO: stop at 1 skipped?
                return tokensXml[pos+offset]
        }
        return null
    }

    @CompileStatic
    private static Set<WordContext> createWordContext(AnalyzedTokenReadings[] tokens, int idx, int offset) {
        
        AnalyzedTokenReadings ctxTokenReading = findCtx(tokens, idx, offset)
        
        ContextToken contextToken
        if( ctxTokenReading != null ) {
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
        else {
            contextToken = offset<0 ? new ContextToken('__BEG', '', 'BEG') : new ContextToken('__END', '', 'END')
        }

        [new WordContext(contextToken, offset)] as Set
    }

    
    @CompileStatic
    def loadDisambigStats() {
        if( statsByWord.size() > 0 )
            return

        long tm1 = System.currentTimeMillis()

        def statsFile = getClass().getResource("/ua/net/nlp/tools/stats/lemma_freqs_hom.txt")
        if( statsFile == null ) {
            System.err.println "Disambiguation stats not found"
            System.exit 1
        }
        
//        def statDir = new File(new File((String)SCRIPT_DIR + "/../../../../../../../.."), "stats")
//        if( ! statDir.isDirectory() ) {
//            System.err.println "Disambiguation stats not found in ${statDir.name}"
//            System.exit 1
//        }
//        def statsFile = new File(statDir, "lemma_freqs_hom.txt")
        
        String word
        String wordEnding
        Stat wordEndingStat
        WordReading wordReading
        String postagNorm

        statsFile.eachLine { String line ->
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
                    if( ! (xp in statsForLemmaXp[lemma] ) ) {
                        statsForLemmaXp[lemma][xp] = Double.valueOf(0)
                    }
                    statsForLemmaXp[lemma][xp] += rate
                }

                wordEnding = null
                if( true || DisambigModule.wordEnding in options.disambiguate ) {
                    wordEnding = getWordEnding(word, postagNorm)
                    if( wordEnding ) {
                        wordEndingStat = statsByWordEnding.computeIfAbsent(wordEnding, {s -> new HashMap<>()})
                            .computeIfAbsent(postagNorm, {x -> new Stat()})
                        wordEndingStat.rate += rate
                    }
                }
    
                return
            }

            // context line
            int pos = p[1] as int
            // TODO: temp
//            if( pos == +1 ) return
            
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
                wordEndingStat.ctxRates.computeIfAbsent(wordContext, {Double.valueOf(0)})
                wordEndingStat.ctxRates[wordContext] += ctxCnt
            }

            // aggregate postag contexts
            def statsByTagEntry = statsByTag[postagNorm]
            statsByTagEntry.ctxRates.computeIfAbsent(wordContext, {Double.valueOf(0)})
            statsByTagEntry.ctxRates[wordContext] += ctxCnt
        }

        normalizeRates()

        long tm2 = System.currentTimeMillis()
        System.err.println("Loaded ${statsByWord.size()} disambiguation stats, ${statsByTag.size()} tags, ${statsByWordEnding.size()} endings, ${statsForLemmaXp.size()} xps in ${tm2-tm1} ms")
        
        if( dbg ) {
            writeDerivedStats()
        }
        
    }

    @CompileStatic
    void normalizeRates() {
        double statsByTagTotal = (Double) statsByTag.collect { k,v -> v.rate }.sum()
        
        // normalize tag rates
        statsByTag.each { String tag, Stat v ->
            v.rate /= statsByTagTotal
            v.ctxRates.entrySet().each { e -> e.value /= (double)statsByTagTotal }
        }

        double statsByWordEndingTotal = (Double) statsByWordEnding.collect { k,v -> v.collect{ k2, v2 -> v2.rate}.sum() }.sum()

//        println ":: tagTotal: $statsByTagTotal, wordEndingTotal: $statsByWordEndingTotal"

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
    }
    
    @CompileStatic
    void writeDerivedStats() {
        def derivedStatsDir = "tmp"
        
        File tff = new File("$derivedStatsDir/tag_freqs.txt")
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

        tff = new File("$derivedStatsDir/tag_ending_freqs.txt")
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

        tff = new File("$derivedStatsDir/tag_xp_freqs.txt")
        tff.text = ''
        statsForLemmaXp
                .sort{ a, b -> a.key.compareTo(b.key) }
                .each { String lemma, Map<String, Double> v ->
                    String vvv = v.collect { xp, v2 ->
                        "$lemma\t$xp\t$v2"
                    }.join("\n")
                    tff << "$vvv\n"
                }

    }
    
    
    @CompileStatic
    static String getWordEnding(String word, String postag) {
        int endLength = 3
        if( postag != null
                && ! postag.contains(":nv")
                && postag =~ /^(noun|adj|verb|adv)/
                && ! word.endsWith('.') 
                && word.length() > endLength 
                && word =~ /[а-яіїєґ]$/ 
                && ! (word =~ /-(що|бо|то|от|но|таки)$/) ) {

            String wordEnding = word
            if( postag.startsWith("verb:rev") || postag.startsWith("advp:rev") ) { 
                endLength = 5 
            }
            else if( word[-endLength] == '-' ) {
                endLength += 1
            }

            if( wordEnding.length() > endLength ) {
                wordEnding = wordEnding[-endLength..-1]

                return wordEnding
            }
        }
        
        null
    }
    
    void printStats(disambigMap) {
        BigDecimal unknown = disambigMap['total'] ? disambigMap['noWord'] * 100 / disambigMap['total'] : 0
        unknown = unknown.round(0)
        //println "Disambig stats: ${disambigMap}: unknown: ${unknown}%"
    }
}
