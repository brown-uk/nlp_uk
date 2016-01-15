package org.nlp_uk.tools

@Grab(group='org.languagetool', module='language-uk', version='3.3-SNAPSHOT')
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

	TagText() {
	}

	def analyzeText(String text) {
		List<AnalyzedSentence> analyzedSentences = langTool.analyzeText(text);

		def sb = new StringBuilder()
		for (AnalyzedSentence analyzedSentence : analyzedSentences) {
			sb.append(analyzedSentence).append("\n");
		}
		return sb.toString()
	}

	def getOmonims(String text) {
		List<AnalyzedSentence> analyzedSentences = langTool.analyzeText(text);

		def sb = new StringBuilder()
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

					sb.append(readings.join("|")).append("\n");
				}
			}
		}
		return sb.toString()
	}


	static void main(String[] argv) {

		def cli = new CliBuilder()

		cli.i(longOpt: 'input', args:1, required: true, 'Input file')
		cli.o(longOpt: 'output', args:1, required: true, 'Output file')
		cli.m(longOpt: 'omonims', 'Print omonims')
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

		def textToAnalyze = new File(options.input).text
		def outputFile = new File(options.output)

		def analyzed
		if( options.m ) {
			analyzed = nlpUk.getOmonims(textToAnalyze)
		}
		else {
			analyzed = nlpUk.analyzeText(textToAnalyze)
		}
		outputFile.text = analyzed
	}

}
