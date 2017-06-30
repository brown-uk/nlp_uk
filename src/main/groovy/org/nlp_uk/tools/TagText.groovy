#!/usr/bin/env groovy

package org.nlp_uk.tools

@Grab(group='org.languagetool', module='language-uk', version='3.8')
@Grab(group='commons-cli', module='commons-cli', version='1.3')

import org.languagetool.*
import org.languagetool.rules.*
import org.languagetool.tokenizers.*
import org.languagetool.language.*
import org.languagetool.uk.*
import groovy.lang.Closure


class TagText {
	JLanguageTool langTool = new MultiThreadedJLanguageTool(new Ukrainian());
	def options
	def homonymMap = [:]
	
	TagText(options) {
		this.options = options
	}


	def tagText(String text) {
		List<AnalyzedSentence> analyzedSentences = langTool.analyzeText(text);

		def sb = new StringBuilder()
		for (AnalyzedSentence analyzedSentence : analyzedSentences) {
		    def sentenceLine = analyzedSentence.toString()
		    sentenceLine = sentenceLine.replaceAll(/(<S>|\]) */, '$0\n')
			sb.append(sentenceLine).append("\n");
		}
		
		if( options.homonymStats ) {
			collectHomonyms(analyzedSentences)
		}
		
		return sb.toString()
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
	
					def homonim = readings.getToken() + "\t" + readings.join("|")
					int cnt = homonymMap.get(homonim, 0)
					homonymMap.put(homonim, cnt+1)
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
			def outputFile = new File(options.output.replace(/\.txt/, '') + '.stats.txt')
			printStream = new PrintStream(outputFile)
		}

		printStream.println("Час-та\tОм.\tЛем\tСлово\tОмоніми")
		
		homonymMap
		.sort { -it.value }
		.each{ k, v ->
			def items = k.split("\t")[1].split("\\|")
			def homonimCount = items.size()
			def posHomonimCount = items.collect { it.split(":", 2)[0] }.unique().size()
			
			def str = String.sprintf("%6d\t%d\t%d\t%s", v, homonimCount, posHomonimCount, k.replace("\t", "\t\t"))
			
			printStream.println(str)
		}
	}

	static void main(String[] argv) {

		def cli = new CliBuilder()

		cli.i(longOpt: 'input', args:1, required: true, 'Input file')
		cli.o(longOpt: 'output', args:1, required: false, 'Output file (default: <input file> - .txt + .tagged.txt)')
		cli.l(longOpt: 'tokenPerLine', '1 token per line')
		cli.d(longOpt: 'disableDisamgigRules', args:1, 'Comma-separated list of ids of disambigation rules to disable')
		cli.s(longOpt: 'homonymStats', 'Collect homohym statistics')
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

		def inputFile = options.input == "-" ? System.in : new File(options.input)


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

		return outputFile
	}

}
