#!/usr/bin/env groovy

package org.nlp_uk.tools

@GrabConfig(systemClassLoader=true)
@Grab(group='org.languagetool', module='language-uk', version='5.2')
//@Grab(group='org.languagetool', module='language-uk', version='5.3-SNAPSHOT')
@Grab(group='ch.qos.logback', module='logback-classic', version='1.2.3')
@Grab(group='info.picocli', module='picocli', version='4.6.+')

import java.util.regex.Pattern

import org.languagetool.*
import org.languagetool.language.*

import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import groovy.json.JsonOutput
import groovy.json.StringEscapeUtils
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.util.Eval
import groovy.xml.MarkupBuilder
import picocli.CommandLine
import picocli.CommandLine.Option
import picocli.CommandLine.ParameterException


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

    static final Pattern PUNCT_PATTERN = Pattern.compile(/[\p{Punct}«»„“…—–]+/)
    static final Pattern LATIN_WORD_PATTERN = Pattern.compile(/\p{IsLatin}+/)

    JLanguageTool langTool = new MultiThreadedJLanguageTool(new Ukrainian())

    TagOptions options
	Map<String, String> semanticTags = new HashMap<>()

	@Canonical
	static class TagResult {
		String tagged
		Stats stats
	}
	
	Stats stats = new Stats()


    void setOptions(TagOptions options) {
        this.options = options
        options.adjust()

        if( options.semanticTags ) {
			if( options.outputFormat == OutputFormat.txt ) {
				System.err.println ("Semantic tagging only available in xml/json output")
				System.exit 1
			}
			
            System.err.println ("Using semantic tagging")
			
			loadSemTags()
        }
    }


    @CompileStatic
    def tagText(String text) {
        List<AnalyzedSentence> analyzedSentences = langTool.analyzeText(text);
		
		StringWriter writer
		GroovyObjectSupport builder
	
		if( options.outputFormat == OutputFormat.xml ) {
			writer = new StringWriter()
			builder = new MarkupBuilder(writer)
		}
        else if( options.outputFormat == OutputFormat.json ) {
            def gen = new JsonGenerator.Options() \
                .disableUnicodeEscaping()
                .build()
            builder = new JsonBuilder(gen)
        }

        def sb = new StringBuilder()
        for (AnalyzedSentence analyzedSentence : analyzedSentences) {
            if( options.outputFormat == OutputFormat.xml ) {
                AnalyzedTokenReadings[] tokens = analyzedSentence.getTokensWithoutWhitespace()

                def tokenReagings = tagAsObject(tokens)
                
				outputSentenceXml(builder, tokenReagings)

				sb.append(writer.toString()).append("\n");
                writer.getBuffer().setLength(0)
            }
            else if( options.outputFormat == OutputFormat.json ) {
                AnalyzedTokenReadings[] tokens = analyzedSentence.getTokensWithoutWhitespace()

                def tokenReadingObj = tagAsObject(tokens)
                
                def s = outputSentenceJson(builder, tokenReadingObj)
                if( sb.length() > 0 ) sb.append(",\n");
                sb.append(s)
//                sb.append("\n");
            }
            else if ( ! options.noTag ) {
                String sentenceLine
                if( options.firstLemmaOnly ) {
                    sentenceLine = analyzedSentence.tokens.collect { 
                        AnalyzedTokenReadings it -> 
                        it.isSentenceStart()
                            ? ''
                            : it.isWhitespace()
                                ? ( "\n".equals(it.getToken()) ? '' : it.getToken() )
                                : it.getToken() + "[" + it.getReadings().get(0).toString() + "]" 
                    }
                    .join('') //(' --- ')
                }
                else {
                    sentenceLine = analyzedSentence.toString()
                    if( options.tokenPerLine ) {
                        sentenceLine = sentenceLine.replaceAll(/(<S>|\]) */, '$0\n')
                    }
                    else {
                        sentenceLine = sentenceLine.replaceAll(/ *(<S>|\[<\/S>\]) */, '')
                    }
                }


                sb.append(sentenceLine) //.append("\n");
            }
        }

		def stats = new Stats()
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

    @CompileStatic
    private tagAsObject(AnalyzedTokenReadings[] tokens) {
        def tokenReadingsT = []

        tokens[1..-1].eachWithIndex { AnalyzedTokenReadings tokenReadings, int idx ->
            String theToken = tokenReadings.getToken()

            if( tokenReadings.isLinebreak() ) {
                def nonEndTagToken = tokenReadings.find { AnalyzedToken it ->
                    ! (it.getPOSTag() in [JLanguageTool.PARAGRAPH_END_TAGNAME, JLanguageTool.SENTENCE_END_TAGNAME])
                }

                if( nonEndTagToken == null )
                    return
            }

            if( tokenReadings.isPosTagUnknown() ) {
                if( isZheleh(options) ) {
                    tokenReadings = adjustTokensWithZheleh(tokenReadings, tokens, idx)
                }
            }


            if( tokenReadings.isPosTagUnknown() && PUNCT_PATTERN.matcher(theToken).matches() ) {
                tokenReadingsT << [tokens: [['value': tokenReadings.getToken(), 'tags': 'punct', 'whitespaceBefore': tokenReadings.isWhitespaceBefore()]]]
                return
            }
            else if( tokenReadings.isPosTagUnknown() && LATIN_WORD_PATTERN.matcher(theToken).matches() ) {
                tokenReadingsT << [tokens: [['value': tokenReadings.getToken(), 'tags': 'noninfl:foreign']]]
                return
            }

            def item = [tokens: []]
            
            List<AnalyzedToken> readings = tokenReadings.getReadings()

            readings.each { AnalyzedToken tkn ->
                String posTag = tkn.getPOSTag()
                if( posTag == JLanguageTool.SENTENCE_END_TAGNAME ) {
                    if( tokenReadings.getReadings().size() > 1 )
                        return
                    posTag = ''
                }

                def semTags = null
                if( options.semanticTags && tkn.getLemma() ) {
                    def key = tkn.getLemma()
                    if( posTag ) {
                        int xpIdx = posTag.indexOf(":xp")
                        if( xpIdx >= 0 ) {
                            key += " " + posTag[xpIdx+1..xpIdx+3]
                        }
                    }
                    semTags = semanticTags.get(key)
                }

                def token = semTags \
                        ? ['value': tkn.getToken(), 'lemma': tkn.getLemma(), 'tags': posTag, 'semtags': semTags]
                        : ['value': tkn.getToken(), 'lemma': tkn.getLemma(), 'tags': posTag]
                
                item.tokens << token 
            }
            
            tokenReadingsT << item
        }
            
        tokenReadingsT
    }


    private outputSentenceXml(GroovyObjectSupport builder, tokenReadings) {
        builder.'sentence'() {
            tokenReadings.each { tr -> tr
                'tokenReading'() {
                    tr.tokens.each { t -> 
                       'token'(t)
                    }
                } 
            }
        }
    }

    private String outputSentenceJson(GroovyObjectSupport builder, tokenReadingsList) {
        builder {
            tokenReadings tokenReadingsList.collect { tr ->
                [ 
                    tokens: tr.tokens.collect { t ->
                        t
                    }
                ]
            }
        }
        String jsonOut = builder.toString()
        jsonOut = JsonOutput.prettyPrint(jsonOut)
        jsonOut = StringEscapeUtils.unescapeJavaScript(jsonOut)
        jsonOut = jsonOut.replaceAll(/(?m)^(.)/, '        $1')
        return jsonOut
    }

    @CompileStatic
    private static String adjustZheleh(String text) {
        text = text.replaceAll(/(?ui)([бвгґджзклмнпрстфхцчшщ])ї/, '$1і')
        text = text.replaceAll(/(?ui)([бвпмфр])([юяє])/, '$1\'$2')
        text = text.replaceAll(/(?ui)([сцз])ь([бвпмф])([ія])/, '$1$2$3')
        text = text.replaceAll(/(?ui)ь([сц])(к)/, '$1ь$2')
        text = text.replaceAll(/(?ui)-(же|ж|би|б)/, ' $1')
    }

    @CompileStatic
	AnalyzedTokenReadings adjustTokensWithZheleh(AnalyzedTokenReadings tokenReadings, AnalyzedTokenReadings[] tokens, int idx) {
		AnalyzedToken origAnalyzedToken = tokenReadings.getReadings().get(0)
		boolean syaIsNext = idx < tokens.size()-2 && tokens[idx+2].getToken() == 'ся'

		if( ( tokenReadings.isPosTagUnknown() || syaIsNext )
			&& origAnalyzedToken.token =~ /[а-яіїєґА-ЯІЇЄҐ]/ ) {

			String adjustedToken = adjustZheleh(origAnalyzedToken.token)

			if( syaIsNext ) {
				tokenReadings = langTool.getLanguage().getTagger().tag([adjustedToken + 'ся']).get(0)
				//                                    println "trying verb:rev $adjustedToken " + tokenReadings.getReadings()
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


    def adjustRules() {
        if( options.disabledRules ) {
            if( ! options.quiet ) {
                System.err.println("Disabled rules: $options.disabledRules")
            }

            def allRules = nlpUk.langTool.language.disambiguator.disambiguator.disambiguationRules
            def rulesToDisable = options.disabledRules.split(",")

            def rulesNotFound = rulesToDisable - allRules.collect { it.id }
            if( rulesNotFound ) {
                System.err.println("WARNING: rules not found for ids: " + rulesNotFound.join(", "))
            }

            allRules.removeAll {
                it.id in rulesToDisable
            }
        }
    }

    def process() {
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
    }
	
	def loadSemTags() {
		// def base = System.getProperty("user.home") + "/work/ukr/spelling/dict_uk/data/sem"
		def base = "https://raw.githubusercontent.com/brown-uk/dict_uk/master/data/sem"
		def semDir = new File("sem")
		if( semDir.isDirectory() ) {
			base = "${semDir.path}"
			System.err.println("Loading semantic tags from ./sem")
		}
		else {
			System.err.println("Loading semantic tags from $base")
		}

		["noun", "adj", "adv", "verb"].each { cat ->
			def lines = base.startsWith("http")
				? "$base/${cat}.csv".toURL().getText("UTF-8")
				: new File(semDir, "${cat}.csv").getText("UTF-8")
			lines.eachLine { line ->
				def parts = line.split(',')
				if( parts.length < 2 ) {
					System.err.println("skipping invalid semantic tag for: " + line)
					return
				}
				def key = parts[0]
				semanticTags.put(key, parts[1])
			}
		}
		System.err.println("Loaded ${semanticTags.size()} semantic tags")
	}
	
    static class TagOptions {
//        @Parameters(arity="1", paramLabel="input", description="The file(s) whose checksum to calculate.")
        @Option(names = ["-i", "--input"], arity="1", description = ["Input file"])
        String input
        @Option(names = ["-o", "--output"], arity="1", description = ["Output file (default: <input file> - .txt + .tagged.txt/.xml)"])
        String output
        @Option(names = ["-l", "--tokenPerLine"], description = ["One token per line"])
        boolean tokenPerLine
        @Option(names = ["-f", "--firstLemmaOnly"], description = ["print only first lemma with first set of tags"
            + " (note: this mode is not recommended as first lemma/tag is almost random, this may be improved later with statistical analysis)"])
        boolean firstLemmaOnly
        @Option(names = ["-x", "--xmlOutput"], description = ["Output in xml format"])
        boolean xmlOutput
        @Option(names = ["-n", "--outputFormat"], arity="1", description = ["Output format: {txt, xml, json}"])
        OutputFormat outputFormat
        @Option(names = ["-s", "--homonymStats"], description = ["Collect homohym statistics"])
        boolean homonymStats
        @Option(names = ["-u", "--unknownStats"], description = ["Collect unknown words statistics"])
        boolean unknownStats
        @Option(names = ["-b", "--filterUnknown"], description = ["Filter out unknown words with non-Ukrainian character combinations"])
        boolean filterUnknown
        @Option(names = ["-w", "--frequencyStats"], description = ["Collect word frequency"])
        boolean frequencyStats
        @Option(names = ["-z", "--lemmaStats"], description = ["Collect lemma frequency"])
        boolean lemmaStats
        @Option(names = ["-e", "--semanticTags"], description = ["Add semantic tags"])
        boolean semanticTags
        @Option(names = ["-k", "--noTag"], description = ["Do not write tagged text (only perform stats)"])
        boolean noTag
        @Option(names = ["-m", "--modules"], arity="1", description = ["Comma-separated list of modules, supported modules: [zheleh]"])
        List<String> modules
        @Option(names = ["--singleThread"], description = ["Always use single thread (default is to use multithreading if > 2 cpus are found)"])
        boolean singleThread
        @Option(names = ["-q", "--quiet"], description = ["Less output"])
        boolean quiet
        @Option(names= ["-h", "--help"], usageHelp= true, description= "Show this help message and exit.")
        boolean helpRequested
        @Option(names = ["-d", "--disableDisamgigRules"], arity="1", required = false, description = ["Comma-separated list of ids of disambigation rules to disable"])
        List<String> disabledRules
        // experimental
        boolean ignoreOtherLanguages
        
        void adjust() {
            if( ! outputFormat ) {
                if( xmlOutput ) {
                    outputFormat = OutputFormat.xml
                }
                else {
                    outputFormat = OutputFormat.txt
                }
            }
            if( ! quiet ) {
                println "Output format: " + outputFormat
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
        
        options.adjust()
        
        if( ! options.output ) {
            def fileExt = "." + options.outputFormat // ? ".xml" : ".txt"
            def outfile = options.input == '-' ? '-' : options.input.replaceFirst(/\.txt$/, '') + ".tagged" + fileExt
            options.output = outfile
        }


        if( ! options.quiet ) {
            if( isZheleh(options) ) {
                System.err.println ("Using adjustments for Zhelekhivka")
                if( options.frequencyStats || options.unknownStats || options.lemmaStats ) {
                    System.err.println ("NOTE: Zhelekhivka adjustments currently do not apply to statistics!")
                }
            }
        }

        return options
    }

	
	class Stats {
		def homonymFreqMap = [:].withDefault { 0 }
		def homonymTokenMap = [:].withDefault{ new HashSet<>() }
		def unknownMap = [:].withDefault { 0 }
		def frequencyMap = [:].withDefault { 0 }
		def lemmaFrequencyMap = [:].withDefault { 0 }
		def knownMap = [:].withDefault { 0 }
		def knownCnt = 0

		
		synchronized void add(Stats stats) {
			stats.homonymFreqMap.each { k,v -> homonymFreqMap[k] += v }
			stats.homonymTokenMap.each { k,v -> homonymTokenMap[k] += v }
			stats.unknownMap.each { k,v -> unknownMap[k] += v }
			stats.knownMap.each { k,v -> knownMap[k] += v }
			stats.frequencyMap.each { k,v -> frequencyMap[k] += v }
			stats.lemmaFrequencyMap.each { k,v -> lemmaFrequencyMap[k] += v }
		    knownCnt += stats.knownCnt
		}
		
		def collectUnknown(List<AnalyzedSentence> analyzedSentences) {
			for (AnalyzedSentence analyzedSentence : analyzedSentences) {
				// if any words contain Russian sequence filter out the whole sentence - this removes tons of Russian words from our unknown list
				// we could also test each word against Russian dictionary but that would filter out some valid Ukrainian words too
				if( options.filterUnknown ) {
					def unknownBad = analyzedSentence.getTokensWithoutWhitespace()[1..-1].find { AnalyzedTokenReadings tokenReadings ->
						tokenReadings.getToken() =~ /[ыэъё]|и[еи]/
					}
					if( unknownBad )
						continue
				}

				analyzedSentence.getTokensWithoutWhitespace()[1..-1].each { AnalyzedTokenReadings tokenReadings ->
					if( (tokenReadings.getAnalyzedToken(0).getPOSTag() == null
					|| JLanguageTool.SENTENCE_END_TAGNAME.equals(tokenReadings.getAnalyzedToken(0).getPOSTag()) )
					&& tokenReadings.getToken() =~ /[а-яіїєґА-ЯІЇЄҐ]/
					&& ! (tokenReadings.getToken() =~ /[ыэъё]|ие|ннн|оі$|[а-яіїєґА-ЯІЇЄҐ]'?[a-zA-Z]|[a-zA-Z][а-яіїєґА-ЯІЇЄҐ]/) ) {
						unknownMap[tokenReadings.getToken()] += 1
					}
				}
				analyzedSentence.getTokensWithoutWhitespace()[1..-1].each { AnalyzedTokenReadings tokenReadings ->
				    if( ! (tokenReadings.getToken() =~ /[0-9]|^[a-zA-Z-]+$/) && tokenReadings.getReadings().any { AnalyzedToken at -> 
				            at.getPOSTag() != null && ! (at.getPOSTag() =~ /_END/)
				        } ) {
				      knownCnt++
				      knownMap[tokenReadings.getToken()] += 1
				    }
				}
			}
		}


		def collectHomonyms(List<AnalyzedSentence> analyzedSentences) {

			for (AnalyzedSentence analyzedSentence : analyzedSentences) {
				for(AnalyzedTokenReadings readings: analyzedSentence.getTokens()) {
					if( readings.size() > 1 ) {
						if( readings.getReadings()[0].getPOSTag().equals(JLanguageTool.SENTENCE_END_TAGNAME) )
							continue

						if( readings.getReadings()[-1].getPOSTag().equals(JLanguageTool.SENTENCE_END_TAGNAME) ) {
							if( readings.size() == 2 )
								continue
							readings = new AnalyzedTokenReadings(readings.getReadings()[0..-2], readings.getStartPos())
						}
						if( readings.getReadings()[-1].getPOSTag() == null && readings.getToken().contains('\u0301') ) {
							readings = new AnalyzedTokenReadings(readings.getReadings()[0..-2], readings.getStartPos())
						}

						def key = readings.join("|")
						homonymFreqMap[key] += 1
						homonymTokenMap[key] << readings.getToken()
					}
				}
			}
		}


		def collectFrequency(List<AnalyzedSentence> analyzedSentences) {
			for (AnalyzedSentence analyzedSentence : analyzedSentences) {
				analyzedSentence.getTokensWithoutWhitespace()[1..-1].each { AnalyzedTokenReadings tokenReadings ->
					if( tokenReadings.getAnalyzedToken(0).getPOSTag()
					&& tokenReadings.getToken() =~ /[а-яіїєґА-ЯІЇЄҐ]/
					&& ! (tokenReadings.getToken() =~ /[ыэъё]|[а-яіїєґА-ЯІЇЄҐ]'?[a-zA-Z]|[a-zA-Z][а-яіїєґА-ЯІЇЄҐ]/) ) {
						frequencyMap[tokenReadings.getToken()] += 1
					}
				}
			}
		}

		def collectLemmaFrequency(List<AnalyzedSentence> analyzedSentences) {
			for (AnalyzedSentence analyzedSentence : analyzedSentences) {
				analyzedSentence.getTokensWithoutWhitespace()[1..-1].each { AnalyzedTokenReadings tokenReadings ->
					tokenReadings.getReadings()
							.findAll { it.getLemma() \
						&& tokenReadings.getToken() ==~ /[а-яіїєґА-ЯІЇЄҐ'-]+/
							}
							.unique()
							.each {
								lemmaFrequencyMap[ it.getLemma() ] += 1
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
	                homonymTokenMap[k] = homonymTokenMap[k].collect { it.toLowerCase() }.unique()
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
			double unknownPct = knownCnt+unknownCnt ? unknownCnt*100/(knownCnt+unknownCnt) : 0
	        println "Known: $knownCnt, unknown: $unknownCnt, " + String.format("%.1f", unknownPct) + "%"
			double unknownFrPct = knownMap.size()+unknownMap.size() ? unknownMap.size()*100/(knownMap.size()+unknownMap.size()) : 0
	        println "Known unique: ${knownMap.size()}, unknown unique: " + unknownMap.size() + ", " + String.format("%.1f", unknownFrPct) + "%"
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
	            def str = String.format("%6d\t%s", v, k)
	            printStream.println(str)
	        }
	    }
	}

	
	
    static void main(String[] args) {

        def nlpUk = new TagText()

        def options = parseOptions(args)

        nlpUk.setOptions(options)

        nlpUk.adjustRules()

        nlpUk.process()

        nlpUk.postProcess()
    }

}
