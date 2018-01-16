#!/usr/bin/env groovy

package org.nlp_uk.tools

@Grab(group='org.languagetool', module='language-uk', version='4.0')
@Grab(group='commons-cli', module='commons-cli', version='1.3')

import org.languagetool.*
import org.languagetool.rules.*
import org.languagetool.tagging.uk.IPOSTag
import org.languagetool.tokenizers.*
import org.languagetool.language.*
import org.languagetool.uk.*
import groovy.lang.Closure
import groovy.xml.MarkupBuilder


class TagText {
	JLanguageTool langTool = new MultiThreadedJLanguageTool(new Ukrainian());
	def options
	def homonymFreqMap = [:].withDefault { 0 }
	def homonymTokenMap = [:].withDefault{ new HashSet<>() }
	def unknownMap = [:].withDefault { 0 }
	def frequencyMap = [:].withDefault { 0 }
	def lemmaFrequencyMap = [:].withDefault { 0 }

	StringWriter writer
	MarkupBuilder xml
	
	TagText(options) {
		this.options = options
		
		if( options.xmlOutput ) {
			writer = new StringWriter()
			xml = new MarkupBuilder(writer)
		}
	}


	def tagText(String text) {
		List<AnalyzedSentence> analyzedSentences = langTool.analyzeText(text);

		def sb = new StringBuilder()
		for (AnalyzedSentence analyzedSentence : analyzedSentences) {
			if( options.xmlOutput ) {
				xml.'sentence'() {
					analyzedSentence.getTokensWithoutWhitespace()[1..<-1].each { AnalyzedTokenReadings tokenReadings ->
						'tokenReading'() {
							tokenReadings.getReadings().each { AnalyzedToken tkn ->
								if( tkn.getPOSTag() in [JLanguageTool.SENTENCE_END_TAGNAME, JLanguageTool.SENTENCE_START_TAGNAME] )
									return
									
								if( tkn.getToken() ==~ /\p{Punct}/ ) {
									'token'('value': tkn.getToken())
								}
								else {
									'token'('value': tkn.getToken(), 'lemma': tkn.getLemma(), 'tags': tkn.getPOSTag())
								}
							}
						}
					}
				}
				sb.append(writer.toString()).append("\n");
				writer.getBuffer().setLength(0)
			}
			else if ( ! options.noTag ) {
				def sentenceLine = analyzedSentence.toString()
				sentenceLine = sentenceLine.replaceAll(/(<S>|\]) */, '$0\n')
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

	def collectUnknown(List<AnalyzedSentence> analyzedSentences) {
   		for (AnalyzedSentence analyzedSentence : analyzedSentences) {
		    analyzedSentence.getTokensWithoutWhitespace()[1..<-1].each { AnalyzedTokenReadings tokenReadings ->
		        if( tokenReadings.getAnalyzedToken(0).getPOSTag() == null 
		                && tokenReadings.getToken() =~ /[а-яіїєґА-ЯІЇЄҐ]/ 
		                && ! (tokenReadings.getToken() =~ /[ыэъё]|[а-яіїєґА-ЯІЇЄҐ]'?[a-zA-Z]|[a-zA-Z][а-яіїєґА-ЯІЇЄҐ]/) ) {
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
	
					def key = readings.join("|")
					homonymFreqMap[key] += 1
					homonymTokenMap[key] << readings.getToken()
				}
			}
		}
	}
	

	def collectFrequency(List<AnalyzedSentence> analyzedSentences) {
   		for (AnalyzedSentence analyzedSentence : analyzedSentences) {
		    analyzedSentence.getTokensWithoutWhitespace()[1..<-1].each { AnalyzedTokenReadings tokenReadings ->
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
		    analyzedSentence.getTokensWithoutWhitespace()[1..<-1].each { AnalyzedTokenReadings tokenReadings ->
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
			def outputFile = new File(options.output.replaceFirst(/\.txt$/, '') + '.homonym.txt')
			printStream = new PrintStream(outputFile)
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
			
			def str = String.sprintf("%6d\t%d\t%d\t%s\t\t%s", v, homonimCount, posHomonimCount, homonymTokenMap[k].join(","), k)
			
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
			def outputFile = new File(options.output.replaceFirst(/\.txt$/, '') + '.unknown.txt')
			printStream = new PrintStream(outputFile)
		}

		unknownMap
		.sort { it.key }
		.each{ k, v ->
			def str = String.sprintf("%6d\t%s", v, k)
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
			def outputFile = new File(options.output.replaceFirst(/\.txt$/, '') + '.freq.txt')
			printStream = new PrintStream(outputFile)
		}

		frequencyMap
		.sort { it.key }
		.each{ k, v ->
			def str = String.sprintf("%6d\t%s", v, k)
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
			def outputFile = new File(options.output.replaceFirst(/\.txt$/, '') + '.lemma.freq.txt')
			printStream = new PrintStream(outputFile)
		}

		lemmaFrequencyMap
		.sort { it.key }
		.each{ k, v ->
			def str = String.sprintf("%6d\t%s", v, k)
			printStream.println(str)
		}
	}

	static void main(String[] argv) {

		def cli = new CliBuilder()

		cli.i(longOpt: 'input', args:1, required: true, 'Input file')
		cli.o(longOpt: 'output', args:1, required: false, 'Output file (default: <input file> - .txt + .tagged.txt)')
		cli.l(longOpt: 'tokenPerLine', '1 token per line')
		cli.x(longOpt: 'xmlOutput', 'output in xml format')
		cli.d(longOpt: 'disableDisamgigRules', args:1, 'Comma-separated list of ids of disambigation rules to disable')
		cli.s(longOpt: 'homonymStats', 'Collect homohym statistics')
		cli.u(longOpt: 'unknownStats', 'Collect unknown words statistics')
		cli.w(longOpt: 'frequencyStats', 'Collect word frequency')
		cli.z(longOpt: 'lemmaStats', 'Collect lemma frequency')
		cli.k(longOpt: 'noTag', 'Do not write tagged text (only perform stats)')
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

            def outfile = options.input == '-' ? '-' : options.input.replaceFirst(/\.txt$/, '') + ".tagged.txt"
            argv2 << "-o" << outfile

            options = cli.parse(argv2)
        }

		def nlpUk = new TagText(options)
		
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

		def outputFile = processByParagraph(options, { buffer ->
			return nlpUk.tagText(buffer)
		});

	
		if( options.homonymStats ) {
			nlpUk.printHomonymStats()
		}
		if( options.unknownStats ) {
			nlpUk.printUnknownStats()
		}
		if( options.frequencyStats ) {
			nlpUk.printFrequencyStats()
		}
		if( options.lemmaStats ) {
			nlpUk.printLemmaFrequencyStats()
		}
	}


	static int MAX_PARAGRAPH_SIZE =  200*1024;
	static def processByParagraph(options, Closure closure) {
		def outputFile
		if( options.output == "-" ) {
			outputFile = System.out
		}
		else {
			outputFile = new File(options.output)
			outputFile.setText('')	// to clear out output file
			outputFile = new PrintStream(outputFile)
		}
		
		if( ! options.quiet && options.input == "-" ) {
			System.err.println ("reading from stdin...")
		}

		if( ! options.quiet && options.xmlOutput ) {
			System.err.println ("writing into xml format")
		}
		
		def inputFile = options.input == "-" ? System.in : new File(options.input)

		if( options.xmlOutput ) {
			outputFile.println('<?xml version="1.0" encoding="UTF-8"?>')
			outputFile.println('<text>\n')
		}
		
		def buffer = new StringBuilder()
		inputFile.eachLine('UTF-8', 0, { line ->
			buffer.append(line).append("\n")

			def str = buffer.toString()
			if( str.endsWith("\n\n") && str.trim().length() > 0
			        || buffer.length() > MAX_PARAGRAPH_SIZE ) {

				def analyzed = closure(str)
				outputFile.print(analyzed)

				buffer = new StringBuilder()
			}
		})
		
		if( buffer ) {
			def analyzed = closure(buffer.toString())
			outputFile.print(analyzed)
		}

		outputFile.println('\n</text>\n')
		
		return outputFile
	}

}
