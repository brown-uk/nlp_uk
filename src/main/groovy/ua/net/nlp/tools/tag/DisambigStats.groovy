package ua.net.nlp.tools.tag;

import java.util.function.Consumer
import java.util.regex.Pattern

import org.languagetool.AnalyzedToken
import org.languagetool.AnalyzedTokenReadings

import groovy.transform.CompileStatic
import groovy.transform.ToString
import ua.net.nlp.bruk.ContextToken
import ua.net.nlp.bruk.WordContext
import ua.net.nlp.bruk.WordReading
import ua.net.nlp.tools.tag.TagTextCore
import ua.net.nlp.tools.tag.TagOptions


public class DisambigStats {
    private static final Pattern UPPERCASED_PATTERN = Pattern.compile(/[А-ЯІЇЄҐ][а-яіїєґ'-]+/)
    private static final boolean USE_SUFFIX_2 = false
    private static final String statsFile = "/ua/net/nlp/tools/stats/lemma_freqs_hom.txt"
    static final String statsVersion = "3.1.1"
    private static final boolean useRightContext = false
    

    boolean disambigBySuffix = true //DisambigModule.wordEnding in options.disambiguate
    boolean disambigByContext = true
    boolean writeDerivedStats = false

    File debugStatsFile = null
    ThreadLocal<List<String>> debugStatsLines = new ThreadLocal<List<String>>() {
        protected List<String> initialValue() {
            return []
        }
    }
    
    TagOptions options
    
    @CompileStatic
    @ToString
    static class Stat {
        double rate = 0.0
        Map<WordContext, Double> ctxRates = [:]
    }
    
    DisambigStats() {
        if( debugStatsFile ) {
            debugStatsFile.text = ''
        }
    }
    
    void setOptions(TagOptions options) {
        if( options.disambiguationDebug ) {
            debugStatsFile = new File("stats_dbg.txt")
            debugStatsFile.text = ''
        }
        this.options = options
    }

    @CompileStatic    
    void debugStats(String msg, Object... args) {
        if( debugStatsFile ) { 
            debugStatsLines.get() << String.sprintf(msg, args)
        }
    }
    @CompileStatic    
    void debugStatsFlush() {
        if( debugStatsFile ) {
            synchronized (debugStatsFile) {
                debugStatsFile << debugStatsLines.get().join("\n") << "\n"
//                debugStatsFile.withWriter { w -> w.flush() }
                debugStatsLines.get().clear()
            }
        }
    }

    // word -> wordReading -> rate/ctxRates
    Map<String, Map<WordReading, Stat>> statsByWord = new HashMap<>(16*1024)
    // wordEnding -> tag -> rate/ctxRates
    Map<String, Map<String, Stat>> statsBySuffix3 = new HashMap<>(3*1024)
    // wordEnding -> tag -> rate/ctxRates
    Map<String, Map<String, Stat>> statsBySuffix2 = new HashMap<>(3*1024)
    // tag -> rate/ctxRates
    Map<String, Stat> statsByTag = new HashMap<>(1*1024)
    // lemma_xpN -> rate
    Map<String, Map<String, Double>> statsForLemmaXp = new HashMap<>(256).withDefault{ new HashMap<>() }
    
    @groovy.transform.SourceURI
    static SOURCE_URI
    // if this script is called from GroovyScriptEngine SourceURI is data: and does not work for File()
    static File SCRIPT_DIR = SOURCE_URI.scheme == "data"
        ? null // new File("src/main/groovy/ua/net/nlp/tools/tag")
        : new File(SOURCE_URI).getParentFile()

    @CompileStatic
    static double round(double d) {
        //BigDecimal.valueOf(d).setScale(4, RoundingMode.CEILING)
        d
    }

        
    @CompileStatic
    List<Double> orderByStats(List<AnalyzedToken> readings, String cleanToken, AnalyzedTokenReadings[] tokens, int idx, TagStats stats) {

        String origToken = cleanToken
        
        if( ! statsByWord.containsKey(cleanToken) ) {
            // if no stats and there are non-prop readings, then try lowercase
            if( UPPERCASED_PATTERN.matcher(cleanToken).matches() 
                    && readings.size() > readings.count{ r -> r.getPOSTag() == null || r.getPOSTag().contains(":prop") }  ) {
                cleanToken = cleanToken.toLowerCase()
            }
        }
        
        debugStats("%s", cleanToken + (!cleanToken.equals(origToken) ? " Lc" : ""))
        debugStats("  -1: %s", idx == 0 ? "^" : tokens[idx-1].getCleanToken())
        
        
        Map<WordReading, Stat> statsForWord = statsByWord[cleanToken]

        stats.disambigMap['total'] ++

        updateDebugStats(readings, cleanToken, stats, statsForWord)
                

        boolean withXp = true
        double tagRateSum = (double) readings.collect { anToken -> getRateByTag(anToken, tokens, idx, withXp, 0, 0) }.sum()
        double sfx3RateSum = disambigBySuffix ? (double) readings.collect { anToken -> getRateBySuffix(anToken, cleanToken, tokens, idx, 0, 0, 3) }.sum() : 0
        double sfx2RateSum = disambigBySuffix ? (double) readings.collect { anToken -> getRateBySuffix(anToken, cleanToken, tokens, idx, 0, 0, 2) }.sum() : 0
        debugStats("  readings: ${readings.size()}, tag total: ${round(tagRateSum)}, sfx3 total: ${round(sfx2RateSum)}, sfx2 total: ${round(sfx2RateSum)}")

        int i=0
        def rates0 = readings.collect { anToken ->
            
            debugStats("    %s / %s", anToken.getPOSTag(), anToken.getLemma())
            
            double ctxQ_ = 1e4
            double wordRate = getRateByWord(anToken, cleanToken, statsForWord, tokens, idx, ctxQ_)
            double rate = wordRate

            boolean prevPrep = idx > 0 && hasPosTag(tokens[idx-1], "prep")
            boolean unforceTag = ! prevPrep && tokens[idx].getCleanToken().endsWith("ів")

            boolean wordEndingUsed = false
            if( disambigBySuffix ) {
                boolean useNext3 = true || ! rate
                if( useNext3 && sfx3RateSum ) {
                    double ctxQ = 6.0e7
                    double sfxRate = getRateBySuffix(anToken, cleanToken, tokens, idx, sfx3RateSum, ctxQ, 3)
                    
                    boolean useNext2 = USE_SUFFIX_2 // && ! wordEndingRate //|| ! rate
                    if( useNext2 && sfx2RateSum ) {
                        sfxRate = getRateBySuffix(anToken, cleanToken, tokens, idx, sfx2RateSum, ctxQ, 2)
                    }
                    if( sfxRate ) {
                        sfxRate /= 6.1e3
//                        sfxRate /= unforceTag ? 6.1e4 : 6.1e3
                        debugStats("      sfx3 rate: -> %f", round(sfxRate))
                        rate += sfxRate
                        wordEndingUsed = true
                    }
                }
            }
            
            boolean useNext = true || ! rate
            if( useNext && tagRateSum ) {
                double ctxQ = 6.0e7 // 4.5e7
                double postagRate = getRateByTag(anToken, tokens, idx, withXp, tagRateSum, ctxQ)
                if( postagRate ) {
                    postagRate /= unforceTag ? 6.1e4 : 6.2e3
                    debugStats("      tag rate: -> %f", round(postagRate))
                    rate += postagRate
                }
            }
            
            debugStats("    final: %f\n", round(rate))
            
            [idx: i++, rate: rate, reading: anToken]
        }

//        debugStats("")
        
//        debugStatsFlush()
        
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
    static boolean hasPosTag(AnalyzedTokenReadings tokenReadings, String tag) {
        tokenReadings.getReadings().find { it -> it.getPOSTag() != null && it.getPOSTag().startsWith(tag) } 
    }
    
    @CompileStatic    
    void updateDebugStats(List<AnalyzedToken> readings, String cleanToken, TagStats stats, Map<WordReading, Stat> statsForWord) {
        if( statsForWord != null ) {
            debugStats("  word stats: yes")
//            debug("found word stats for $cleanToken / $readings")
            stats.disambigMap['word'] ++
        }
        else {
            debugStats("  word stats: no")
            stats.disambigMap['noWord'] ++
            
            boolean suffixStats3 = false
            
            for(AnalyzedToken r: readings) {
                String wordEnding = getWordEnding(cleanToken, r.getPOSTag(), 3)
                if( wordEnding in statsBySuffix3 ) {
                    stats.disambigMap['wordEnd'] ++
                    suffixStats3 = true
                    break;
                }
            }
            debugStats("  sfx3 stats: %s", suffixStats3 ? "yes" : "no")

            if( USE_SUFFIX_2 ) {
                if( ! suffixStats3 ) {
                    boolean suffixStats2 = false
                    
                    for(AnalyzedToken r: readings) {
                        String wordEnding = getWordEnding(cleanToken, r.getPOSTag(), 2)
                        if( wordEnding in statsBySuffix2 ) {
                            stats.disambigMap['sfx2'] ++
                            suffixStats2 = true
                            break;
                        }
                    }
                    debugStats("  sfx2 stats: %s", suffixStats2 ? "yes" : "no")
                }
            }
        }
    }
    
    @CompileStatic
    private double getRateByWord(AnalyzedToken at, String cleanToken, Map<WordReading, Stat> stats, AnalyzedTokenReadings[] tokens, int idx, double ctxQ) {
            
        if( stats == null )
            return 0

        WordReading wordReading = new WordReading(at.getLemma(), at.getPOSTag())
        Stat r = stats[wordReading]
        Double rate = r ? r.rate : 0

        debugStats("      wrd rate: %f", round(rate))
        
        def oldRate = rate
        if( rate ) {
            rate = adjustByContext(rate, wordReading, r.ctxRates, tokens, idx, ctxQ, ContextMode.WORD, at.getPOSTag())
            debugStats("      wrd rate -> %f", round(rate))
        }
        
        return rate
    }

    @CompileStatic
    private double getRateBySuffix(AnalyzedToken at, String cleanToken, AnalyzedTokenReadings[] tokens, int idx, double total, double ctxQ, int len) {

        double rate = 0
        String postag = at.getPOSTag()
        String wordEnding = getWordEnding(cleanToken, postag, len)
        if( ! wordEnding )
            return 0
        
        Map<String, Stat> statsForWordEnding = len == 3 ? statsBySuffix3[wordEnding] :  statsBySuffix2[wordEnding]
        if( statsForWordEnding != null ) {
            String normPostag = normalizePostagForRate(postag)
            Stat stat = statsForWordEnding[normPostag]
            
            if( ! stat ) {
//            println "WARN: no stats for tag: $wordEnding"
                return 0
            }
                
            rate = stat.rate
            if( total ) {
                rate /= 3.5d
                ctxQ *= 2d
                rate /= total
            }
            assert rate <= 1.0, "total: $total"

            double oldRate = rate

            if( total )
                debugStats("      sfx$len rate: %f ($wordEnding / $normPostag)", round(oldRate))

            if( rate && total ) {
                rate = adjustByContext(rate, wordEnding, stat.ctxRates, tokens, idx, ctxQ, ContextMode.TAG, normPostag)
            }
    
//            if( total && oldRate != rate )
//                debugStats("      sfx$len rate: -> %f", round(rate))
        }
        
        return rate
    }
    
    @CompileStatic
    private double getRateByTag(AnalyzedToken reading, AnalyzedTokenReadings[] tokens, int idx, boolean withXp, double total, double ctxQ) {
        String postag = reading.getPOSTag()
        String normPostag = normalizePostagForRate(postag)
        def stat = statsByTag[normPostag]
        
        if( ! stat ) {
//            println "WARN: no stats for tag: $normPostag"
            return 0
        }
        
        double rate = stat.rate
        if( total ) {
            rate /= 3.5d
            ctxQ *= 2d
            rate /= total
        }
        assert rate <= 1.0
        
        double oldRate = rate

        if( total )
            debugStats("      tag rate: %f", round(oldRate))

        if( rate && total ) {
            rate = adjustByContext(rate, normPostag, stat.ctxRates, tokens, idx, ctxQ, ContextMode.TAG, normPostag)
        }

        if( rate && withXp ) {
            rate = adjustByXp(rate, postag, reading)
        }        
        
//        if( total && oldRate != rate )
//            debugStats("      tag rate: -> %f", round(rate))
//        debug(dbg, "  tag rate: $normPostag, idx: $idx: oldRate: ${round(oldRate)}, rate: ${round(rate)}")

        return rate
    }

    @CompileStatic
    double adjustByXp(double rate, String postag, AnalyzedToken reading) {
        if( postag.contains(":xp") ) {
            String xp = (postag =~ /xp[0-9]/)[0]
            String lemma = reading.getLemma()
            Double mi = statsForLemmaXp[lemma][xp]
            if( mi != null ) {
                mi = mi/(double)10.0 + 1
                debugStats("    xp for $lemma, $xp with q=$mi")
                rate *= mi
            }
        }
        rate
    } 
    
    private static final Pattern POSTAG_NORM_PATTERN = ~ /:(xp[1-9]|ua_[0-9]{4}|comp.|&predic|&insert|vulg|coll)/
    
    @CompileStatic
    private static String normalizePostagForRate(String postag) {
        POSTAG_NORM_PATTERN.matcher(postag).replaceAll('')
    }

    @CompileStatic
    private static boolean contextMatches(WordContext wctx1, WordContext currContext, ContextMode ctxMode, String normPostag) {
        wctx1.offset == currContext.offset \
                && (
                    wctx1.contextToken.word == currContext.contextToken.word 
                    || (ctxMode == ContextMode.TAG
                        && contextMatchesByTag(wctx1, currContext, normPostag)) )
    }

    @CompileStatic
    private static boolean contextMatchesByTag(WordContext wctx1, WordContext currContext, String normPostag) {
        if( currContext.contextToken.postag =~ /^(adj|noun|verb|advp)/ ) {
            return wctx1.contextToken.postag == currContext.contextToken.postag
        }

        return false
    }
    
    @CompileStatic
    private static boolean contextMatchesLemma(WordContext wctx1, WordContext currContext, String normPostag) {
        if( normPostag =~ /^(adj|noun)(?!.*v_naz)/
                && wctx1.contextToken.postag =~ /^(verb)/
                && currContext.contextToken.postag =~ /^(verb)/ ) {
//            if( wctx1.contextToken.lemma == currContext.contextToken.lemma )
//                debugStats("      verb: $normPostag  for lemma ${wctx1.contextToken.lemma} ${wctx1.contextToken.lemma == currContext.contextToken.lemma}")
            return wctx1.contextToken.lemma == currContext.contextToken.lemma
        }
        return false
    }
    
    enum ContextMode { WORD, TAG }
    
    @CompileStatic
    private <T> double adjustByContext(double rate, T key, Map<WordContext, Double> ctxStats, AnalyzedTokenReadings[] tokens, int idx, double ctxCoeff, ContextMode ctxMode, String normPostag) {
        if( ! disambigByContext )
            return rate
        
        Set<WordContext> currContextsPrev = createWordContext(tokens, idx, -1)
        Set<WordContext> currContextsNext = useRightContext ? createWordContext(tokens, idx, +1) : null
        
        // TODO: limit previous tokens by ratings already applied?
        Map<WordContext, Double> matchedContexts = ctxStats
            .findAll {WordContext wc, Double v2 -> v2
                currContextsPrev.find { currContext -> contextMatches(wc, currContext, ctxMode, normPostag) } \
                    || (useRightContext && currContextsNext.find { currContext -> contextMatches(wc, currContext, ctxMode, normPostag) } )
            }

        if( matchedContexts ) {
//            def str = matchedContexts.collect{ k,v -> "$k: $v"}.join("\n          ")
//            debugStats("        ctxs:\n          %s", str)
        }
        
        // if any (previous) readings match the context add its context rate
        Double matchRateSum = (Double) matchedContexts.collect{k,v -> v}.sum(0.0) /// (double)matchedContexts.size()

        
        Map<WordContext, Double> matchedContextsLemma = ctxStats
            .findAll {WordContext wc, Double v2 -> v2
                currContextsPrev.find { currContext -> contextMatchesLemma(wc, currContext, normPostag) }
            }

        if( matchedContextsLemma ) {
            def str = matchedContextsLemma.collect{ k,v -> "$k: $v"}.join("\n          ")
            debugStats("        ctxsL:\n          %s", str)
            
            matchRateSum += ((Double) matchedContextsLemma.collect{k,v -> v}.sum(0.0)) * 100.0
        }
        
        matchRateSum = adjustByContextAdjNoun(rate, key, tokens, idx, ctxMode, matchRateSum)
        
        Set<Integer> matchedOffsets = useRightContext ? matchedContexts.collect{k,v -> k.offset} as Set : [-1] as Set

        if( matchRateSum ) {
            // normalize context rate to main rate and give it a weight
            matchRateSum /= matchedOffsets.size()
            double oldRate = rate
//            double adjust = (matchRateSum / rate) * ctxCoeff + 1
//            rate *= adjust
            double adjust = matchRateSum * ctxCoeff
            rate += adjust
            debugStats("        ctxs: ${matchedContexts.size()}, mrSum: $matchRateSum, adjust: ${round(adjust)} -> rate: ${rate}")
        }
        else {
//            debugStats("        ctxs: 0")
        }

        rate
    }

    
    @CompileStatic
    private <T> double adjustByContextAdjNoun(double rate, T key, AnalyzedTokenReadings[] tokens, int idx, ContextMode ctxMode, Double matchRateSum) {
        if( ctxMode == ContextMode.TAG 
                && idx < tokens.length-1 
                && matchRateSum < 1 ) {

            String tag = (String)key;
            if( tag.startsWith("adj") && tokens[idx].getCleanToken().toLowerCase() != "та" ) {
                String genRegex = "^noun"
                if( tag.contains("ranim") ) genRegex += ":(un)?anim"
                else if( tag.contains("rinanim") ) genRegex += ":(un|in)anim"
                else genRegex += ":([ui]n)?anim"
                genRegex += tag[3..<11]
//                println ":: $tag / $genRegex"
                
                def wcF = tokens[idx+1].getReadings().find { AnalyzedToken at -> at.getPOSTag() =~ genRegex }
                if( wcF ) {
//                    println ":: found ${wcF} : ${matchRateSum} -> ${matchRateSum += 0.5}"
                    matchRateSum += 0.35
                }
            }
        }
        matchRateSum
    }
        
    @CompileStatic
    static String getTag(String theToken, String tag) {
        if( TagTextCore.PUNCT_PATTERN.matcher(theToken).matches() ) {
            return 'punct'
        }
        else if( TagTextCore.SYMBOL_PATTERN.matcher(theToken).matches() ) {
            return 'symb'
        }
        else if( TagTextCore.XML_TAG_PATTERN.matcher(theToken).matches() ) {
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
                    def normalizedTokenLemma = ContextToken.normalizeContextString(tokenReading.getLemma(), '', postag)
                    contextToken = new ContextToken(normalizedTokenValue, normalizedTokenLemma, postag)
                    new WordContext(contextToken, offset)
                } as Set
        }
        else {
            contextToken = offset<0 ? new ContextToken('__BEG', '', 'BEG') : new ContextToken('__END', '', 'END')
        }

        [new WordContext(contextToken, offset)] as Set
    }

    void download() {
        if( SCRIPT_DIR == null ) { // should not happen - jar will bundle the stats
            System.err.println "Can't download from inside the jar"
            System.exit 1
        }
        
        def targetDir = new File(SCRIPT_DIR, "../../../../../../resources/")
        targetDir.mkdirs()
        assert targetDir.isDirectory()

        File targetFile = new File(targetDir, statsFile)
        targetFile.parentFile.mkdirs()
        
        def remoteStats = "https://github.com/brown-uk/nlp_uk/releases/download/v${statsVersion}/lemma_freqs_hom.txt"
        System.err.println("Downloading $remoteStats...");
        def statTxt = new URL(remoteStats).getText('UTF-8')
        
        targetFile.setText(statTxt, 'UTF-8')
    }
    
    
    @CompileStatic
    def loadDisambigStats() {
        if( statsByWord.size() > 0 )
            return

        long tm1 = System.currentTimeMillis()

        def statsFileRes = getClass().getResource(statsFile)
        if( statsFileRes == null ) {
            System.err.println "Disambiguation stats not found, run \"TagText.groovy --download\" to download it from github, and then retry"
            System.exit 1
        }
        
        
        String word
        WordReading wordReading
        String postagNorm

        String wordSuffix3
        String wordSuffix2
        Stat wordEndingStat
        Stat wordSuffix2Stat
        int lineNum = 0

        statsFileRes.eachLine { String line ->
            if( lineNum++ == 0 ) {
                def starter = "# version: "
                def statsVersionFound = ""
                if( line.startsWith(starter) ) {
                    statsVersionFound = (line - starter).trim()                
                }
                if( statsVersion != statsVersionFound ) {
                    System.err.println "Disambiguation stats version mismatch, expected \"$statsVersion\", run \"TagText.groovy --download\" to download it from github, and then retry"
                    System.exit 1
                }
                return
            }
            
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

                wordSuffix3 = null
                wordSuffix2 = null
                if( disambigBySuffix ) {
                    wordSuffix3 = getWordEnding(word, postagNorm, 3)
                    if( wordSuffix3 ) {
                        wordEndingStat = statsBySuffix3.computeIfAbsent(wordSuffix3, {s -> new HashMap<>()})
                            .computeIfAbsent(postagNorm, {x -> new Stat()})
                        wordEndingStat.rate += rate
                    }
                    if( USE_SUFFIX_2 ) {
                        wordSuffix2 = getWordEnding(word, postagNorm, 2)
                        if( wordSuffix2 ) {
                            wordSuffix2Stat = statsBySuffix2.computeIfAbsent(wordSuffix2, {s -> new HashMap<>()})
                            .computeIfAbsent(postagNorm, {x -> new Stat()})
                            wordSuffix2Stat.rate += rate
                        }
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

            // aggregate suffix contexts
            if( wordSuffix3 ) {
                wordEndingStat.ctxRates.computeIfAbsent(wordContext, {Double.valueOf(0)})
                wordEndingStat.ctxRates[wordContext] += ctxCnt
            }
            // aggregate suffix contexts
            if( wordSuffix2 ) {
                wordSuffix2Stat.ctxRates.computeIfAbsent(wordContext, {Double.valueOf(0)})
                wordSuffix2Stat.ctxRates[wordContext] += ctxCnt
            }

            // aggregate postag contexts
            def statsByTagEntry = statsByTag[postagNorm]
            statsByTagEntry.ctxRates.computeIfAbsent(wordContext, {Double.valueOf(0)})
            statsByTagEntry.ctxRates[wordContext] += ctxCnt
        }

        long tm12 = System.currentTimeMillis()
        
        normalizeRates()

        long tm2 = System.currentTimeMillis()
        System.err.println("Loaded ${statsByWord.size()} disambiguation stats, ${statsByTag.size()} tags, ${statsBySuffix3.size()} suffix-3, ${statsBySuffix2.size()} suffix-2, ${statsForLemmaXp.size()} xps in ${tm2-tm1} ms")
        
        if( writeDerivedStats ) {
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

        double statsBySuffix3Total = (Double) statsBySuffix3.collect { k,v -> v.collect{ k2, v2 -> v2.rate}.sum() }.sum()
        
//        println ":: tagTotal: $statsByTagTotal, wordEndingTotal: $statsByWordEndingTotal"

        // normalize suffix rates
        statsBySuffix3.each { String wordEnding2, Map<String, Stat> v ->
            v.each { String tag, Stat s ->
                s.rate /= statsBySuffix3Total
                s.ctxRates.entrySet().each { e -> e.value /= (double)statsBySuffix3Total }
            }
        }
        
        if( USE_SUFFIX_2 ) {
            // normalize suffix rates
            double statsBySuffix2Total = (Double) statsBySuffix2.collect { k,v -> v.collect{ k2, v2 -> v2.rate}.sum() }.sum()
            statsBySuffix2.each { String wordEnding2, Map<String, Stat> v ->
                v.each { String tag, Stat s ->
                    s.rate /= statsBySuffix2Total
                    s.ctxRates.entrySet().each { e -> e.value /= (double)statsBySuffix2Total }
                }
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
        statsBySuffix3
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
                
        tff = new File("$derivedStatsDir/tag_sfx2_freqs.txt")
        tff.text = ''

        if( USE_SUFFIX_2 ) {
            statsBySuffix2
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
    static String getWordEnding(String word, String postag, int endLength) {
        if( postag != null
                && ! postag.contains(":nv")
                && postag =~ /^(noun|adj|verb|adv)/
                && ! word.endsWith('.') 
                && word.length() > endLength 
                && word =~ /[а-яіїєґ]$/ 
                && ! (word =~ /-(що|бо|то|от|но|таки)$/) ) {

            String wordEnding = word
            if( postag.startsWith("verb:rev") || postag.startsWith("advp:rev") ) { 
                endLength = endLength + 2
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
