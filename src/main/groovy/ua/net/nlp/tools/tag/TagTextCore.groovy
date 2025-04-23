#!/usr/bin/env groovy

package ua.net.nlp.tools.tag

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Function
import java.util.regex.Pattern
import java.util.stream.Collectors

import org.languagetool.AnalyzedSentence
import org.languagetool.AnalyzedToken
import org.languagetool.AnalyzedTokenReadings
import org.languagetool.JLanguageTool
import org.languagetool.Language
import org.languagetool.Languages
import org.languagetool.LtBuildInfo
import org.languagetool.MultiThreadedJLanguageTool
import org.languagetool.language.Ukrainian

import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import picocli.CommandLine
import picocli.CommandLine.ParameterException
import ua.net.nlp.tools.TextUtils
import ua.net.nlp.tools.TextUtils.IOFiles
import ua.net.nlp.tools.TextUtils.OutputFormat
import ua.net.nlp.tools.TextUtils.ResultBase


@CompileStatic
class TagTextCore {

    public static final Pattern PUNCT_PATTERN = Pattern.compile(/[,.:;!?\/()\[\]{}«»„“"'…\u2013\u2014\u201D\u201C•■♦-]+/)               // "
    public static final Pattern SYMBOL_PATTERN = Pattern.compile(/[%&@$*+=<>\u00A0-\u00BF\u2000-\u20CF\u2100-\u218F\u2200-\u22FF]+/)
    static final Pattern UNKNOWN_PATTERN = Pattern.compile(/(.*-)?[а-яіїєґА-ЯІЇЄҐ][а-яіїєґА-ЯІЇЄҐ'\u02BC\u2019]+(-.*)?/)
    static final Pattern NON_UK_PATTERN = Pattern.compile(/^[\#№u2013-]|[\u2013-]$|[ыэъё]|[а-яіїєґ][a-z]|[a-z][а-яіїєґ]/, Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE)
    static final Pattern UNCLASS_PATTERN = Pattern.compile(/\p{IsLatin}[\p{IsLatin}\p{IsDigit}-]*|[0-9]+-?[а-яіїєґА-ЯІЇЄҐ]+|[а-яіїєґА-ЯІЇЄҐ]+-?[0-9]+/)
    static final Pattern NONINFL_PATTERN = Pattern.compile(/[а-зй-яіїєґ]/, Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE) // exclude Russian и
    public static final Pattern XML_TAG_PATTERN = Pattern.compile(/<\/?[a-zA-Z_0-9]+>/)
    private final Pattern CONTROL_CHAR_PATTERN_R = Pattern.compile(/[\u0000-\u0008\u000B-\u0012\u0014-\u001F\u0A0D]/, Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE)
    enum TaggingLevel { tagger, stats }
    
    Ukrainian language = (Ukrainian) Languages.getLanguageForShortCode("uk");
    
    JLanguageTool langTool = new MultiThreadedJLanguageTool(language)

    TagOptions options
    UdModule udModule = new UdModule()
    VerticalModule verticalModule = new VerticalModule()
    
	@Canonical
	public static class TagResult extends ResultBase {
		TagStats stats
        
        TagResult(String str, TagStats stats) {
            super(str);
            this.stats = stats
        }
    }

    TagStats stats = new TagStats()
    DisambigStats disambigStats = new DisambigStats()
    SemTags semTags = new SemTags()
    ModZheleh modZheleh = new ModZheleh(langTool)
    ModLesya modLesya = new ModLesya(langTool)
    TagUnknown tagUnknown = new TagUnknown()


    @CompileStatic
    TagResult tagText(String text) {

        def stats = new TagStats()
        stats.options = options

        List<TaggedSentence> taggedSentences = tagTextCore(text, stats)

        def outputFormats = new OutputFormatter(options)
        outputFormats.init()

        def sb = new StringBuilder()
        
        int sentId = 1
        for(TaggedSentence taggedSent: taggedSentences) {
            if ( ! options.noTag ) {
                if( options.outputFormat == OutputFormat.xml ) {
                    CharSequence x = outputFormats.outputSentenceXml(taggedSent.tokens)
                    sb.append(x).append("\n")
                }
                else if( options.outputFormat == OutputFormat.json ) {
                    if( taggedSent.tokens ) {
                        def s = outputFormats.outputSentenceJson(taggedSent.tokens)
                        if( sb.length() > 0 ) sb.append(",\n");
                        sb.append(s)
                    }
                }
                else if( options.outputFormat == OutputFormat.vertical ) {
                    verticalModule.printSentence(taggedSent, sb, sentId)
                }
                else if( options.outputFormat == OutputFormat.conllu ) {
                    udModule.printSentence(taggedSent, sb, sentId)
                }
                else { // legacy text
                    if( sb.length() > 0 ) sb.append("\n")

                    if( taggedSent.tokens ) {
                        def prevToken = null
                        taggedSent.tokens.each { TTR token ->
                            if( prevToken != null && (token.tokens[0].whitespaceBefore == null || token.tokens[0].whitespaceBefore == true) ) {
                                sb.append(" ")
                            }
                            if( options.lemmaOnly ) {
                                sb.append(token.tokens[0].lemma)
                            }
                            else {
                                def lemmasAndTags = token.tokens.collect{ t ->
                                    options.lemmaOnly ? t.lemma : "${t.lemma}/${t.tags}"
                                }.join(",")
                                sb.append("${token.tokens[0].value}[$lemmasAndTags]")
                            }
                            prevToken = token
                        }
                    }
                }
            }
            sentId++
        }
        
        return new TagResult(sb.toString(), stats)
    }

    static class TaggedSentence {
        List<TTR> tokens
        String text
    }
    
    
    @CompileStatic
    void tagTextStream(InputStream input, OutputStream output) {

        def stats = new TagStats()
        stats.options = options

        def outputFormats = new OutputFormatter(options)
        outputFormats.init()
        
        def printStream = new PrintStream(output, true, "UTF-8")

        TextUtils.processFile(input, printStream, options, { String buffer ->
            
            List<TaggedSentence> taggedSentences = tagTextCore(buffer, stats)

            def sb = new StringBuilder()

            for(TaggedSentence taggedSent: taggedSentences) {
                if ( ! options.noTag ) {
                    if( options.outputFormat == OutputFormat.xml ) {
                        CharSequence x = outputFormats.outputSentenceXml(taggedSent.tokens)
                        sb.append(x).append("\n")
                    }
                    else if( options.outputFormat == OutputFormat.json ) {
                        def s = outputFormats.outputSentenceJson(taggedSent.tokens)
                        if( sb.length() > 0 ) sb.append(",\n");
                        sb.append(s)
                    }
                }
            }

            output.flush()
            return new TagResult(sb.toString(), stats)
            
        } as Function<String, TagResult>, 
        { } as Consumer<TagResult>)
    }

    @CompileStatic
    List<TaggedSentence> tagTextCore(String text, TagStats stats) {
        // remove control chars so we don't create broken xml
        def m = text =~ CONTROL_CHAR_PATTERN_R
        if( m ) {
            m.replaceAll('')
        }
        
        List<AnalyzedSentence> analyzedSentences = analyzeText(text)
        
        tagTextCore(analyzedSentences, stats)
    }
    
    @CompileDynamic
    List<AnalyzedSentence> analyzeText(String text) {
        options.sentencePerLine
            ? langTool.analyzeSentences( text.split("\n") as List )
            : langTool.analyzeText(text)
    }

    public List<TaggedSentence> tagTextCore(List<AnalyzedSentence> analyzedSentences) {
        tagTextCore(analyzedSentences, null);
    }
        
    List<TaggedSentence> tagTextCore(List<AnalyzedSentence> analyzedSentences, TagStats stats) {
        List<TaggedSentence> taggedSentences = 
          analyzedSentences.parallelStream().map { AnalyzedSentence analyzedSentence ->
            
            cleanup(analyzedSentence)
            
            AnalyzedTokenReadings[] tokens = analyzedSentence.getTokensWithoutWhitespace()

            List<TTR> taggedObjects = tagAsObject(tokens, stats)

            List<TaggedSentence> ret = [new TaggedSentence(tokens: taggedObjects, text: analyzedSentence.text)]
            
            if( options.tokenFormat ) {
                if( tokens[-1].hasPosTag(JLanguageTool.PARAGRAPH_END_TAGNAME) ) {
                    ret << new TaggedSentence()
                }
            }
            
            if( options.showDisambigRules ) {
                analyzedSentence.tokens.each { AnalyzedTokenReadings it ->
                    def historicalAnnotations = it.getHistoricalAnnotations()
                    if( historicalAnnotations && ! historicalAnnotations.contains("add_paragaph_end") ) {
                        println "* " + historicalAnnotations.trim()
                    }
                }
            }
            
            ret
        }
        .flatMap{ l -> l.stream() }
        .collect(Collectors.toList())
        
        if( options.homonymStats ) {
            stats.collectHomonyms(analyzedSentences)
        }
        if( options.unknownStats ) {
            stats.collectUnknown(taggedSentences)
        }
        if( options.frequencyStats ) {
            stats.collectFrequency(analyzedSentences)
        }
        if( options.lemmaStats ) {
            stats.collectLemmaFrequency(analyzedSentences)
        }

        taggedSentences
    }


    void cleanup(AnalyzedSentence analyzedSentence) {
        // multiwords are very LT-specific
        analyzedSentence.getTokens().each { AnalyzedTokenReadings t ->
            def multiWordReadings = t.getReadings().findAll { AnalyzedToken r ->
                r.getPOSTag() != null && r.getPOSTag().startsWith("<")
            }
            multiWordReadings.each { r ->
                t.removeReading(r, "remove_multiword")
            }
        }
    }
        
    private static boolean hasPosTag(AnalyzedTokenReadings tokenReadings) {
        tokenReadings.getReadings().stream()
            .anyMatch{ t -> ! isTagEmpty(t.getPOSTag()) }
    }   

    @Canonical
    static class TaggedToken {
        String value
        String lemma
        String tags
        String semtags
        Boolean whitespaceBefore
        List<TaggedToken> alts
        // technical attributes
        TaggingLevel level
        BigDecimal confidence
        
        Boolean isWhitepaceBefore() {
            tags != 'punct' ? null : whitespaceBefore
        }
        
        @Override
        public String toString() {
            "[$value / $lemma / $tags]";
        }
    }
    
    @CompileStatic
    @Canonical
    static class TTR {
        List<TaggedToken> tokens
    }
    
    @CompileStatic
    static class TokenInfo {
        String cleanToken
        String cleanToken2
        AnalyzedTokenReadings[] tokens
        int idx
        List<TTR> taggedTokens
    }

    @CompileStatic
    private List<TTR> tagAsObject(AnalyzedTokenReadings[] tokens, TagStats stats) {
        if( tokens.size() < 2 )
            return []
        
        tokens = tokens[1..-1] // remove SENT_START
        List<TTR> tokenReadingsT = []

//        tokens.eachWithIndex { AnalyzedTokenReadings tokenReadings, int idx ->
        for(int idx=0; idx<tokens.length; idx++) {
            AnalyzedTokenReadings tokenReadings = tokens[idx]
            
            String theToken = tokenReadings.getToken()
            String cleanToken = tokenReadings.getCleanToken()
            
            boolean hasTag = hasPosTag(tokenReadings) 

            if( isZheleh(options) 
                    && idx < tokens.size() -1 
                    && tokens[idx+1].getCleanToken().equals("ся") ) {
                tokenReadings = modZheleh.adjustTokens(tokenReadings, tokens, idx)
                hasTag = hasPosTag(tokenReadings)
            }
            
            TokenInfo ti = new TokenInfo(cleanToken: cleanToken, tokens: tokens, idx: idx, taggedTokens: tokenReadingsT, cleanToken2: cleanToken)
            
//            if( hasTag ) {
//                // TODO: tmp workaround, remove after LT 6.1
//                if( theToken.startsWith("+") && theToken =~ /^\+[0-9]/ ) {
//                    tokenReadingsT << new TTR(tokens: [new TaggedToken('value': '+', lemma: '+', tags: 'symb')])
//                    String tags = theToken ==~ /\+[0-9]+([-,.][0-9]+)?/ ? 'number' : 'unclass'
//                    tokenReadingsT << new TTR(tokens: [new TaggedToken('value': theToken[1..-1], lemma: theToken[1..-1], tags: tags)])
//                    return tokenReadingsT
//                }
//            }
            
            // TODO: temp workaround for disambiguator problem in PAIR_OF_PROPER_NOUNS in LT
            if( "".equals(tokenReadings.getAnalyzedToken(0).getPOSTag()) ) { //&& theToken.matches("[–—-]") ) {
                hasTag = false;
            }
            
            if( ! hasTag 
                    // TODO: ugly workaround for disambiguator problem
                    || "\u2014".equals(theToken) ) {
                if( tokenReadings.isLinebreak() )
                    continue // return tokenReadingsT
    
                if( PUNCT_PATTERN.matcher(theToken).matches() ) {
                    def tkn = new TaggedToken(value: theToken, lemma: cleanToken, tags: 'punct', 'whitespaceBefore': tokenReadings.isWhitespaceBefore())
                    tokenReadingsT << new TTR(tokens: [tkn])
                    continue // return tokenReadingsT
                }
                else if( SYMBOL_PATTERN.matcher(theToken).matches() ) {
                    tokenReadingsT << new TTR(tokens: [new TaggedToken('value': theToken, lemma: cleanToken, tags: 'symb')])
                    continue // return tokenReadingsT
                }
                else if( XML_TAG_PATTERN.matcher(theToken).matches() ) {
                    tokenReadingsT << new TTR(tokens: [new TaggedToken(value: theToken, lemma: cleanToken, tags: 'xmltag')])
                    continue // return tokenReadingsT
                }
                else if( UNKNOWN_PATTERN.matcher(theToken).matches() && ! NON_UK_PATTERN.matcher(theToken).find() ) {
                    if( isZheleh(options) ) {
                        tokenReadings = modZheleh.adjustTokens(tokenReadings, tokens, idx)
                        hasTag = hasPosTag(tokenReadings)
                    }
                    else if( isLesya(options) ) {
                        tokenReadings = modLesya.adjustTokens(tokenReadings, tokens, idx)
                        hasTag = hasPosTag(tokenReadings)
                    }

                    if( ! hasTag ) {
                        
                        if( options.tagUnknown ) {
                            List<TaggedToken> taggedTokens = tagUnknown.tag(theToken, idx, tokens)
                            if( taggedTokens ) {
                                if( options.showTaggingLevel ) {
                                    taggedTokens.each{ it.level = TaggingLevel.stats }
                                }
                                if( ! options.unknownRate ) {
                                    taggedTokens.each{ it.confidence = null }
                                }
                                if( options.disambiguate ) {
                                    if( options.disambiguate && taggedTokens.size() > 1 ) {
                                        // use left context to disambiguate unknown - after "prep" only for now
                                        if( idx > 0 && tokens[idx-1].getReadings().find{ it.getPOSTag() != null && it.getPOSTag().startsWith("prep") } ) {
                                            Map<AnalyzedToken, TaggedToken> readingsMap = taggedTokens.collectEntries{
                                                def at = new AnalyzedToken(it.value, it.tags, it.lemma)
                                                [(at): it]
                                            }
                                            List<AnalyzedToken> readings = readingsMap.keySet() as List
                                            disambigStats.orderByStats(readings, ti, stats)
                                            taggedTokens = readings.collect { readingsMap[it] }
                                        }
                                    }
                                }
                                if( options.singleTokenOnly ) {
                                    taggedTokens = taggedTokens.take(1)
                                }

                                tokenReadingsT << new TTR(tokens: taggedTokens)
                                continue // return tokenReadingsT
                            }
                        }
                        
                        def lemma = options.setLemmaForUnknown ? cleanToken : ''
                        tokenReadingsT << new TTR(tokens: [new TaggedToken('value': theToken, lemma: lemma, tags: 'unknown')])
                        continue // return tokenReadingsT
                    }
                }
                else if( NONINFL_PATTERN.matcher(theToken).matches() ) {
                    tokenReadingsT << new TTR(tokens: [new TaggedToken('value': theToken, lemma: cleanToken, tags: 'noninfl')])
                    continue
                }
                else {
                    tokenReadingsT << new TTR(tokens: [new TaggedToken('value': theToken, lemma: cleanToken, tags: 'unclass')])
                    continue // return tokenReadingsT
                }
            }
            
            TTR item = new TTR(tokens: [])
            
            List<String> splitPart = options.splitHyphenParts ? TextUtils.splitWithPart(cleanToken) : null
            if( splitPart ) {
                ti.cleanToken2 = splitPart[0]
            }
            
            List<AnalyzedToken> readings = new ArrayList<>(tokenReadings.getReadings())
            readings.removeIf{ AnalyzedToken tkn ->
                String posTag = tkn.getPOSTag()
                posTag == null || posTag.endsWith("_END") || posTag.startsWith("<")
            }
            
            List<Double> rates = null
            if( options.disambiguate && readings.size() > 1 ) {
                rates = disambigStats.orderByStats(readings, ti, stats)
            }

            if( options.tokenFormat ) {
                List<TaggedToken> tagTokens = readings.collect { AnalyzedToken tkn ->
                    getTagTokens(tkn, splitPart)
                }
                Object firstToken = tagTokens[0]
                if( tagTokens.size() > 1 ) {
                    if( ! options.singleTokenOnly ) {
                        firstToken['alts'] = tagTokens[1..-1]
                    }
                    if( rates ) {
                        addRates(tagTokens, rates)
                    }
                }
                item.tokens = [ firstToken ]
            }
            else {
                item.tokens = readings.collect { AnalyzedToken tkn ->   
                    getTagTokens(tkn, splitPart)
                }
            }

            tokenReadingsT << item
            
            if( options.separateDotAbbreviation ) {
                if( cleanToken.endsWith(".") && item.tokens.find { it.tags.contains(":abbr") } ) {
                    item.tokens.each { 
                        it.value = it.value.replaceFirst(/\.$/, '')
                        it.lemma = it.lemma.replaceFirst(/\.$/, '')
                    }
                    def dotToken = new TaggedToken('value': '.', 'lemma': '.', 'tags': 'punct')
                    tokenReadingsT << new TTR(tokens: [dotToken])
                }
//                else if( cleanToken ==~ /[0-9]+.[0-9]+/ && item.tokens.find { it.tags.contains("unclass") } ) {
//                    item.tokens.each { 
//                        it.value = it.value.replace('.', '')
//                        it.lemma = it.lemma.replace('.', '')
//                    }
//                    def dotToken = new TaggedToken('value': '.', 'lemma': '.', 'tags': 'punct')
//                    tokenReadingsT << new TTR(tokens: [dotToken])
//                }
            }
            
            if( splitPart ) {
                def partToken = new TaggedToken('value': splitPart[1], 'lemma': splitPart[1][1..-1].toLowerCase(), 'tags': 'part')
                tokenReadingsT << new TTR(tokens: [partToken])
            }

        }
        
        disambigStats.debugStatsFlush()
            
        return tokenReadingsT
    }

    @CompileStatic
    static void addRates(List<TaggedToken> tagTokens, List<Double> rates) {
        BigDecimal sum = (BigDecimal) rates.sum()
        tagTokens.eachWithIndex { TaggedToken t, int idx2 ->
//            if( ! sum ) System.err.println "sum of 0 for: $tagTokens"
            BigDecimal q = sum ? rates[idx2] / sum : BigDecimal.ZERO
            t['confidence'] = q.round(3)
        }
    }
    
    
    @CompileStatic
    private TaggedToken getTagTokens(AnalyzedToken tkn, List<String> splitPart) {
        String posTag = tkn.getPOSTag()
        String semTags = semTags.getSemTags(tkn, posTag)

//        posTag = posTag.replaceFirst(/:r(in)?anim/, '')
        
        String lemma = tkn.getLemma() ?: ''
        if( options.setLemmaForUnknown && ! lemma ) {
            lemma = tkn.getToken().replace('\u0301', '') // TODO: cleanToken
        }
        if( lemma && lemma.indexOf('-') >= 0 && splitPart ) {
            lemma = TextUtils.WITH_PARTS.matcher(lemma).replaceFirst('$1')
        }
        String value = splitPart ? splitPart[0] : tkn.getToken()
        
        TaggedToken token = semTags \
                        ? new TaggedToken('value': value, 'lemma': lemma, 'tags': posTag, 'semtags': semTags)
                : new TaggedToken('value': value, 'lemma': lemma, 'tags': posTag)
    }


    private static boolean isZheleh(TagOptions options) {
        return options.modules && 'zheleh' in options.modules
    }
    private static boolean isLesya(TagOptions options) {
        return options.modules && 'lesya' in options.modules
    }


    @CompileDynamic
    def process() {
//        def stats = new TagStats()
//        stats.options = options
        
        def outputFile = TextUtils.processByParagraph(options, { buffer ->
            return tagText(buffer)
        },
        { TagResult result ->
            // when processing stdin add stats to the global ones
            this.stats.add(result.stats)
        });
    }

    @CompileDynamic
    def process(IOFiles fileInfo) {
        def stats = new TagStats()
        stats.options = options

        def outputFile = TextUtils.processByParagraphInternal(options, fileInfo.inputFile, fileInfo.outputFile, { buffer ->
            return tagText(buffer)
        },
        { TagResult result ->
            // when processing by file add stats to the file stats first
            stats.add(result.stats)
        });

        // ... then add to global ones
        this.stats.add(stats)
        addUnknownPct(stats, fileInfo)
    }

    @CompileDynamic
    def addUnknownPct(TagStats stats, IOFiles fileInfo) {
//    println "== ${fileInfo.filename}, ${stats.knownCnt}, ${stats.unknownMap}"
      if( fileInfo.filename
            && ( stats.knownCnt || stats.unknownMap || stats.unclassMap ) ) {
          def notKnownCnt = stats.unknownMap.values().sum(0) + stats.unclassMap.values().sum(0)
          this.stats.unknownPctMap[fileInfo.filename] = [(notKnownCnt)*1000 / (stats.knownCnt + notKnownCnt), stats.knownCnt]
      }
      if( options.progress ) {
        def sz = this.stats.unknownPctMap.size()
        if( sz % 100 == 0 ) {
          System.err.println "Processed ${sz} files"
        }
      }
    }

    def postProcess() {

        if( options.homonymStats ) {
            stats.printHomonymStats()
        }
        if( options.unknownStats ) {
            stats.printUnknownStats()
        }
        if( options.frequencyStats ) {
            stats.printFrequencyStats()
        }
        if( options.lemmaStats ) {
            stats.printLemmaFrequencyStats()
        }
        
        if( options.disambiguate ) {
            disambigStats.printStats(stats.disambigMap)
        }
    }

    static boolean isTagEmpty(String posTag) {
        posTag == null || posTag.endsWith("_END")
    }
    
    static TagOptions parseOptions(String[] argv) {
        TagOptions options = new TagOptions()
        CommandLine commandLine = new CommandLine(options)
        try {
            commandLine.parseArgs(argv)
            if (options.helpRequested) {
                commandLine.usage(System.out)
                System.exit 0
            } 
        } catch (ParameterException ex) {
            println ex.message
            commandLine.usage(System.out)
            System.exit 1
        }

        return options
    }

    
    void setOptions(TagOptions options) {
        options.adjust()

        setInputOutput(options)

        this.options = options
        stats.options = options
//        modZheleh.options = options

        if( ! options.quiet ) {
            if( isZheleh(options) ) {
                System.err.println ("Using adjustments for Zhelekhivka")
                if( options.frequencyStats || options.unknownStats || options.lemmaStats ) {
                    System.err.println ("NOTE: Zhelekhivka adjustments currently do not apply to statistics!")
                }
            }
        }

        semTags.options = options
        if( options.semanticTags ) {
            if( options.outputFormat == OutputFormat.txt ) {
                System.err.println ("Semantic tagging only available in xml/json output")
                System.exit 1
            }
            semTags.loadSemTags()
        }

        disambigStats.setOptions(options)
        if( options.disambiguate ) {
            disambigStats.loadDisambigStats()
        }
        if( options.tagUnknown ) {
            tagUnknown.loadStats()
        }
        
        if( options.tokenFormat ) {
            options.xmlSchema = "https://github.com/brown-uk/nlp_uk/raw/master/src/main/resources/schema.xsd"
        }
//        language.getSentenceTokenizer().setSingleLineBreaksMarksParagraph(options.singleNewLineAsParagraph)

        if( options.outputFormat == OutputFormat.conllu ) {
            udModule.options = options
            udModule.language = language
            udModule.init()
        }
        else if( options.outputFormat == OutputFormat.vertical ) {
            verticalModule.options = options
        }
    }

    void setInputOutput(TagOptions options) {
        
        if( ! options.input ) {
            options.input = "-"
        }
        if( ! options.output ) {
            def fileExt = "." + options.outputFormat.getExtension() // ? ".xml" : ".txt"
            def adj = options.lemmaOnly ? 'lemmatized' : 'tagged'
            def outfile = options.input == '-' ? '-' : options.input.replaceFirst(/(\.[a-z]+)?$/, ".$adj${fileExt}")
            options.output = outfile
        }

    }
    
	
    static void main(String[] args) {

        def nlpUk = new TagTextCore()
        
        def options = parseOptions(args)

        try {
            nlpUk.setOptions(options)
        }
        catch(IllegalStateException e) {
            System.err.println(e.getMessage())
            System.exit(1)
        }

        if( ! options.quiet ) {
            printLtVersion()
        }

        // TODO: quick hack to support recursive processing
        if( options.recursive ) {
            def dirs = options.inputFiles ?: ["."]
            
            List<File> files = []
            dirs.collect { d -> 
                new File(d).eachFileRecurse { f -> 
                      if( f.name.toLowerCase().endsWith(".txt") ) {
                          files << f
                      }
                }
            }
            
            println "Found ${files.size()} files with .txt extension" // + files
            options.singleThread = true
            processFilesParallel(nlpUk, options, files.collect { it.path })
        }
        // TODO: quick hack to support multiple files
        else if( options.inputFiles && options.inputFiles != ["-"] ) {
            options.singleThread = true
            processFilesParallel(nlpUk, options, options.inputFiles)
        }
        else if( options.listFile ) {
            options.singleThread = true
            def files = new File(options.listFile).readLines('UTF-8')
            processFilesParallel(nlpUk, options, files)
        }
        else {
            nlpUk.process()
            nlpUk.postProcess()
        }
    }

    def static printLtVersion() {
        println("LT version: ${LtBuildInfo.OS.getVersion()}")
        def dictUkVersionRes = Ukrainian.class.getClassLoader().getResourceAsStream('org/languagetool/resource/uk/VERSION')
        def dictUkversion = dictUkVersionRes ? dictUkVersionRes.text : "<unknown>"
        println("dict_uk version: ${dictUkversion}")
        if( Runtime.version().feature() >= 19 
                && LtBuildInfo.OS.getVersion() == "6.5" ) {
            System.err.println("WARNING: With Java >= 19 you need to use LanguageTool 6.6-SNAPSHOT or later (otherwise tokenization may produce wrong results)")
        }
    }

    static processFilesParallel(TagTextCore nlpUk, TagOptions options, List<String> inputFiles) {
        ExecutorService executors = Executors.newWorkStealingPool()
        inputFiles.forEach{ filename ->
            options.output = ""
            options.input = filename
            nlpUk.setInputOutput(options)
            IOFiles files = TextUtils.prepareInputOutput(options)
            
            executors.submit({
                nlpUk.process(files)
            } as Runnable)
        }
        
        executors.shutdown()
        executors.awaitTermination(1, TimeUnit.DAYS)
        nlpUk.postProcess()
    }
    
}
