package ua.net.nlp.tools.tag

import java.util.regex.Pattern

import org.languagetool.AnalyzedSentence
import org.languagetool.AnalyzedToken
import org.languagetool.AnalyzedTokenReadings
import org.languagetool.JLanguageTool
import ua.net.nlp.tools.tag.TagTextCore
import ua.net.nlp.tools.tag.TagTextCore.TTR
import ua.net.nlp.tools.tag.TagTextCore.TaggedToken
import ua.net.nlp.tools.tag.TagOptions

import groovy.transform.CompileStatic


public class TagStats {
    static final Pattern CYR_LETTER = Pattern.compile(/[а-яіїєґА-ЯІЇЄҐ]/)
    static final Pattern ONLY_CYR_LETTER = Pattern.compile(/[а-яіїєґА-ЯІЇЄҐ'-]+/)
    static final Pattern IGNORE_TAGS_FOR_LEMMA = Pattern.compile(/arch|alt|short|long|bad/)
//    static final Pattern NON_UK_LETTER = Pattern.compile(/[ыэъёЫЭЪЁ]|ие|ИЕ|ннн|оі$|[а-яіїєґА-ЯІЇЄҐ]'?[a-zA-Z]|[a-zA-Z][а-яіїєґА-ЯІЇЄҐ]/)

    TagOptions options
    
    Map<String, Integer> homonymFreqMap = [:].withDefault { 0 }
    Map<String, Set<String>> homonymTokenMap = [:].withDefault{ new LinkedHashSet<>() }
    Map<String, Integer> unknownMap = [:].withDefault { 0 }
    Map<String, Integer> unclassMap = [:].withDefault { 0 }
    Map<String, Integer> frequencyMap = [:].withDefault { 0 }
    Map<String, Integer> lemmaFrequencyMap = [:].withDefault { 0 }
    Map<String, Set<String>> lemmaFrequencyPostagsMap = [:].withDefault { [] as Set }
    Set lemmaAmbigs = new HashSet<>()
    Map<String, Integer> knownMap = [:].withDefault { 0 }
    int knownCnt = 0

    Map<String, Integer> disambigMap = [:].withDefault { 0 }
    
    synchronized void add(TagStats stats) {
        stats.homonymFreqMap.each { k,v -> homonymFreqMap[k] += v }
        stats.homonymTokenMap.each { k,v -> homonymTokenMap[k].addAll(v) }
        stats.unknownMap.each { k,v -> unknownMap[k] += v }
        stats.unclassMap.each { k,v -> unclassMap[k] += v }
        stats.knownMap.each { k,v -> knownMap[k] += v }
        stats.frequencyMap.each { k,v -> frequencyMap[k] += v }
        stats.lemmaFrequencyMap.each { k,v -> lemmaFrequencyMap[k] += v }
        stats.lemmaFrequencyPostagsMap.each { k,v -> lemmaFrequencyPostagsMap[k] += v }
        lemmaAmbigs.addAll(stats.lemmaAmbigs)
        knownCnt += stats.knownCnt
        
        stats.disambigMap.each { k,v -> disambigMap[k] += v }
    }

    @CompileStatic
    def collectUnknown(List<List<TTR>> analyzedSentences) {
        for (List<TTR> sentTTR : analyzedSentences) {
            // if any words contain Russian sequence filter out the whole sentence - this removes tons of Russian words from our unknown list
            // we could also test each word against Russian dictionary but that would filter out some valid Ukrainian words too
            
            if( options.filterUnknown ) {
                def unknownBad = sentTTR.any { TTR ttr ->
                    def value = ttr.tokens[0].value
                    value = value.toLowerCase()
                    value.indexOf('еи') >= 0 || value =~ /[ыэёъ]/ || value ==~ /и|не|что/
                }
                if( unknownBad )
                    continue
            }

            sentTTR.each { TTR ttr ->
                TaggedToken tk = ttr.tokens[0]
                if( tk.tags == 'unknown' ) {
                    unknownMap[tk.value] += 1
                }
                else if( tk.tags == 'unclass' ) {
                    unclassMap[tk.value] += 1
                }
                else if( tk.value =~ CYR_LETTER ) {
                    knownCnt++
                    knownMap[tk.value] += 1
                }
            }
        }
    }


    @CompileStatic
    def collectHomonyms(List<AnalyzedSentence> analyzedSentences) {

        for (AnalyzedSentence analyzedSentence : analyzedSentences) {
            for(AnalyzedTokenReadings readings: analyzedSentence.getTokens()) {
                if( readings.getReadingsLength() < 2 )
                    continue

                List<AnalyzedToken> tokenReadings = new ArrayList<>(readings.getReadings())
                tokenReadings.removeIf{ AnalyzedToken it -> 
                    it.getPOSTag() == null \
                        || it.getPOSTag() in [JLanguageTool.SENTENCE_END_TAGNAME, JLanguageTool.PARAGRAPH_END_TAGNAME] \
                        || it.getPOSTag().startsWith('<') 
                }

                if( tokenReadings.size() < 2 )
                    continue

                def key = tokenReadings.join("|")
                homonymFreqMap[key] += 1
                homonymTokenMap[key] << readings.getCleanToken()
            }
        }
    }


    @CompileStatic
    def collectFrequency(List<AnalyzedSentence> analyzedSentences) {
        for (AnalyzedSentence analyzedSentence : analyzedSentences) {
            analyzedSentence.getTokensWithoutWhitespace()[1..-1].each { AnalyzedTokenReadings tokenReadings ->
                if( TagTextCore.isTagEmpty(tokenReadings.getAnalyzedToken(0).getPOSTag())
                        && tokenReadings.getToken() =~ CYR_LETTER ) {
//                            && ! (tokenReadings.getToken() =~ /[ыэъё]|[а-яіїєґА-ЯІЇЄҐ]'?[a-zA-Z]|[a-zA-Z][а-яіїєґА-ЯІЇЄҐ]/) ) {
                    frequencyMap[tokenReadings.getCleanToken()] += 1
                }
            }
        }
    }

    @CompileStatic
    def collectLemmaFrequency(List<AnalyzedSentence> analyzedSentences) { 
        for (AnalyzedSentence analyzedSentence : analyzedSentences) {
            analyzedSentence.getTokensWithoutWhitespace()[1..-1].each { AnalyzedTokenReadings tokenReadings ->
                 Map<String, List<AnalyzedToken>> lemmas = tokenReadings.getReadings()
                    .findAll { it.getLemma() \
                        && tokenReadings.getCleanToken() ==~ ONLY_CYR_LETTER \
                        && ! (it.getPOSTag() =~ /unknown|unclass|symb/ ) //IGNORE_TAGS_FOR_LEMMA) 
                    }
                    .groupBy{ it.getLemma() }
                
                lemmas.each { String k, List<AnalyzedToken> v ->
                    lemmaFrequencyMap[ k ] += 1
                    if( lemmas.size() > 1 ) {
                        lemmaAmbigs << k
                    }
                    def tags = v.findAll { 
                        it.getPOSTag() && ! it.getPOSTag().startsWith("SENT") 
                    }.collect { 
                        it.getPOSTag().replaceFirst(/:.*/, '') 
                    }
                    lemmaFrequencyPostagsMap[k].addAll( tags ) 
                }
            }
        }
    }


    def printHomonymStats() {

        def printStream
        if( options.output == "-" ) {
            printStream = System.out
            printStream.println "\n\n"
        }
        else {
            def outputFile = new File(options.output.replaceFirst(/\.(txt|xml|json)$/, '') + '.homonym.txt')
            printStream = new PrintStream(outputFile, "UTF-8")
        }

        printStream.println("Час-та\tОм.\tЛем\tСлово\tОмоніми")

        homonymFreqMap
            .sort { -it.value }
            .each{ k, v ->
                def items = k.split(/\|/)
                def homonimCount = items.size()
                def posHomonimCount = items.collect { it.split(":", 2)[0] }.unique().size()
    
                def lemmasHaveCaps = items.any { Character.isUpperCase(it.charAt(0)) }
                if( ! lemmasHaveCaps ) {
                    homonymTokenMap[k] = homonymTokenMap[k].collect { it.toLowerCase() } as Set
                }
    
                def str = String.format("%6d\t%d\t%d\t%s\t\t%s", v, homonimCount, posHomonimCount, homonymTokenMap[k].join(","), k)
                printStream.println(str)
            }
    }

    def printUnknownStats() {

        def printStream
        if( options.output == "-" ) {
            printStream = System.out
            printStream.println "\n\nUnknown:\n\n"
        }
        else {
            def outputFile = new File(options.output.replaceFirst(/\.(txt|xml|json)$/, '') + '.unknown.txt')
            printStream = new PrintStream(outputFile, "UTF-8")
        }

        unknownMap
            .sort { it.key }
            .each{ k, v ->
                def str = String.format("%6d\t%s", v, k)
                printStream.println(str)
            }

        int unknownCnt = unknownMap ? unknownMap.values().sum() as int : 0
        double unknownPct = knownCnt+unknownCnt ? (double)unknownCnt*100/(knownCnt+unknownCnt) : 0
        println "Known: $knownCnt, unknown: $unknownCnt, " + String.format("%.1f", unknownPct) + "%"

        double unknownUniquePct = knownMap.size()+unknownMap.size() ? (double)unknownMap.size()*100/(knownMap.size()+unknownMap.size()) : 0
        println "Known unique: ${knownMap.size()}, unknown unique: " + unknownMap.size() + ", " + String.format("%.1f", unknownUniquePct) + "%"

        Map unknownWordsMap = unknownMap.findAll { k,v -> k =~ /(?iu)^[а-яіїєґ][а-яіїєґ'-]*/ }
        double unknownUniqueWordPct = knownMap.size()+unknownWordsMap.size() ? (double)unknownWordsMap.size()*100/(knownMap.size()+unknownWordsMap.size()) : 0
        println "\tunknown unique (letters only): " + unknownWordsMap.size() + ", " + String.format("%.1f", unknownUniqueWordPct) + "%"
        
        printUnclassStats()
    }

    def printUnclassStats() {
        def printStream
        if( options.output == "-" ) {
            printStream = System.out
            printStream.println "\n\nUnclass:\n\n"
        }
        else {
            def outputFile = new File(options.output.replaceFirst(/\.(txt|xml|json)$/, '') + '.unclass.txt')
            printStream = new PrintStream(outputFile, "UTF-8")
        }

        unclassMap
            .sort { it.key }
            .each{ k, v ->
                def str = String.format("%6d\t%s", v, k)
                printStream.println(str)
            }
    }
    
    def printFrequencyStats() {

        def printStream
        if( options.output == "-" ) {
            printStream = System.out
            printStream.println "\n\n"
        }
        else {
            def outputFile = new File(options.output.replaceFirst(/\.(txt|xml|json)$/, '') + '.freq.txt')
            printStream = new PrintStream(outputFile, "UTF-8")
        }

        frequencyMap
        .sort { it.key }
        .each{ k, v ->
            def str = String.format("%6d\t%s", v, k)
            printStream.println(str)
        }
    }

    def printLemmaFrequencyStats() {

        println "Unique lemmas: ${lemmaFrequencyMap.size()}"

        def printStream
        if( options.output == "-" ) {
            printStream = System.out
            printStream.println "\n\n"
        }
        else {
            def outputFile = new File(options.output.replaceFirst(/\.(txt|xml|json)$/, '') + '.lemma.freq.txt')
            printStream = new PrintStream(outputFile, "UTF-8")
        }

        lemmaFrequencyMap
            .sort { it.key }
            .each{ k, v ->
                String amb = k in lemmaAmbigs ? "\tA" : ""
                def str = String.format("%6d\t%s%s\t%s", v, k, amb, lemmaFrequencyPostagsMap[k].join(","))
                printStream.println(str)
            }
    }
}
