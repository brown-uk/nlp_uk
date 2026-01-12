package ua.net.nlp.tools.stress

import groovy.transform.Canonical
import groovy.transform.CompileDynamic
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


@CompileStatic
class StressTextCore {
    def textUtils = new TextUtils() 
    TagTextCore tagText = new TagTextCore()
    StressOptions options
	// plain lemma -> lemma key tag -> list of forms
	Map<String, Map<String, List<StressInfo>>> stresses
    Stats stats = new Stats()
    StringWriter writer

	
	@Canonical
	static class StressResult extends ResultBase {
        StressResult(String text, Stats stats) {
            super(text)
            this.stats = stats
        }
        
		public Stats stats
	}
	
	StressTextCore() {
		stresses = StressInfo.loadStressInfo()
	}
	

    void setOptions(StressOptions options) {
        this.options = options

        def tagOptions = new TagOptions()
        tagOptions.disambiguate = options.disambiguate
        tagOptions.tagUnknown = false
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
            if( ! tokens ) {
                continue
            }

			String sentenceLine = outputStressed(tokens, stats)
			
			sb.append(sentenceLine) //.append("\n");
		}

		return new StressResult(sb.toString(), stats)
	}

	@CompileStatic
	static boolean isMatch(StressInfo it, String theToken, TaggedToken anToken) {
        def tokenTags = anToken.tags
        String normalizedWord = tokenTags.contains(':prop') ? theToken : theToken.toLowerCase() 
		if( Util.stripAccent(it.word) != normalizedWord )
            return false

        def stressTags = it.tags
            
        // derivative forms
        if( stressTags.startsWith(':') ) {
            if( anToken.tags.startsWith('noun:anim:p:v_zna:rare') ) {
                tokenTags = tokenTags.replace('noun:anim:p:v_zna:rare', 'noun:anim:p:v_naz')
            }
            else if( anToken.tags.startsWith('noun:inanim:m:v_zna:var') ) {
                tokenTags = tokenTags.replace('noun:inanim:m:v_zna:var', 'noun:inanim:m:v_rod')
            }
            
            if( ! tokenTags.contains(stressTags) )
                return false
        }
                        
        return true
	}
	

	@CompileStatic
	private String getStressed(String theToken, List<TaggedToken> analyzedTokens, Stats stats, List<TTR> sentenceTokens, int idx) {
			
		def words = analyzedTokens.collect { TaggedToken anToken ->
			
			String keyTag = Util.getTagKey(anToken.tags)
			def tokenLemma = anToken.lemma
			println "key: $tokenLemma $keyTag"
			int stressOffset = 0

			if( tokenLemma =~ /(?iu)^((що)?якнай|щонай).*(ий|е)$/ ) {
				tokenLemma = tokenLemma.replaceFirst(/(?iu)^((що)?якнай|щонай)/, '')
				stressOffset += 2
				if( tokenLemma.toLowerCase().startsWith("щоякнай") ) {
					stressOffset += 1
				}
			}
            
            if( tokenLemma == 'сам' ) {
                tokenLemma = 'сами́й'
            }
			
            if( tokenLemma in StressInfo.lemmasWithXp ) {
                tokenLemma = StressInfo.adjustLemma(tokenLemma, anToken.tags)
            }
            
            def infByLemma = stresses[tokenLemma]
            def normalizedLemma = tokenLemma
            
            // try alt
            if( ! infByLemma ) {

                if( keyTag.startsWith("ad") && anToken.tags.contains(':comp') ) {
                    def normalizedLemma_ = normalizedLemma.replaceFirst(/^най/, '')
                    if( stresses[normalizedLemma_] ) {
                        normalizedLemma = normalizedLemma_
                        infByLemma = stresses[normalizedLemma]
                        stressOffset += 1
                    }
                    else if( keyTag.startsWith("adv") && anToken.tags.contains(':compc') ) {
                        normalizedLemma_ = normalizedLemma.replaceFirst(/е$/, 'ий')
                        def normalizedTag = 'adj'
                        def _info = stresses[normalizedLemma_]
                        if( _info && _info[normalizedTag] ) {
                            normalizedLemma = normalizedLemma_
                            keyTag = normalizedTag
                            infByLemma = _info
                        }
                    }
                    else {
                        normalizedLemma_ = normalizedLemma.replaceFirst(/іш(ий)/, '$1')
                        if( stresses[normalizedLemma_] ) {
                            normalizedLemma = normalizedLemma_
                            infByLemma = stresses[normalizedLemma]
                            if( normalizedLemma.startsWith('най') ) {
                                stressOffset += 1
                            }
                        }
                    }
                }

                if( anToken.tags =~ /:alt|:up19/ ) {
                    if( tokenLemma.toLowerCase().contains('ґ') ) {
                        normalizedLemma = tokenLemma.replace('Ґ', 'Г').replace('ґ', 'г')
                        infByLemma = stresses[normalizedLemma]
                    }
                    else if( tokenLemma.toLowerCase().contains('проєкт') ) {
                        normalizedLemma = tokenLemma.replace('роєкт', 'роект')
                        infByLemma = stresses[normalizedLemma]
                    }
                }

                if( anToken.tags.startsWith('verb:rev') && tokenLemma.endsWith('ся') ) {
                    normalizedLemma = tokenLemma[0..-3]
                    infByLemma = stresses[normalizedLemma]
                }
            }
            
            List<StressInfo> infos = []
			if( infByLemma ) {
				infos = infByLemma[keyTag] ?: infos

				if( ! infos ) {
					if( keyTag.startsWith("verb") ) {
						String genericTag = keyTag.replaceFirst(/:(im)?perf/, ':imperf:perf')
                        infos = stresses[normalizedLemma][genericTag]
					}
//					else if( keyTag.startsWith("noun") && keyTag.contains(":+") ) {
//						// TODO: other genders
//						String genericTag = keyTag.replaceFirst(/:[mfn]/, ':m:+n')
//                        infos = stresses[normalizedLemma][genericTag]
//					}
				}

				// get noun lemma from singular
				if( keyTag.startsWith("noun") ) {
                    if( keyTag.endsWith(":p") ){
        				for(String s: [":m", ":f", ":n"]) {
        					String genderTag = keyTag.replaceFirst(/:p$/, s)
                            def info_ = stresses[normalizedLemma][genderTag]
        					if( info_ ) {
        						infos += info_
        					}
        				}
                    }
				}
			}
//			else if( anToken.tags.startsWith("adv:comp") ) {
//				// if we have докладніше adj:n:comp skip unknown adv:comp
//				if( analyzedTokens.any { it.tags && it.tags.startsWith("adj:n:v_naz:comp") } )
//					return
//			}

			println "info: $infos"
			if( infos ) {
				// handle /1/ - simple offset
				if( infos.size() == 2 && infos[1].offset ) {
					return Util.applyAccents(theToken, [infos[1].base + infos[1].offset])
				}
				
				def foundForms = infos
                    .findAll { StressInfo it -> 
						isMatch(it, theToken, anToken) 
                            && isContextMatch(it, theToken, anToken, sentenceTokens, idx)
					}
					.collect{ 
						def x = Util.stripAccent(it.word) == theToken
							? it.word
							: Util.restoreAccent(it.word, theToken, 0)  // casing is off - need to apply accent
						x
					}

				foundForms = foundForms.unique()

				if( foundForms ) {
					foundForms
				}
				else {
					Util.restoreAccent(infos[0].word, theToken, stressOffset)
				}
			}
			else {
				if( Util.getSyllCount(tokenLemma) == 1 ) {
					println "single syll lemma: $tokenLemma"
					Util.applyAccents(theToken, [1])
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

		println "stressed: $words"

		int stressCount = words.count { ((String)it).indexOf('\u0301') >= 0 } as int

        String joined = words.join("/")
        
		if( stressCount > 1 ) {
			stats.addHomonyms(joined, analyzedTokens.collect { it.tags })
		}
        
        return joined
	}
    
	
    static boolean isContextMatch(StressInfo it, String theToken, TaggedToken anToken, List<TTR> sentenceTokens, int idx) {
        if( it.comment && it.comment.startsWith('<') ) {
            def precond = it.comment.split('< ')[1]
            if( idx < 1 ) return false
            
            println "precond: $precond -- $idx" //  -- ${it.comment}"
            if( precond =~ /[a-z]/ )
                return sentenceTokens[idx-1].tokens[0].tags ==~ precond
            else
                return sentenceTokens[idx-1].tokens[0].lemma ==~ precond
        }
        return true
    }
    
    
	private String outputStressed(List<TTR> sentenceTokens, Stats stats) {
		StringBuilder sb = new StringBuilder()
		
		sentenceTokens.eachWithIndex { TTR wordToken, int idx ->
			String theToken = wordToken.tokens[0].value

            if( ! sb.isEmpty()
                    && (wordToken.tokens[0].whitespaceBefore == null || wordToken.tokens[0].whitespaceBefore == true)
                    && (idx == 0 || ! (sentenceTokens[idx-1].tokens[0].value.matches(/[«(„]/))) ) {
                sb.append(' ')
            }
                
			if( theToken.indexOf('\u0301') >= 1
					|| Util.getSyllCount(theToken) < 2 ) {
				sb.append(theToken);
			}
			else {
				List<TaggedToken> analyzedTokens = wordToken.tokens
					.findAll { TaggedToken tr -> 
						tr.lemma 
                    }

				if( analyzedTokens ) {
					println "lemmas: $analyzedTokens"
					String stressed = getStressed(theToken, analyzedTokens, stats, sentenceTokens, idx)
                    
                    // фізик-ядерник
                    if( ! stressed.contains("\u0301") && stressed =~ /[-\u2013]/ ) {
                        
                        def m = ~/(?iu)([а-яіїєґ']{3,})([-\u2013])([а-яіїєґ']{3,})/
                        def match = m.matcher(theToken)
                        if( match.matches() ) {
                            List<TaggedToken> nouns = analyzedTokens.findAll { TaggedToken tr -> tr.tags.startsWith('noun') }
                            if( nouns ) {
                                def w1 = match.group(1)
                                def w2 = match.group(3)
                                
                                List<TaggedSentence> taggedSentences1 = tagText.tagTextCore(w1, null)
                                TTR token1 = taggedSentences1[0].tokens[0] //.tokens.findAll{ it.tags =~ /noun/ }
                                
                                List<TaggedSentence> taggedSentences2 = tagText.tagTextCore(w2, null)
                                TTR token2 = taggedSentences2[0].tokens[0] //.tokens.findAll{ it.tags =~ /noun/ }

                                String sentenceLine = outputStressed([token1, token2], stats)
                                if( sentenceLine.contains("\u0301") ) {
                                    stressed = sentenceLine.replace(' ', match.group(2))
                                    println "compound: $stressed"
                                }
                            }
                        }
                        else {
                            def m2 = ~/(?iu)([0-9]+)([-\u2013])([а-яіїєґ']{3,})/
                            def match2 = m2.matcher(theToken)
                            if( match2.matches() ) {
                                List<TaggedToken> adjs = analyzedTokens.findAll { TaggedToken tr -> tr.tags =~ /adj|noun/ }
                                if( adjs ) {
                                    def w1 = match2.group(1)
                                    def w2 = match2.group(3)
                                    def hyphen = match2.group(2)
                                    
                                    if( w2 ==~ /річчя|ліття/ ) {
                                        w2 = w2.replace('річч', 'рі\u0301чч')
                                        w2 = w2.replace('літт', 'лі\u0301тт')
                                        stressed = "$w1$hyphen$w2"
                                    }
                                    else {
                                        List<TaggedSentence> taggedSentences2 = tagText.tagTextCore(w2, null)
                                        TTR token2 = taggedSentences2[0].tokens[0] //.tokens.findAll{ it.tags =~ /noun/ }
                                        def sentenceLine = outputStressed([token2], stats)
                                        if( sentenceLine.contains("\u0301") ) {
                                            stressed = "$w1$hyphen$sentenceLine"
                                            println "compound: $stressed"
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if( stressed.indexOf('\u0301') == -1 ) {
                        stats.addUnknown(theToken, analyzedTokens.collect { it.tags })
                    }
            
					sb.append(stressed)
				}
				else {
                    stats.addUnknown(theToken, null)
					sb.append(theToken)
				}
			}
		}
		
		sb.toString()
	}
	    

    @CompileDynamic
    def process() {
        def outputFile = textUtils.processByParagraph(options, { String buffer ->
            return stressText(buffer)
        },
		{ StressResult result ->
			stats.add(result.stats) 
		});
    
        if( options.unknownStats ) {
            def filename = options.input ? options.input.replaceFirst(/\.txt$/, '')  : "stdout"
            stats.collectStats(filename)
        }
    }


    @CompileStatic
    static class StressOptions extends OptionsBase {
//        @Parameters(index = "0", description = "Input files. Default: stdin", arity="0..")
//        List<String> inputFiles
        @Option(names = ["--singleThread"], description = "Always use single thread (default is to use multithreading if > 2 cpus are found)")
        boolean singleThread
        @Option(names = ["-g", "--disambiguate"], description = "Disambiguate first.", defaultValue = "true")
        boolean disambiguate = true
        @Option(names = ["-su", "--unknownStats"], description = "Collect statistics for words with no stressing info")
        boolean unknownStats
    }
    	

    static StressOptions parseOptions(String[] argv) {

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
