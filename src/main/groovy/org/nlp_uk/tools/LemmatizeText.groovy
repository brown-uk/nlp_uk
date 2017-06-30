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
			        def lemmas = options.firstLemma ? readings.readings[0].getLemma() : readings*.lemma.unique().join("|")
			        sb.append(lemmas)
			    }
			}
		}
		return sb.toString()
	}


	static void main(String[] argv) {

		def cli = new CliBuilder()

		cli.i(longOpt: 'input', args:1, required: true, 'Input file')
		cli.o(longOpt: 'output', args:1, required: false, 'Output file (default: <input file> - .txt + .lemmatized.txt)')
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

        // ugly way to define default value for output
        if( ! options.output ) {
            def argv2 = new ArrayList(Arrays.asList(argv))

            def outfile = options.input == '-' ? '-' : options.input.replaceFirst(/\.txt$/, '') + ".lemmatized.txt"
            argv2 << "-o" << outfile

            options = cli.parse(argv2)
        }


		def nlpUk = new LemmatizeText(options)

		processByParagraph(options, { buffer->
			return nlpUk.getAnalyzed(buffer)
		})
	}

	private getAnalyzed(String textToAnalyze) {
	    return analyzeText(textToAnalyze)
	}

	
	static int MAX_PARAGRAPH_SIZE =  200*1024
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
