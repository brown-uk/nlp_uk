#!/usr/bin/env groovy

package ua.net.nlp.tools.tag

@GrabConfig(systemClassLoader=true)
//@Grab(group='org.languagetool', module='language-uk', version='5.10-SNAPSHOT')
@Grab(group='org.languagetool', module='language-uk', version='5.9')
@Grab(group='ch.qos.logback', module='logback-classic', version='1.4.+')
@Grab(group='info.picocli', module='picocli', version='4.6.+')

import java.math.RoundingMode
import java.util.regex.Pattern
import java.util.stream.Collectors
import org.apache.commons.lang3.mutable.MutableInt

import org.languagetool.AnalyzedSentence
import org.languagetool.AnalyzedToken
import org.languagetool.AnalyzedTokenReadings
import org.languagetool.JLanguageTool
import org.languagetool.MultiThreadedJLanguageTool
import org.languagetool.language.Ukrainian

import ua.net.nlp.bruk.ContextToken
import ua.net.nlp.bruk.WordContext
import ua.net.nlp.bruk.WordReading
import ua.net.nlp.tools.TextUtils
import ua.net.nlp.tools.tag.DisambigStats
import ua.net.nlp.tools.tag.ModZheleh
import ua.net.nlp.tools.tag.OutputFormats
import ua.net.nlp.tools.tag.SemTags
import ua.net.nlp.tools.tag.TagStats
import ua.net.nlp.tools.tag.TagOptions
import ua.net.nlp.tools.tag.TagOptions.OutputFormat

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.util.Eval

import picocli.CommandLine
import picocli.CommandLine.Option
import picocli.CommandLine.ParameterException
import picocli.CommandLine.Parameters


class TagTextCore {
    
    def textUtils = new TextUtils()
    
    static final Pattern PUNCT_PATTERN = Pattern.compile(/[,.:;!?\/()\[\]{}«»„“"'…\u2013\u2014\u201D\u201C•■♦-]+/)
    static final Pattern SYMBOL_PATTERN = Pattern.compile(/[%&@$*+=<>\u00A0-\u00BF\u2000-\u20CF\u2100-\u218F\u2200-\u22FF]+/)
    static final Pattern UNKNOWN_PATTERN = Pattern.compile(/(.*-)?[а-яіїєґА-ЯІЇЄҐ][а-яіїєґА-ЯІЇЄҐ'\u02BC\u2019]+(-.*)?/)
    static final Pattern NON_UK_PATTERN = Pattern.compile(/^[\#№u2013-]|[\u2013-]$|[ыэъё]|[а-яіїєґ][a-z]|[a-z][а-яіїєґ]/, Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE)
    static final Pattern UNCLASS_PATTERN = Pattern.compile(/\p{IsLatin}[\p{IsLatin}\p{IsDigit}-]*|[0-9]+-?[а-яіїєґА-ЯІЇЄҐ]+|[а-яіїєґА-ЯІЇЄҐ]+-?[0-9]+/)
    static final Pattern XML_TAG_PATTERN = Pattern.compile(/<\/?[a-zA-Z_0-9]+>/)
    
    def language = new Ukrainian() {
        @Override
        protected synchronized List<?> getPatternRules() { return [] }
    }

    JLanguageTool langTool = new MultiThreadedJLanguageTool(language)

    TagOptions options
    
	@Canonical
	static class TagResult {
		String tagged
		TagStats stats
	}
	
	TagStats stats = new TagStats()
    DisambigStats disambigStats = new DisambigStats()
    SemTags semTags = new SemTags()
    ModZheleh modZheleh = new ModZheleh(langTool)
    ModLesya modLesya = new ModLesya(langTool)
    


    @CompileStatic
    TagResult tagText(String text) {

        def stats = new TagStats()
        stats.options = options

        List<List<TTR>> taggedSentences = tagTextCore(text, stats)

        def outputFormats = new OutputFormats(options)
        outputFormats.init()

        def sb = new StringBuilder()
        
        for(List<TTR> taggedSent: taggedSentences) {
            if ( ! options.noTag ) {
                if( options.outputFormat == OutputFormat.xml ) {
                    CharSequence x = outputFormats.outputSentenceXml(taggedSent)
                    sb.append(x).append("\n")
                }
                else if( options.outputFormat == OutputFormat.json ) {
                    def s = outputFormats.outputSentenceJson(taggedSent)
                    if( sb.length() > 0 ) sb.append(",\n");
                    sb.append(s)

                    //                    sb.append(",\n{}")
                }
                else { // legacy text
                    if( sb.length() > 0 ) sb.append("\n")
                    def prevToken = null
                    taggedSent.each { TTR token ->
                        if( prevToken != null && (token.tokens[0].whitespaceBefore == null || token.tokens[0].whitespaceBefore == true) ) {
                            sb.append(" ")
                        }
                        def lemmasAndTags = token.tokens.collect{ t -> "${t.lemma}/${t.tags}" }.join(",")
                        sb.append("${token.tokens[0].value}[$lemmasAndTags]")
                        prevToken = token
                    }
                }
            }
        }
        
        return new TagResult(sb.toString(), stats)
    }

    @CompileStatic
    List<List<TTR>> tagTextCore(String text, TagStats stats) {
        if( stats == null ) {
            stats = new TagStats()
            stats.options = options
        }
        
        List<AnalyzedSentence> analyzedSentences = langTool.analyzeText(text);

        List<List<TTR>> taggedSentences = []
        for (AnalyzedSentence analyzedSentence : analyzedSentences) {
            
            cleanup(analyzedSentence)
            
            AnalyzedTokenReadings[] tokens = analyzedSentence.getTokensWithoutWhitespace()

            List<TTR> taggedObjects = tagAsObject(tokens, stats)

            taggedSentences << taggedObjects
            
            if( options.tokenFormat ) {
                if( tokens[-1].hasPosTag(JLanguageTool.PARAGRAPH_END_TAGNAME) ) {
                    taggedSentences << []
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
        }
        
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

    @CompileStatic
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
        
    @CompileStatic
    private static boolean hasPosTag(AnalyzedTokenReadings tokenReadings) {
        tokenReadings.getReadings().stream()
            .anyMatch{ t -> ! isTagEmpty(t.getPOSTag()) }
    }   

    @CompileStatic
    @Canonical
    static class TaggedToken {
        String value
        String lemma
        String tags
        String semtags
        Boolean whitespaceBefore
        BigDecimal q
        List<TaggedToken> alts
    }
    
    @CompileStatic
    @Canonical
    static class TTR {
        List<TaggedToken> tokens
    }

    @CompileStatic
    private List<TTR> tagAsObject(AnalyzedTokenReadings[] tokens, TagStats stats) {
        tokens = tokens[1..-1] // remove SENT_START
        List<TTR> tokenReadingsT = []

        tokens.eachWithIndex { AnalyzedTokenReadings tokenReadings, int idx ->
            String theToken = tokenReadings.getToken()
            String cleanToken = tokenReadings.getCleanToken()
            
            boolean hasTag = hasPosTag(tokenReadings) 

            if( isZheleh(options) 
                    && idx < tokens.size() -1 
                    && tokens[idx+1].getCleanToken().equals("ся") ) {
                tokenReadings = modZheleh.adjustTokens(tokenReadings, tokens, idx)
                hasTag = hasPosTag(tokenReadings)
            }
            
            // TODO: ugly workaround for disambiguator problem
            if( ! hasTag || "\u2014".equals(theToken) ) {
                if( tokenReadings.isLinebreak() )
                    return tokenReadingsT
    
                if( PUNCT_PATTERN.matcher(theToken).matches() ) {
                    def tkn = options.tokenFormat 
                        ? new TaggedToken(value: theToken, lemma: cleanToken, tags: 'punct')
                        : new TaggedToken(value: theToken, lemma: cleanToken, tags: 'punct', 'whitespaceBefore': tokenReadings.isWhitespaceBefore())
                    tokenReadingsT << new TTR(tokens: [tkn])
                    return tokenReadingsT
                }
                else if( SYMBOL_PATTERN.matcher(theToken).matches() ) {
                    tokenReadingsT << new TTR(tokens: [new TaggedToken('value': theToken, lemma: cleanToken, tags: 'symb')])
                    return tokenReadingsT
                }
                else if( XML_TAG_PATTERN.matcher(theToken).matches() ) {
                    tokenReadingsT << new TTR(tokens: [new TaggedToken(value: theToken, lemma: cleanToken, tags: 'xmltag')])
                    return tokenReadingsT
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
                        def lemma = options.setLemmaForUnknown ? cleanToken : ''
                        tokenReadingsT << new TTR(tokens: [new TaggedToken('value': theToken, lemma: lemma, tags: 'unknown')])
                        return tokenReadingsT
                    }
                }
                else { // if( UNCLASS_PATTERN.matcher(theToken).matches() ) {
                    tokenReadingsT << new TTR(tokens: [new TaggedToken('value': theToken, lemma: cleanToken, tags: 'unclass')])
                    return tokenReadingsT
                }
            }
            
            TTR item = new TTR(tokens: [])
            
            List<String> splitPart = options.isPartsSeparate() ? TextUtils.splitWithPart(cleanToken) : null
            if( splitPart ) {
                cleanToken = splitPart[0]
            }
            
            List<AnalyzedToken> readings = new ArrayList<>(tokenReadings.getReadings())
            readings.removeIf{ AnalyzedToken tkn ->
                String posTag = tkn.getPOSTag()
                posTag == null || posTag.endsWith("_END") || posTag.startsWith("<")
            }
            
            List<Double> rates = null
            if( options.disambiguate && readings.size() > 1 ) {
                rates = disambigStats.orderByStats(readings, cleanToken, tokens, idx, stats)
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
            t['q'] = q.round(3)
        }
    }
    
    
    @CompileStatic
    private TaggedToken getTagTokens(AnalyzedToken tkn, List<String> splitPart) {
        String posTag = tkn.getPOSTag()
        String semTags = semTags.getSemTags(tkn, posTag)

//        posTag = posTag.replaceFirst(/:r(in)?anim/, '')
        
        String lemma = tkn.getLemma() ?: ''
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


    def process() {
        stats = new TagStats()
        stats.options = options
        
        def outputFile = textUtils.processByParagraph(options, { buffer ->
            return tagText(buffer)
        },
		{ TagResult result ->
			stats.add(result.stats) 
		});
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

    
    @CompileStatic
    static boolean isTagEmpty(String posTag) {
        posTag == null || posTag.endsWith("_END")
    }
    

    @CompileStatic
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
            if( options.outputFormat == OutputFormat.txt ) {
                System.err.println ("Semantic tagging only available in xml/json output")
                System.exit 1
            }

            disambigStats.loadDisambigStats()
        }
    }

    void setInputOutput(TagOptions options) {
        
        if( ! options.input ) {
            options.input = "-"
        }
        if( ! options.output ) {
            def fileExt = "." + options.outputFormat // ? ".xml" : ".txt"
            def outfile = options.input == '-' ? '-' : options.input.replaceFirst(/\.txt$/, ".tagged${fileExt}")
            options.output = outfile
        }

    }
    
    void download() {
        disambigStats.download()
        semTags.download()
    }
    
	
    static void main(String[] args) {

        def nlpUk = new TagTextCore()
        
        def options = parseOptions(args)

        nlpUk.setOptions(options)
        
        if( options.download ) {
            nlpUk.download()
            return
        }

        // TODO: quick hack to support multiple files
        if( options.inputFiles && options.inputFiles != ["-"] ) {
            options.inputFiles.forEach{ filename ->
                options.output = ""
                options.input = filename
                nlpUk.setInputOutput(options)
                nlpUk.process()
                nlpUk.postProcess()
            }
        }
        else {
            nlpUk.process()
            nlpUk.postProcess()
        }
    }

}
