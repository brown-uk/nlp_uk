#!/usr/bin/env groovy

package org.nlp_uk.tools

@Grab(group='org.languagetool', module='language-uk', version='3.5')
@Grab(group='commons-cli', module='commons-cli', version='1.3')

import java.net.*
import org.languagetool.*
import org.languagetool.rules.*
import org.languagetool.tokenizers.*
import org.languagetool.language.*
import org.languagetool.uk.*
import groovy.lang.Closure


class TagText {
	JLanguageTool langTool = new MultiThreadedJLanguageTool(new Ukrainian());
	def options
	
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
		return sb.toString()
	}



	static void main(String[] argv) {

		def cli = new CliBuilder()

		cli.i(longOpt: 'input', args:1, required: true, 'Input file')
		cli.o(longOpt: 'output', args:1, required: true, 'Output file')
		cli.l(longOpt: 'tokenPerLine', '1 token per line')
		cli.d(longOpt: 'disabledRules', args:1, 'Comma-separated list of ids of disambigation rules to disable')
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

		processByParagraph(options, { buffer ->
			return nlpUk.tagText(buffer)
		});

	}


	static int MAX_PARAGRAPH_SIZE =  200*1024;
	static void processByParagraph(options, Closure closure) {
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

	}

}
