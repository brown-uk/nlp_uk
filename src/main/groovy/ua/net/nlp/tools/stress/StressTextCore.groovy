package ua.net.nlp.tools.stress

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import picocli.CommandLine
import picocli.CommandLine.Option
import picocli.CommandLine.ParameterException
import ua.net.nlp.tools.OptionsBase
import ua.net.nlp.tools.OutputFormat
import ua.net.nlp.tools.TextUtils
import ua.net.nlp.tools.TextUtils.ResultBase
import ua.net.nlp.tools.stress.StressTextCore
import ua.net.nlp.tools.tag.TTR
import ua.net.nlp.tools.tag.TagOptions
import ua.net.nlp.tools.tag.TagTextCore
import ua.net.nlp.tools.tag.TagTextCore.TaggedSentence
import ua.net.nlp.tools.tag.TagTextCore.TaggedToken


class StressTextCore {
    def textUtils = new TextUtils() 
    TagTextCore tagText = new TagTextCore()
    StressOptions options
    
	@Canonical
	static class StressInfo {
        String word
        String tags
        int base
        int offset 
		
		String toString() { word ? String.format("%s %s", word, tags) : String.format("%d %d", base, offset) }
	} 
	
	// plain lemma -> lemma key tag -> list of forms
	Map<String, Map<String, List<StressInfo>>> stresses = new HashMap<>() //.withDefault { new HashMap<>().withDefault{ new ArrayList<>() } }

	static class Stats { 
		Map<String, Integer> unknown = [:].withDefault{ 0 } 
		int homonymCnt
		
		void add(Stats stats) { 
			this.unknown += stats.unknown 
			this.homonymCnt += stats.homonymCnt
		}
	}
	
	@Canonical
	static class StressResult extends ResultBase {
        StressResult(String text, Stats stats) {
            super(text)
            this.stats = stats
        }
        
		Stats stats
	}
	
	Stats stats = new Stats()
	
    StringWriter writer

	StressTextCore() {
		loadStressInfo()
	}
	

    void setOptions(options) {
        this.options = options

        def tagOptions = new TagOptions()
        tagOptions.disambiguate = options.disambiguate
        tagText.setOptions(tagOptions)
    }


	@CompileStatic
	StressResult stressText(String text) {
		assert stresses

		Stats stats = new Stats()
        
		List<TaggedSentence> taggedSentences = tagText.tagTextCore(text, null)

		def sb = new StringBuilder((int)(text.size() * 1.10))
        int ii = 0;
		for (TaggedSentence analyzedSentence : taggedSentences) {
            if( ! sb.isEmpty() && ii < taggedSentences.size() - 1) {
                sb.append(' ')
            }
            ++ii;
            
			List<TTR> tokens = analyzedSentence.tokens

			String sentenceLine = outputStressed(tokens, stats)
			
			sb.append(sentenceLine) //.append("\n");
		}

		return new StressResult(sb.toString(), stats)
	}

	@CompileStatic
	static boolean isMatch(StressInfo it, String theToken, TaggedToken anToken) {
        String normalizedWord = anToken.tags.contains(':prop') ? theToken : theToken.toLowerCase() 
		if( stripAccent(it.word) != normalizedWord )
            return false

        // derivative forms
        if( it.tags.startsWith(':') ) {
            String normTag = anToken.tags
            if( ! normTag.contains(it.tags) )
                return false
        }
                        
        return true
	}
	

	@CompileStatic
	private String getStressed(String theToken, List<TaggedToken> analyzedTokens, Stats stats) {
			
        int idx = -1
		def words = analyzedTokens.collect { TaggedToken anToken ->
            ++idx
			
			String keyTag = getTagKey(anToken.tags)
			def tokenLemma = anToken.lemma
			println "key: $tokenLemma $keyTag"
			int stressOffset = 0

			if( tokenLemma =~ /^((що)?якнай|щонай).*(ий|е)$/ ) {
				tokenLemma = tokenLemma.replaceFirst(/^((що)?якнай|щонай)/, '')
				stressOffset += 2
				if( tokenLemma.startsWith("щоякнай") ) {
					stressOffset += 1
				}
			}
				
			List<StressInfo> infos = []
			
            def inf = stresses[tokenLemma]
			if( inf ) {
				 infos = inf[keyTag] ?: infos

				if( ! infos ) {
					if( keyTag.startsWith("verb") ) {
						String genericTag = keyTag.replaceFirst(/:(im)?perf/, ':imperf:perf')
						if( genericTag in stresses[tokenLemma] ) {
							infos = stresses[tokenLemma][genericTag]
						}
					}
					else if( keyTag.startsWith("noun") && keyTag.contains(":+") ) {
						// TODO: other genders
						String genericTag = keyTag.replaceFirst(/:[mfn]/, ':m:+n')
						if( genericTag in stresses[tokenLemma] ) {
							infos = stresses[tokenLemma][genericTag]
						}
					}
				}

				// get noun lemma from singular
				if( keyTag.startsWith("noun") && keyTag.endsWith(":p") ) {
					for(String s: [":m", ":f", ":n"]) {
						String genderTag = keyTag.replaceFirst(/:p$/, s)
						if( genderTag in stresses[tokenLemma] ) {
							infos += stresses[tokenLemma][genderTag]
						}
					}
				}
			}
			else if( anToken.tags.startsWith("adv:comp") ) {
				// if we have докладніше adj:n:comp skip unknown adv:comp
				if( analyzedTokens.any { it.tags && it.tags.startsWith("adj:n:v_naz:comp") } )
					return
			}

			println "info: $infos"
			if( infos ) {
				// handle /1/ - simple offset
				if( infos.size() == 2 && infos[1].offset ) {
					return applyAccents(theToken, [infos[1].base + infos[1].offset])
				}
				
				def foundForms = infos
                    .findAll { StressInfo it -> 
						isMatch(it, theToken, anToken) 
                            && contextMatch(it, theToken, anToken, idx, analyzedTokens)
					}
					.collect{ 
						def x = stripAccent(it.word) == theToken
							? it.word
							: restoreAccent(it.word, theToken, 0)  // casing is off - need to apply accent
						x
					}

				foundForms = foundForms.unique()

				if( foundForms ) {
					foundForms
				}
				else {
					restoreAccent(infos[0].word, theToken, stressOffset)
				}
			}
			else {
				if( getSyllCount(tokenLemma) == 1 ) {
					println "single syll lemma: $tokenLemma"
					applyAccents(theToken, [1])
				}
				else {
//						stats.unknownCnt++
					theToken
				}
			}
		}
		.flatten()
		.findAll{ it }
		.unique()

		println "words: $words"

		int stressCount = words.count { ((String)it).indexOf('\u0301') >= 0 } as int
		
		if( stressCount > 1 ) {
			stats.homonymCnt++
		}
		if( words.join("/").indexOf('\u0301') == -1 ) {
			stats.unknown[theToken] += 1
		}
		words.join("/")
	}
	
    static boolean contextMatch(StressInfo it, String theToken, TaggedToken anToken, int idx, List<TaggedToken> analyzedTokens) {
        if( it.word == "чо́гось" && it.tags == ":v_rod" ) {
            return idx >= 1 && analyzedTokens[idx-1].value.equalsIgnoreCase("до")
        }
        return true
    }
    
    
	private String outputStressed(List<TTR> tokens, Stats stats) {
//		println ":: " + tokens
		
		StringBuilder sb = new StringBuilder()
		
		tokens.eachWithIndex { TTR wordToken, int idx ->
			String theToken = wordToken.tokens[0].value

            if( ! sb.isEmpty()
                    && (wordToken.tokens[0].whitespaceBefore == null || wordToken.tokens[0].whitespaceBefore == true) ) {
                sb.append(' ')
            }
                
			if( theToken.indexOf('\u0301') >= 1
					|| getSyllCount(theToken) < 2 ) {
				sb.append(theToken);
			}
			else {
				List<TaggedToken> analyzedTokens = wordToken.tokens
					.findAll { TaggedToken tr -> 
						tr.lemma 
                    }

				if( analyzedTokens ) {
					println "lemmas: $analyzedTokens"
					def stressed = getStressed(theToken, analyzedTokens, stats)
					sb.append(stressed)
				}
				else {
					stats.unknown[theToken] += 1
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
        if( tag.contains('lname') )
            return 'lname'
        
		tag.replace(':inanim', '') \
			.replaceFirst(/(noun(:(un)?anim)?:[mnfps]|(noun(:(un)?anim)?).*pron|verb(:perf|:imperf)+|adj(.*?:adjp)?|[a-z]+).*/, '$1') \
			.replaceFirst(/adj.*?:adjp/, 'adj:adjp')
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
			else if( "аеєиіїоуюяАЕЄИІЇОУЮЯ".indexOf((int)it) >= 0 ) {
				syllIdx += 1
			}
		}
		idxs
	}
	
	@CompileStatic
	static String restoreAccent(String lemma, String word, int offset) {
        List<Integer> accents = getAccentSyllIdxs(lemma)
		if( offset ) {
			accents.eachWithIndex{ int a, int i -> accents[i]+=offset }
		}
		println "restore for: $lemma: $accents"
		applyAccents(word, accents)
	}

	@CompileStatic
	static String applyAccents(String word, List<Integer> accents) {
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

    @CompileStatic
    static class StressOptions extends OptionsBase {
//        @Parameters(index = "0", description = "Input files. Default: stdin", arity="0..")
//        List<String> inputFiles
        @Option(names = ["--singleThread"], description = "Always use single thread (default is to use multithreading if > 2 cpus are found)")
        boolean singleThread
        @Option(names = ["-g", "--disambiguate"], description = "Disambiguate first.", defaultValue = "true")
        boolean disambiguate = true
    
    }
    	

    static parseOptions(String[] argv) {

        StressOptions options = new StressOptions()
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

        if( ! options.output ) {
            def fileExt = ".txt"
            def outfile = options.input == '-' ? '-' : options.input.replaceFirst(/\.txt$/, '') + ".stressed" + fileExt
            options.output = outfile
        }
        options.outputFormat = OutputFormat.txt

        return options
    }

	
	
    static void main(String[] args) {

        def nlpUk = new StressTextCore()

        def options = parseOptions(args)

        nlpUk.setOptions(options)

        nlpUk.process()
    }

}
