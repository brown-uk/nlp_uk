package ua.net.nlp.tools.tag

import java.util.regex.Pattern

import org.languagetool.AnalyzedSentence
import org.languagetool.AnalyzedToken
import org.languagetool.AnalyzedTokenReadings
import org.languagetool.JLanguageTool
import ua.net.nlp.tools.tag.TagTextCore
import ua.net.nlp.tools.tag.TagOptions

import groovy.transform.CompileStatic


public class TagStats {
    static final Pattern CYR_LETTER = Pattern.compile(/[а-яіїєґА-ЯІЇЄҐ]/)
    static final Pattern NON_UK_LETTER = Pattern.compile(/[ыэъёЫЭЪЁ]|ие|ИЕ|ннн|оі$|[а-яіїєґА-ЯІЇЄҐ]'?[a-zA-Z]|[a-zA-Z][а-яіїєґА-ЯІЇЄҐ]/)

    TagOptions options
    
    Map<String, Integer> homonymFreqMap = [:].withDefault { 0 }
    Map<String, Set<String>> homonymTokenMap = [:].withDefault{ new LinkedHashSet<>() }
    Map<String, Integer> unknownMap = [:].withDefault { 0 }
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
        stats.knownMap.each { k,v -> knownMap[k] += v }
        stats.frequencyMap.each { k,v -> frequencyMap[k] += v }
        stats.lemmaFrequencyMap.each { k,v -> lemmaFrequencyMap[k] += v }
        stats.lemmaFrequencyPostagsMap.each { k,v -> lemmaFrequencyPostagsMap[k] += v }
        lemmaAmbigs.addAll(stats.lemmaAmbigs)
        knownCnt += stats.knownCnt
        
        stats.disambigMap.each { k,v -> disambigMap[k] += v }
    }

    @CompileStatic
    def collectUnknown(List<AnalyzedSentence> analyzedSentences) {
        for (AnalyzedSentence analyzedSentence : analyzedSentences) {
            // if any words contain Russian sequence filter out the whole sentence - this removes tons of Russian words from our unknown list
            // we could also test each word against Russian dictionary but that would filter out some valid Ukrainian words too
            
            def tokensNoSpace = analyzedSentence.getTokensWithoutWhitespace()[1..-1]
            if( options.filterUnknown ) {
                def unknownBad = tokensNoSpace.any { AnalyzedTokenReadings tokenReadings ->
                    tokenReadings.getCleanToken().indexOf('еи') > 0
                }
                if( unknownBad )
                    continue
            }

            tokensNoSpace.each { AnalyzedTokenReadings tokenReadings ->
                    String posTag = tokenReadings.getAnalyzedToken(0).getPOSTag()
                    if( TagTextCore.isTagEmpty(posTag) ) {
                        String token = tokenReadings.getCleanToken()
                        if( CYR_LETTER.matcher(token).find()
                            && ! NON_UK_LETTER.matcher(token).find() ) {
                        unknownMap[token] += 1
                    }
                }
            }

            tokensNoSpace.each { AnalyzedTokenReadings tokenReadings ->
                if( ! (tokenReadings.getToken() =~ /[0-9]|^[a-zA-Z-]+$/) 
                        && tokenReadings.getReadings().any { AnalyzedToken at -> ! TagTextCore.isTagEmpty(at.getPOSTag())} ) {
                  knownCnt++
                  knownMap[tokenReadings.getToken()] += 1
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
                        && tokenReadings.getToken() =~ /[а-яіїєґА-ЯІЇЄҐ]/ ) {
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
                        && tokenReadings.getCleanToken() ==~ /[а-яіїєґА-ЯІЇЄҐ'-]+/ \
                        && ! (it.getPOSTag() =~ /arch|alt|short|long|bad|</) 
                    }
                    .groupBy{ it.getLemma() }
                
                lemmas.each { String k, List<AnalyzedToken> v ->
                    lemmaFrequencyMap[ k ] += 1
                    if( lemmas.size() > 1 ) {
                        lemmaAmbigs << k
                    }
                    def tags = v.findAll{ it.getPOSTag() && ! it.getPOSTag().startsWith("SENT") }.collect { it.getPOSTag().replaceFirst(/:.*/, '') }
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
            printStream.println "\n\n"
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
