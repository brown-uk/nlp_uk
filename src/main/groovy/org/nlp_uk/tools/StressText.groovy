#!/usr/bin/env groovy

package org.nlp_uk.tools

@GrabConfig(systemClassLoader=true)
//@Grab(group='org.languagetool', module='language-uk', version='5.2')
@Grab(group='org.languagetool', module='language-uk', version='5.3-SNAPSHOT')
@Grab(group='ch.qos.logback', module='logback-classic', version='1.2.3')
@Grab(group='org.codehaus.groovy', module='groovy-cli-picocli', version='3.0.7')

import java.util.regex.Pattern

import org.languagetool.*
import org.languagetool.rules.*
import org.languagetool.tagging.uk.IPOSTag
import org.languagetool.tokenizers.*
import org.languagetool.language.*
import org.languagetool.uk.*

import groovy.cli.picocli.CliBuilder
import groovy.lang.Closure
import groovy.lang.Lazy
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.util.Eval


class StressText {
    @groovy.transform.SourceURI
    static SOURCE_URI
    // if this script is called from GroovyScriptEngine SourceURI is data: and does not work for File()
    static SCRIPT_DIR = SOURCE_URI.scheme == "data" 
		? new File("src/main/groovy/org/nlp_uk/tools")
		: new File(SOURCE_URI).parent

    def textUtils = new TextUtils() 
//    def textUtils = System.hasProperty("groovy.grape.enable") 
//		? new TextUtils() 
//		: Eval.me(new File("$SCRIPT_DIR/TextUtils.groovy").text + "\n new TextUtils()")

    JLanguageTool langTool = new MultiThreadedJLanguageTool(new Ukrainian())

    def options
	@Canonical
	class StressInfo { String word; String tags; int base; int offset 
		
		String toString() { word ? String.format("%s %s", word, tags) : String.format("%d %d", base, offset) }
	} 
	
	// plain lemma -> lemma key tag -> list of forms
	Map<String, Map<String, List<StressInfo>>> stresses = new HashMap<>() //.withDefault { new HashMap<>().withDefault{ new ArrayList<>() } }

	static class Stats { 
		int unknownCnt 
		int homonymCnt
		
		void add(Stats stats) { 
			this.unknownCnt += stats.unknownCnt 
			this.homonymCnt += stats.homonymCnt
		}
	}
	
	@Canonical
	static class StressResult {
		String tagged
		Stats stats
	}
	
	Stats stats = new Stats()
	
    StringWriter writer

	StressText() {
		loadStressInfo()
	}
	

    void setOptions(options) {
        this.options = options
    }


	@CompileStatic
	StressResult stressText(String text) {
		assert stresses

		Stats stats = new Stats()
		
		List<AnalyzedSentence> analyzedSentences = langTool.analyzeText(text);

		def sb = new StringBuilder()
		for (AnalyzedSentence analyzedSentence : analyzedSentences) {
			AnalyzedTokenReadings[] tokens = analyzedSentence.getTokens()

			String sentenceLine = outputStressed(tokens, stats)
			
			sb.append(sentenceLine) //.append("\n");
		}

		return new StressResult(sb.toString(), stats)
	}

	@CompileStatic
	boolean isMatch(StressInfo it, String theToken, AnalyzedToken anToken) {
		stripAccent(it.word) == theToken.toLowerCase() //&& it.tags in anToken.getPOSTag()
	}
	

	@CompileStatic
	private String getStressed(String theToken, List<AnalyzedToken> analyzedTokens, Stats stats) {
			
		def words = analyzedTokens.collect { AnalyzedToken anToken -> 
				if( anToken.getPOSTag() == null )
					return	
			
				String keyTag = getTagKey(anToken.getPOSTag())
				println "key: $anToken.lemma $keyTag"

				List<StressInfo> infos = []
				
				if( anToken.lemma in stresses ) {
					 infos = stresses[anToken.lemma][keyTag] ?: infos

					if( ! infos ) {
						if( keyTag.startsWith("verb") ) {
							String genericTag = keyTag.replaceFirst(/:(im)?perf/, ':imperf:perf')
							if( genericTag in stresses[anToken.lemma] ) {
								infos = stresses[anToken.lemma][genericTag]
							}
						}
						else if( keyTag.startsWith("noun") && keyTag.contains(":+") ) {
							// TODO: other genders
							String genericTag = keyTag.replaceFirst(/:[mfn]/, ':m:+n')
							if( genericTag in stresses[anToken.lemma] ) {
								infos = stresses[anToken.lemma][genericTag]
							}
						}

					}

					// get noun lemma from singular
					if( keyTag.startsWith("noun") && keyTag.endsWith(":p") ) {
						for(String s: [":m", ":f", ":n"]) {
							String genderTag = keyTag.replaceFirst(/:p$/, s)
							if( genderTag in stresses[anToken.lemma] ) {
								infos += stresses[anToken.lemma][genderTag]
							}
						}
					}
				}
				
				println "info: $infos"
				if( infos ) {
					// handle /1/ - simple offset
					if( infos.size() == 2 && infos[1].offset ) {
						return applyAccents(theToken, [infos[1].base + infos[1].offset] as int[])
					}
					
					def foundForms = infos.findAll { StressInfo it -> 
												isMatch(it, theToken, anToken) 
											}
											.collect{ 
												def x = stripAccent(it.word) == theToken
													? it.word
													: restoreAccent(it.word, theToken)  // casing is off - need to apply accent
												x
											}

					if( foundForms.size() > 1 ) {
//						System.err.println "Multiple forms found for $theToken"
					}
											
					foundForms = foundForms.unique()

					if( foundForms ) {
						foundForms
					}
					else {
						restoreAccent(infos[0].word, theToken)
					}
				}
				else {
					if( getSyllCount(anToken.lemma) == 1 ) {
						println "single syll lemma: $anToken.lemma"
						applyAccents(theToken, [1] as int[])
					}
					else {
//						stats.unknownCnt++
						theToken
					}
				}
			}
			.flatten()
			.unique()

		println "words: $words"

		int stressCount = words.count { ((String)it).indexOf('\u0301') >= 0 } as int
		
		if( stressCount > 1 ) {
			stats.homonymCnt++
		}
		if( stressCount < words.size() ) {
			stats.unknownCnt++
		}
		words.join("/")
	}
	
	
	private String outputStressed(AnalyzedTokenReadings[] tokens, Stats stats) {
//		println ":: " + tokens
		
		StringBuilder sb = new StringBuilder()
		
		tokens[1..-1].eachWithIndex { AnalyzedTokenReadings tokenReadings, int idx ->
			String theToken = tokenReadings.token
			
			if( tokenReadings.token.length() < 2 
					|| theToken.indexOf('\u0301') >= 1
					|| getSyllCount(theToken) < 2 ) {
				sb.append(theToken);
			}
			else {
				List<AnalyzedToken> analyzedTokens = tokenReadings.getReadings()
					.findAll { AnalyzedToken tr -> 
						tr.getPOSTag() != null && ! tr.getPOSTag().endsWith("_END") && tr.getLemma() 
					}
					
				if( analyzedTokens ) {
					println "lemmas: $analyzedTokens"
					def stressed = getStressed(theToken, analyzedTokens, stats)
					sb.append(stressed)
				}
				else {
					stats.unknownCnt++
					sb.append(theToken)
				}
			}
		}
		
		sb.toString()
	}
	    

    def process() {
        def outputFile = textUtils.processByParagraph(options, { buffer ->
            return stressText(buffer)
        },
		{ StressResult result ->
			stats.add(result.stats) 
		});
    }

	
	@CompileStatic
	static String getTagKey(String tag) {
		tag.replace(':inanim', '') \
			.replaceFirst(/(noun(:(un)?anim)?:[mnfps]|(noun(:(un)?anim)?).*&pron|verb(:perf|:imperf)+|adj(.*?:&adjp)?|[a-z]+).*/, '$1') \
			.replaceFirst(/adj.*?:&adjp/, 'adj:&adjp')
	}
	
	@CompileStatic
	static int getSyllCount(String word) {
		int cnt = 0
		word.getChars().each { char ch ->
			if( isWovel(ch) )
				cnt += 1
		}
		cnt
	}
	
	@CompileStatic
	static boolean isWovel(char ch) {
		"аеєиіїоуюяАЕЄИІЇОУЮЯ".indexOf((int)ch) >= 0
	}
	
	@CompileStatic
	static String stripAccent(String word) {
		word.replace("\u0301", "")
	}
	
	@CompileStatic
	static List<Integer> getAccentSyllIdxs(String word) {
		int syllIdx = 0
		List<Integer> idxs = []
		word.getChars().each { char it ->
			if( it == '\u0301' ) {
				idxs << syllIdx
			}
			else if( "аеєиіїоуюя".indexOf((int)it) >= 0 ) {
				syllIdx += 1
			}
		}
		idxs
	}
	
	@CompileStatic
	static String restoreAccent(String lemma, String word) {
		int[] accents = getAccentSyllIdxs(lemma)
		println "restore for: $lemma: $accents"
		applyAccents(word, accents)
	}

	@CompileStatic
	static String applyAccents(String word, int[] accents) {
		def sb = new StringBuilder()
		int syll = 0
		word.getChars().eachWithIndex { char ch, int idx ->
			sb.append(ch)
			if( isWovel(ch) ) {
				syll += 1
				if( syll in accents ) sb.append('\u0301')
			}
		}
		sb.toString()
	}

	def loadStressInfo() {
		long tm1 = System.currentTimeMillis()
		
		// def base = System.getProperty("user.home") + "/work/ukr/spelling/dict_uk/data/sem"
//		def base = "https://raw.githubusercontent.com/brown-uk/dict_uk/master/data/stress"
		File base
		def stressDir = new File("stress")
		if( stressDir.isDirectory() ) {
			base = stressDir
		}
		else {
			System.err.println("Loading stress info from resource")
//			base = getClass().getResource("/stress")
		}

		System.err.println("Loading stress info from $base")
		["all_stress", "all_stress_prop", "add"].each { file ->
//			def lines = base.startsWith("http")
//				? "$base/${cat}.csv".toURL().getText("UTF-8")
			
			def src = base ? new File(base, file+".txt") : getClass().getResourceAsStream("/stress/${file}.txt")
			def lines = src.getText("UTF-8")

//			println "File: ${file}.txt, lines: ${lines.size()}"
			
			def lastLemmaFull
			def lastLemma
			def lastLemmaTags
			lines.eachLine { line ->
				if( line.indexOf('#') >= 0 )
					line = line.replaceFirst(/\s*#.*/, '')

				// /1/
				String trimmed = line.trim()
				if( trimmed.indexOf(' ') <= 0 && trimmed.startsWith("/") ) {
//					println "x: " + trimmed + " "  + trimmed.charAt(1) + " " + lastLemmaFull
					int offset = trimmed[1] as int
					int[] lemmaAccents = getAccentSyllIdxs(lastLemmaFull) ?: [1]
					stresses[lastLemma][lastLemmaTags] << new StressInfo(base: lemmaAccents[0], offset: offset)
					return
				}
					
				assert trimmed.indexOf(' ') > 0, "Failed at $line" 
					
				def (word, tags) = trimmed.split(' ')
				if( ! line.startsWith(' ') ) {
					lastLemmaFull = word
					lastLemma = stripAccent(word)
					lastLemmaTags = getTagKey(tags)
				}
				
				if( ! (lastLemma in stresses) ) {
					stresses.put(lastLemma, new HashMap<>())
				}
				if( ! (lastLemmaTags in stresses[lastLemma]) ) {
					stresses[lastLemma].put(lastLemmaTags, [])
				}

//				if( lastLemma == "аналізувати" ) println "$lastLemmaTags / $word + $tags" 
				stresses[lastLemma][lastLemmaTags] << new StressInfo(word, tags)
			}
		}

		long tm2 = System.currentTimeMillis()
		System.err.println("Loaded ${stresses.size()} stress forms, ${tm2-tm1}ms")
	}
	

    static parseOptions(String[] argv) {
        def cli = new CliBuilder()

        cli.i(longOpt: 'input', args:1, required: true, 'Input file')
        cli.o(longOpt: 'output', args:1, required: false, 'Output file (default: <input file> - .txt + .stressed.txt)')
        cli._(longOpt: 'singleThread', 'Always use single thread (default is to use multithreading if > 2 cpus are found)')
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

            def fileExt = ".txt"
            def outfile = options.input == '-' ? '-' : options.input.replaceFirst(/\.txt$/, '') + ".stressed" + fileExt
            argv2 << "-o" << outfile

            options = cli.parse(argv2)

            if( ! options.output ) {
                cli.usage()
                System.exit(0)
            }
        }

        return options
    }

	
	
    static void main(String[] args) {

        def nlpUk = new StressText()

        def options = parseOptions(args)

        nlpUk.setOptions(options)

        nlpUk.process()
    }

}
