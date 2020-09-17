#!/usr/bin/env groovy

package org.nlp_uk.tools

@Grab(group='org.languagetool', module='language-uk', version='5.0')
//@Grab(group='org.languagetool', module='language-uk', version='5.1-SNAPSHOT')
@Grab(group='ch.qos.logback', module='logback-classic', version='1.2.3')
@Grab(group='commons-cli', module='commons-cli', version='1.4')

import java.util.regex.Pattern

import org.languagetool.*
import org.languagetool.rules.*
import org.languagetool.tagging.uk.IPOSTag
import org.languagetool.tokenizers.*
import org.languagetool.language.*
import org.languagetool.uk.*

import groovy.lang.Closure
import groovy.lang.Lazy
import groovy.transform.CompileStatic
import groovy.xml.MarkupBuilder
import groovy.util.Eval


class TagText {
    @groovy.transform.SourceURI
    static SOURCE_URI
    // if this script is called from GroovyScriptEngine SourceURI is data: and does not work for File()
    static SCRIPT_DIR = SOURCE_URI.scheme == "file" ? new File(SOURCE_URI).parent : new File(".")

    // easy way to include a class without forcing classpath to be set
    static textUtils = Eval.me(new File("$SCRIPT_DIR/TextUtils.groovy").text)

    static PUNCT_PATTERN = Pattern.compile(/[\p{Punct}«»„“…—–]+/)
    static LATIN_WORD_PATTERN = Pattern.compile(/\p{IsLatin}+/)

    JLanguageTool langTool = new MultiThreadedJLanguageTool(new Ukrainian())

    def options
    def homonymFreqMap = [:].withDefault { 0 }
    def homonymTokenMap = [:].withDefault{ new HashSet<>() }
    def unknownMap = [:].withDefault { 0 }
    def frequencyMap = [:].withDefault { 0 }
    def lemmaFrequencyMap = [:].withDefault { 0 }
	Map<String, String> semanticTags = new HashMap<>()

    StringWriter writer
    MarkupBuilder xml


    void setOptions(options) {
        this.options = options

        if( options.xmlOutput ) {
            writer = new StringWriter()
            xml = new MarkupBuilder(writer)
        }

        if( options.semanticTags ) {
			if( ! options.xmlOutput ) {
				System.err.println ("Semantic tagging only available in xml output")
				System.exit 1
			}
			
            System.err.println ("Using semantic tagging")
			
			loadSemTags()
        }
    }


    def tagText(String text) {
        List<AnalyzedSentence> analyzedSentences = langTool.analyzeText(text);

        def sb = new StringBuilder()
        for (AnalyzedSentence analyzedSentence : analyzedSentences) {
            if( options.xmlOutput ) {
                AnalyzedTokenReadings[] tokens = analyzedSentence.getTokensWithoutWhitespace()

				xml.'sentence'() {

					tokens[1..-1].eachWithIndex { AnalyzedTokenReadings tokenReadings, int idx ->

                        if( tokenReadings.isLinebreak() ) {
                            def nonEndTagToken = tokenReadings.find {
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

                        'tokenReading'() {

                            List<AnalyzedToken> tokenReadings2 = tokenReadings.getReadings()

                            tokenReadings2.each { AnalyzedToken tkn ->

                                if( PUNCT_PATTERN.matcher(tkn.getToken()).matches() ) {
                                    'token'('value': tkn.getToken(), 'tags': 'punct', 'whitespaceBefore': tkn.isWhitespaceBefore() )
                                }
                                else if( tokenReadings.isPosTagUnknown() && LATIN_WORD_PATTERN.matcher(tkn.getToken()).matches() ) {
                                    'token'('value': tkn.getToken(), 'tags': 'noninfl:foreign' )
                                }
                                else {
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
									semTags
									    ? 'token'('value': tkn.getToken(), 'lemma': tkn.getLemma(), 'tags': posTag, 'semtags': semTags)
										: 'token'('value': tkn.getToken(), 'lemma': tkn.getLemma(), 'tags': posTag)
                                }
                            }
                        }
                    }
                }
                sb.append(writer.toString()).append("\n");
                writer.getBuffer().setLength(0)
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


                sb.append(sentenceLine).append("\n");
            }
        }

        if( options.homonymStats ) {
            collectHomonyms(analyzedSentences)
        }
        if( options.unknownStats ) {
            collectUnknown(analyzedSentences)
        }
        if( options.frequencyStats ) {
            collectFrequency(analyzedSentences)
        }

        if( options.lemmaStats ) {
            collectLemmaFrequency(analyzedSentences)
        }

        return sb.toString()
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
                if( tokenReadings.getAnalyzedToken(0).getPOSTag() != null
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
            def outputFile = new File(options.output.replaceFirst(/\.(txt|xml)$/, '') + '.homonym.txt')
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
            def outputFile = new File(options.output.replaceFirst(/\.(txt|xml)$/, '') + '.unknown.txt')
            printStream = new PrintStream(outputFile, "UTF-8")
        }

        unknownMap
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
            def outputFile = new File(options.output.replaceFirst(/\.(txt|xml)$/, '') + '.freq.txt')
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
            def outputFile = new File(options.output.replaceFirst(/\.(txt|xml)$/, '') + '.lemma.freq.txt')
            printStream = new PrintStream(outputFile, "UTF-8")
        }

        lemmaFrequencyMap
        .sort { it.key }
        .each{ k, v ->
            def str = String.format("%6d\t%s", v, k)
            printStream.println(str)
        }
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
        });
    }

    def postProcess() {

        if( options.homonymStats ) {
            printHomonymStats()
        }
        if( options.unknownStats ) {
            printUnknownStats()
        }
        if( options.frequencyStats ) {
            printFrequencyStats()
        }
        if( options.lemmaStats ) {
            printLemmaFrequencyStats()
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
	

    static parseOptions(String[] argv) {
        def cli = new CliBuilder()

        cli.i(longOpt: 'input', args:1, required: true, 'Input file')
        cli.o(longOpt: 'output', args:1, required: false, 'Output file (default: <input file> - .txt + .tagged.txt/.xml)')
        cli.l(longOpt: 'tokenPerLine', '1 token per line')
        cli.f(longOpt: 'firstLemmaOnly', 'print only first lemma with first set of tags'
            + ' (note: this mode is not recommended as first lemma/tag is almost random, this may be improved later with statistical analysis)')
        cli.x(longOpt: 'xmlOutput', 'output in xml format')
        cli.d(longOpt: 'disableDisamgigRules', args:1, 'Comma-separated list of ids of disambigation rules to disable')
        cli.s(longOpt: 'homonymStats', 'Collect homohym statistics')
        cli.u(longOpt: 'unknownStats', 'Collect unknown words statistics')
        cli.b(longOpt: 'filterUnknown', 'Filter out unknown words with non-Ukrainian character combinations')
        cli.w(longOpt: 'frequencyStats', 'Collect word frequency')
        cli.z(longOpt: 'lemmaStats', 'Collect lemma frequency')
        cli.e(longOpt: 'semanticTags', 'Add semantic tags')
        cli.k(longOpt: 'noTag', 'Do not write tagged text (only perform stats)')
        cli.m(longOpt: 'modules', args:1, required: false, 'Comma-separated list of modules, supported modules: [zheleh]')
        cli.q(longOpt: 'quiet', 'Less output')
        cli.h(longOpt: 'help', 'Help - Usage Information')


        def options = cli.parse(argv)

        if (!options) {
            System.exit(0)
        }

        if ( options.h ) {
            cli.usage()
            System.exit(0)
        }

        // ugly way to define default value for output
        if( ! options.output ) {
            def argv2 = new ArrayList(Arrays.asList(argv))

            def fileExt = options.xmlOutput ? ".xml" : ".txt"
            def outfile = options.input == '-' ? '-' : options.input.replaceFirst(/\.txt$/, '') + ".tagged" + fileExt
            argv2 << "-o" << outfile

            options = cli.parse(argv2)

            if( ! options.output ) {
                cli.usage()
                System.exit(0)
            }
        }


        if( ! options.quiet ) {
            if( isZheleh(options) ) {
                System.err.println ("Using adjustments for Zhelekhivka")
                if( options.s || options.u || options.z ) {
                    System.err.println ("NOTE: Zhelekhivka adjustments currently do not apply to statistics!")
                }
            }
        }

        return options
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
