package org.nlp_uk.tools

@Grab(group='org.languagetool', module='language-uk', version='3.4')
@Grab(group='commons-cli', module='commons-cli', version='1.3')

import java.net.*
import org.languagetool.*
import org.languagetool.rules.*
import org.languagetool.tokenizers.*
import org.languagetool.language.*
import org.languagetool.uk.*


class TagText {
	//	JLanguageTool langTool = new JLanguageTool(new Ukrainian())
	JLanguageTool langTool = new MultiThreadedJLanguageTool(new Ukrainian());


	def analyzeText(String text) {
		List<AnalyzedSentence> analyzedSentences = langTool.analyzeText(text);

		def sb = new StringBuilder()
		for (AnalyzedSentence analyzedSentence : analyzedSentences) {
			analyzedSentence.getTokens().each { AnalyzedTokenReadings readings->
			    if( readings.isWhitespace() || readings.getAnalyzedToken(0).lemma == null ) {
			        sb.append(readings.token)
			    }
			    else {
			        def lemmas = readings*.lemma.unique().join("|")
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


		def nlpUk = new TagText()

		def outputFile
		if( options.output != "-" ) {
			outputFile = new File(options.output)
			outputFile.setText('')	// to clear out output file
		}
		else {
			outputFile = System.out
		}

		if( options.input == "-" ) {
			if( ! options.quiet ) {
				System.err.println ("reading from stdin...")
			}
			def buffer = ""
			System.in.eachLine('UTF-8', 0, { line ->
				buffer += line + "\n"
				if( buffer.endsWith("\n\n") ) {
					def analyzed = getAnalyzed(nlpUk, buffer, options)
					outputFile.print(analyzed)
					buffer = ""
				}
			})
			if( buffer ) {
				def analyzed = getAnalyzed(nlpUk, buffer, options)
				outputFile.print(analyzed)
			}
		}
		else {
			def textToAnalyze = new File(options.input).getText('UTF-8')

			def analyzed = getAnalyzed(nlpUk, textToAnalyze, options)

			outputFile.setText(analyzed, 'UTF-8')
		}
	}

	private static getAnalyzed(TagText nlpUk, String textToAnalyze, options) {
	    return nlpUk.analyzeText(textToAnalyze)
	}

}
