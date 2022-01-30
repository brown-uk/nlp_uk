#!/usr/bin/env groovy

package org.nlp_uk.tools

@GrabConfig(systemClassLoader=true)
//@Grab(group='org.languagetool', module='language-uk', version='5.6')
@Grab(group='org.languagetool', module='language-uk', version='5.7-SNAPSHOT')
@Grab(group='ch.qos.logback', module='logback-classic', version='1.2.3')
@Grab(group='info.picocli', module='picocli', version='4.6.+')

import java.math.RoundingMode
import java.util.regex.Pattern
import java.util.stream.Collectors
import org.apache.commons.lang3.mutable.MutableInt
import org.languagetool.*
import org.languagetool.language.*
import org.nlp_uk.bruk.ContextToken
import org.nlp_uk.bruk.WordContext
import org.nlp_uk.bruk.WordReading
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.util.Eval

import picocli.CommandLine
import picocli.CommandLine.Option
import picocli.CommandLine.ParameterException
import picocli.CommandLine.Parameters


class TagText {
    enum OutputFormat { txt, xml, json }
    
    @groovy.transform.SourceURI
    static SOURCE_URI
    // if this script is called from GroovyScriptEngine SourceURI is data: and does not work for File()
    static SCRIPT_DIR = SOURCE_URI.scheme == "data" 
		? new File("src/main/groovy/org/nlp_uk/tools")
		: new File(SOURCE_URI).parent

    // easy way to include a class without forcing classpath to be set
    def textUtils = Eval.me(new File("$SCRIPT_DIR/TextUtils.groovy").text + "\n new TextUtils()")
    
    static final Pattern PUNCT_PATTERN = Pattern.compile(/[,.:;!?\/()\[\]{}«»„“"'…\u2013\u2014\u201D\u201C•■♦-]+/)
    static final Pattern SYMBOL_PATTERN = Pattern.compile(/[%&@$*+=<>\u00A0-\u00BF\u2000-\u20CF\u2100-\u218F\u2200-\u22FF]+/)
    static final Pattern UNKNOWN_PATTERN = Pattern.compile(/(.*-)?[а-яіїєґА-ЯІЇЄҐ]+(-.*)?/)
    static final Pattern NON_UK_PATTERN = Pattern.compile(/[ыэъёЫЭЪЁ]|[иИ]{2}/)
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
    


    @CompileStatic
    def tagText(String text) {
        List<AnalyzedSentence> analyzedSentences = langTool.analyzeText(text);
		
//		StringWriter writer
//		GroovyObjectSupport builder
	
		if( options.outputFormat == OutputFormat.xml ) {
//			writer = new StringWriter()
//			builder = new MarkupBuilder(writer)
		}
        else if( options.outputFormat == OutputFormat.json ) {
//            def gen = new JsonGenerator.Options() \
//                .disableUnicodeEscaping()
//                .build()
//            builder = new JsonBuilder(gen)
        }

        def stats = new TagStats()
        stats.options = options
        
        def sb = new StringBuilder()
        for (AnalyzedSentence analyzedSentence : analyzedSentences) {
            
            analyzedSentence.getTokens().each { AnalyzedTokenReadings t ->
                def multiWordReadings = t.getReadings().findAll { AnalyzedToken r ->
                    r.getPOSTag() != null && r.getPOSTag().startsWith("<")
                }
                multiWordReadings.each { r ->
                    t.removeReading(r, "remove_multiword")
                }
            }

            if ( ! options.noTag ) {

                if( options.outputFormat == OutputFormat.xml ) {
                    def field = AnalyzedSentence.getDeclaredField("nonBlankTokens")
                    field.setAccessible(true)
                    AnalyzedTokenReadings[] tokens = analyzedSentence.getTokensWithoutWhitespace()

                    def taggedObjects = tagAsObject(tokens, stats)

                    StringBuilder x = outputSentenceXml(taggedObjects)
                    sb.append(x).append("\n")
                    
                    if( options.tokenFormat ) {
                        if( tokens[-1].hasPosTag(JLanguageTool.PARAGRAPH_END_TAGNAME) ) {
                            sb.append("<paragraph/>\n")
                        }
                    }
                }
                else if( options.outputFormat == OutputFormat.json ) {
                    AnalyzedTokenReadings[] tokens = analyzedSentence.getTokensWithoutWhitespace()

                    def tokenReadingObj = tagAsObject(tokens, stats)

                    def s = outputSentenceJson(tokenReadingObj)
                    if( sb.length() > 0 ) sb.append(",\n");
                    sb.append(s)
                }
                else {
                    String sentenceLine
                    sentenceLine = analyzedSentence.toString()
                    sentenceLine.replaceAll(/,[^\/].*?\/<.*?>/, '')
                    if( options.tokenPerLine ) {
                        sentenceLine = sentenceLine.replaceAll(/(<S>|\]) */, '$0\n')
                    }
                    else {
                        sentenceLine = sentenceLine.replaceAll(/ *(<S>|\[<\/S>\]) */, '')
                    }

                    sb.append(sentenceLine) //.append("\n");
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
        }
        
        if( options.homonymStats ) {
            stats.collectHomonyms(analyzedSentences)
        }
        if( options.unknownStats ) {
            stats.collectUnknown(analyzedSentences)
        }
        if( options.frequencyStats ) {
            stats.collectFrequency(analyzedSentences)
        }
        if( options.lemmaStats ) {
            stats.collectLemmaFrequency(analyzedSentences)
        }

        return new TagResult(sb.toString(), stats)
    }

    private static class TTR {
        List<?> tokens
    }

    @CompileStatic
    private static boolean hasPosTag(AnalyzedTokenReadings tokenReadings) {
        tokenReadings.getReadings().stream()
            .anyMatch{ t -> ! isTagEmpty(t.getPOSTag()) }
    }   
    
    @CompileStatic
    private List<TTR> tagAsObject(AnalyzedTokenReadings[] tokens, TagStats stats) {
        tokens = tokens[1..-1] // remove SENT_START
        List<TTR> tokenReadingsT = []

        tokens.eachWithIndex { AnalyzedTokenReadings tokenReadings, int idx ->
            String theToken = tokenReadings.getToken()
            String cleanToken = tokenReadings.getCleanToken()
            
            boolean hasTag = hasPosTag(tokenReadings) 

            // TODO: ugly workaround for disambiguator problem
            if( ! hasTag || "\u2014".equals(theToken) ) {
                if( tokenReadings.isLinebreak() )
                    return
    
                if( PUNCT_PATTERN.matcher(theToken).matches() ) {
                    tokenReadingsT << new TTR(tokens: [[value: theToken, lemma: cleanToken, tags: 'punct', 'whitespaceBefore': tokenReadings.isWhitespaceBefore()]])
                    return
                }
                else if( SYMBOL_PATTERN.matcher(theToken).matches() ) {
                    tokenReadingsT << new TTR(tokens: [['value': theToken, lemma: cleanToken, tags: 'symb']])
                    return
                }
                else if( XML_TAG_PATTERN.matcher(theToken).matches() ) {
                    tokenReadingsT << new TTR(tokens: [[value: theToken, lemma: cleanToken, tags: 'xmltag']])
                    return
                }
                else if( UNKNOWN_PATTERN.matcher(theToken).matches() && ! NON_UK_PATTERN.matcher(theToken).find() ) {
                    if( isZheleh(options) ) {
                        tokenReadings = adjustTokensWithZheleh(tokenReadings, tokens, idx)
                        hasTag = hasPosTag(tokenReadings)
                    }
                    
                    if( ! hasTag ) {
                        def lemma = options.setLemmaForUnknown ? cleanToken : ''
                        tokenReadingsT << new TTR(tokens: [['value': theToken, lemma: lemma, tags: 'unknown']])
                        return
                    }
                }
                else { // if( UNCLASS_PATTERN.matcher(theToken).matches() ) {
                    tokenReadingsT << new TTR(tokens: [['value': theToken, lemma: cleanToken, tags: 'unclass']])
                    return
                }

            }

            
            TTR item = new TTR(tokens: [])
            
            List<AnalyzedToken> readings = new ArrayList<>(tokenReadings.getReadings())
            readings.removeIf{ it -> it.getPOSTag() != null && it.getPOSTag().endsWith("_END") }
            
            readings = readings.findAll { AnalyzedToken tkn ->
                String posTag = tkn.getPOSTag()
                posTag != null && ! posTag.startsWith("<")
            }
            
            List<BigDecimal> rates = null
            if( options.disambiguate && readings.size() > 1 ) {
                rates = disambigStats.orderByStats(readings, cleanToken, tokens, idx, stats)
            }

            if( options.tokenFormat ) {
                List<Object> tagTokens = readings.collect { AnalyzedToken tkn ->
                    getTagTokens(tkn)
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
                    getTagTokens(tkn)
                }
            }
            
            tokenReadingsT << item
        }
            
        tokenReadingsT
    }

    @CompileStatic
    static void addRates(List<Object> tagTokens, List<BigDecimal> rates) {
        BigDecimal sum = (BigDecimal) rates.sum()
        tagTokens.eachWithIndex { Object t, int idx2 ->
//            if( ! sum ) System.err.println "sum of 0 for: $tagTokens"
            BigDecimal q = sum ? rates[idx2] / sum : BigDecimal.ZERO
            t['q'] = q.round(3)
        }
    }
    
    
    @CompileStatic
    private getTagTokens(AnalyzedToken tkn) {
        String posTag = tkn.getPOSTag()
        String semTags = semTags.getSemTags(tkn, posTag)

        String lemma = tkn.getLemma() ?: ''
        def token = semTags \
                        ? ['value': tkn.getToken(), 'lemma': lemma, 'tags': posTag, 'semtags': semTags]
                : ['value': tkn.getToken(), 'lemma': lemma, 'tags': posTag]
    }



    private StringBuilder outputSentenceXml(taggedObjects) {
//        builder.'sentence'() {
//            tokenReadings.each { tr -> tr
//                'tokenReading'() {
//                    tr.tokens.each { t -> 
//                       'token'(t)
//                    }
//                } 
//            }
//        }
        
        // XmlBuilder is nice but using strings gives almost 20% speedup on large files
        StringBuilder sb = new StringBuilder(1024)
        sb.append("<sentence>\n");
        taggedObjects.each { tr -> tr
            if( ! options.tokenFormat ) {
                sb.append("  <tokenReading>\n")
            }
            tr.tokens.each { t -> 
                appendToken(t, sb)
            }
            if( ! options.tokenFormat ) {
                sb.append("  </tokenReading>\n")
            }
        }
        sb.append("</sentence>");
        return sb
    }

    private void appendToken(t, StringBuilder sb) {
        String indent = options.tokenFormat ? "  " : "    "
        sb.append(indent).append("<token value=\"").append(quoteXml(t.value, false)).append("\"")
        if( t.lemma != null ) {
            sb.append(" lemma=\"").append(quoteXml(t.lemma, false)).append("\"")
        }
        if( t.tags ) {
            sb.append(" tags=\"").append(quoteXml(t.tags, false)).append("\"")

            if( ! options.tokenFormat ) {
                if( t.tags == "punct" ) {
                    sb.append(" whitespaceBefore=\"").append(t.whitespaceBefore).append("\"")
                }
            }
        }
        if( t.semtags ) {
            sb.append(" semtags=\"").append(quoteXml(t.semtags, false)).append("\"")
        }
        if( t.q != null ) {
            sb.append(" q=\"").append(t.q).append("\"")
        }

        if( t.alts ) {
            sb.append(">\n    <alts>\n")
            t.alts.each { ti ->
                sb.append("    ")
                appendToken(ti, sb)
            }
            sb.append("    </alts>\n").append(indent).append("</token>\n")
        }
        else {
            sb.append(" />\n")
        }
    }
    
    private StringBuilder outputSentenceJson(tokenReadingsList) {
//        builder {
//            tokenReadings tokenReadingsList.collect { tr ->
//                [ 
//                    tokens: tr.tokens.collect { t ->
//                        t
//                    }
//                ]
//            }
//        }
//        String jsonOut = builder.toString()
//        jsonOut = JsonOutput.prettyPrint(jsonOut)
//        jsonOut = StringEscapeUtils.unescapeJavaScript(jsonOut)
//        jsonOut = jsonOut.replaceAll(/(?m)^(.)/, '        $1')
//        return jsonOut

        // JsonBuilder is nice but using strings gives almost 40% speedup on large files
        StringBuilder sb = new StringBuilder(1024)
        sb.append("    {\n");
        sb.append("      \"tokenReadings\": [\n");
        tokenReadingsList.eachWithIndex { tr, trIdx -> tr
            sb.append("        {\n");
            sb.append("          \"tokens\": [\n");
            
            tr.tokens.eachWithIndex { t, tIdx -> 
                sb.append("            { ")
                sb.append("\"value\": \"").append(quoteJson(t.value)).append("\"")
                if( t.lemma != null ) {
                    sb.append(", \"lemma\": \"").append(quoteJson(t.lemma)).append("\"")
                }
                if( t.tags != null ) {
                    sb.append(", \"tags\": \"").append(t.tags).append("\"")
                    if( t.tags == "punct" ) {
                        sb.append(", \"whitespaceBefore\": ").append(t.whitespaceBefore) //.append("")
                    }
                }
                if( t.semtags ) {
                    sb.append(", \"semtags\": \"").append(t.semtags).append("\"")
                }
                sb.append(" }");
                if( tIdx < tr.tokens.size() - 1 ) {
                    sb.append(",")
                }
                sb.append("\n")
            }

            sb.append("          ]");
            sb.append("\n        }");
            if( trIdx < tokenReadingsList.size() - 1 ) {
                sb.append(",")
            }
            sb.append("\n")
        }
        sb.append("      ]\n");
        sb.append("    }");
        return sb
    }
    
    @CompileStatic
    static String quoteJson(String s) {
        s.replace('"', '\\"')
    }
    @CompileStatic
    static String quoteXml(String s, boolean withApostrophe) {
//        XmlUtil.escapeXml(s)
        // again - much faster on our own
        s = s.replace('&', "&amp;").replace('<', "&lt;").replace('>', "&gt;").replace('"', "&quot;")
        if( withApostrophe ) {
            s = s.replace('\'', "&apos;")
        }
        s
    }

    @CompileStatic
    private static String adjustZheleh(String text) {
        // найпаскуднїшою
        text = text.replaceAll(/(?ui)([бвгґджзклмнпрстфхцчшщ])ї/, '$1і')
        // і у сімї
        text = text.replaceAll(/(?ui)([бвпмфр])([юяє])/, '$1\'$2')
        text = text.replaceAll(/(?ui)([сцз])ь([бвпмф])([ія])/, '$1$2$3')
        // next are tagged as ":bad" in tagger
//        text = text.replaceAll(/(?ui)ь([сц])(к)/, '$1ь$2')
//        text = text.replaceAll(/(?ui)-(же|ж|би|б)/, ' $1')
    }

    @CompileStatic
	AnalyzedTokenReadings adjustTokensWithZheleh(AnalyzedTokenReadings tokenReadings, AnalyzedTokenReadings[] tokens, int idx) {
		AnalyzedToken origAnalyzedToken = tokenReadings.getReadings().get(0)
		boolean syaIsNext = idx < tokens.size()-1 && tokens[idx+1].getToken() == 'ся'

		if( ( tokenReadings.isPosTagUnknown() || syaIsNext )
			&& origAnalyzedToken.token =~ /[а-яіїєґА-ЯІЇЄҐ]/ ) {

			String adjustedToken = adjustZheleh(origAnalyzedToken.token)

			if( syaIsNext ) {
				tokenReadings = langTool.getLanguage().getTagger().tag([adjustedToken + 'ся']).get(0)
				// println "trying verb:rev $adjustedToken " + tokenReadings.getReadings()
			}

			if( tokenReadings.isPosTagUnknown() ) {
				tokenReadings = langTool.getLanguage().getTagger().tag([adjustedToken]).get(0)
			}

			// put back original word
			for(int i=0; i<tokenReadings.getReadings().size(); i++) {
				AnalyzedToken token = tokenReadings.getReadings().get(i);

				String posTag = token.getPOSTag()

				tokenReadings.getReadings().set(i, new AnalyzedToken(origAnalyzedToken.token, posTag, token.lemma))
			}
		}

		return tokenReadings
	}
	
	

    private static boolean isZheleh(options) {
        return options.modules && 'zheleh' in options.modules
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
    

    	
    static class TagOptions {
        @Option(names = ["-i", "--input"], arity="1", description = "Input file. Default: stdin")
        String input
        @Parameters(index = "0", description = "Input files. Default: stdin", arity="0..")
        List<String> inputFiles
        @Option(names = ["-o", "--output"], arity="1", description = "Output file (default: <input file base name> + .tagged.txt/.xml/.json) or stdout if input is stdin")
        String output
        @Option(names = ["-x", "--xmlOutput"], description = "Output in xml format (deprecated: use --outputFormat)")
        boolean xmlOutput
        @Option(names = ["-n", "--outputFormat"], arity="1", description = "Output format: {xml (default), json, txt}", defaultValue = "xml")
        OutputFormat outputFormat

        @Option(names = ["-sh", "--homonymStats"], description = "Collect homohym statistics")
        boolean homonymStats
        @Option(names = ["-su", "--unknownStats"], description = "Collect unknown words statistics")
        boolean unknownStats
        @Option(names = ["-sfu", "--filterUnknown"], description = "Filter out unknown words with non-Ukrainian character combinations")
        boolean filterUnknown
        @Option(names = ["-sf", "--frequencyStats"], description = "Collect word frequency")
        boolean frequencyStats
        @Option(names = ["-sl", "--lemmaStats"], description = "Collect lemma frequency")
        boolean lemmaStats

        @Option(names = ["-e", "--semanticTags"], description = "Add semantic tags")
        boolean semanticTags
        @Option(names = ["-l", "--tokenPerLine"], description = "One token per line (for .txt output only)")
        boolean tokenPerLine
        @Option(names = ["-k", "--noTag"], description = "Do not write tagged text (only perform stats)")
        boolean noTag
        @Option(names = ["--setLemmaForUnknown"], description = "Fill lemma for unknown words (default: empty lemma)")
        boolean setLemmaForUnknown

        @Option(names = ["-m", "--modules"], arity="1", description = "Comma-separated list of modules, supported modules: [zheleh]")
        List<String> modules
        
        @Option(names = ["--singleThread"], description = "Always use single thread (default is to use multithreading if > 2 cpus are found)")
        boolean singleThread
        @Option(names = ["-q", "--quiet"], description = "Less output")
        boolean quiet
        @Option(names= ["-h", "--help"], usageHelp= true, description= "Show this help message and exit.")
        boolean helpRequested

        @Option(names = ["-t", "--tokenFormat"], description = "Use <token> format (instead of <tokenReading>)")
        boolean tokenFormat
        @Option(names = ["-t1", "--singleTokenOnly"], description = "Print only one token")
        boolean singleTokenOnly

        @Option(names = ["-d", "--showDisambigRules"], description = "Show disambiguation rules applied")
        boolean showDisambigRules
        @Option(names = ["-g", "--disambiguate"], description = "Use disambiguation modules: [frequency, wordEnding, context]", arity="0..", defaultValue="frequency")
        public List<DisambigModule> disambiguate
        @Option(names = ["-gr", "--disambiguationRate"], description = "Show a disambiguated token ratings")
        boolean showDisambigRate

        public enum DisambigModule {
            frequency,
            wordEnding,
            context
        }
        
        void adjust() {
            if( ! outputFormat ) {
                if( xmlOutput ) {
                    outputFormat = OutputFormat.xml
                }
                else {
                    outputFormat = OutputFormat.xml
                }
            }
            if( singleTokenOnly ) {
                tokenFormat = true
            }


            if( disambiguate == null ) {
                if( showDisambigRate ) {
                    disambiguate = [DisambigModule.frequency]
                }
                else {
                    disambiguate = [] 
                }
            }
            else if( ! (DisambigModule.frequency in disambiguate) ) {
                disambiguate << DisambigModule.frequency
            }

            if( ! quiet ) {
                println "Output format: " + outputFormat
                println "Disambig: " + disambiguate
            }
        }
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

        disambigStats.options = options
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
    
    
	
    static void main(String[] args) {

        def nlpUk = new TagText()
        
        def options = parseOptions(args)

        nlpUk.setOptions(options)

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
