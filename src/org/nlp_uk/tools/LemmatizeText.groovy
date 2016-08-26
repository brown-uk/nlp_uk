#!/usr/bin/env groovy

package org.nlp_uk.tools

@Grab(group='org.languagetool', module='language-uk', version='3.4')
@Grab(group='commons-cli', module='commons-cli', version='1.3')

import java.net.*
import org.languagetool.*
import org.languagetool.rules.*
import org.languagetool.tokenizers.*
import org.languagetool.language.*
import org.languagetool.uk.*
import groovy.lang.Closure


class LemmatizeText {
	JLanguageTool langTool = new MultiThreadedJLanguageTool(new Ukrainian());

	def options
	
	LemmatizeText(options) {
		this.options = options
	}

	def analyzeText(String text) {
		List<AnalyzedSentence> analyzedSentences = langTool.analyzeText(text);

		def sb = new StringBuilder()
		for (AnalyzedSentence analyzedSentence : analyzedSentences) {
			analyzedSentence.getTokens().each { AnalyzedTokenReadings readings->
			    if( readings.isWhitespace() || readings.getAnalyzedToken(0).lemma == null ) {
			        sb.append(readings.token)
			    }
			    else {
			        def lemmas = options.firstLemma ? readings[0].getLemma() : readings*.lemma.unique().join("|")
			        sb.append(lemmas)
			    }
			}
		}
		return sb.toString()
	}


	static void main(String[] argv) {

		def cli = new CliBuilder()

		cli.i(longOpt: 'input', args:1, required: true, 'Input file')
		cli.o(longOpt: 'output', args:1, required: true, 'Output file')
		cli.f(longOpt: 'firstLemma', 'Pick first lemma for homonyms')
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


		def nlpUk = new LemmatizeText(options)

		processByParagraph(options, { buffer->
			return nlpUk.getAnalyzed(buffer)
		})
	}

	private getAnalyzed(String textToAnalyze) {
	    return analyzeText(textToAnalyze)
	}

	
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


		def buffer = ""
		inputFile.eachLine('UTF-8', 0, { line ->
			buffer += line + "\n"

			if( buffer.endsWith("\n\n") ) {
				def analyzed = closure(buffer)
				outputFile.print(analyzed)
				buffer = ""
			}
		})
		
		if( buffer ) {
			def analyzed = closure(buffer)
			outputFile.print(analyzed)
		}

	}

}
