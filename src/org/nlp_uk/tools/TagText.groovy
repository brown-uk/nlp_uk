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
			sb.append(analyzedSentence).append("\n");
		}
		return sb.toString()
	}

	def getHomonims(String text) {
		List<AnalyzedSentence> analyzedSentences = langTool.analyzeText(text);

		def homonimMap = [:]
		
		def sb = new StringBuilder()
		
		def title = "Частота\tКіл-ть\tМіж ч. м.\tСлово\tОмоніми\n"
		sb.append(title)
		
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
					int cnt = homonimMap.get(homonim, 0)
					homonimMap.put(homonim, cnt+1)
				}
			}
		}
		
		homonimMap = homonimMap.sort { -it.value }
		
		homonimMap.each{ k, v ->
			def items = k.split("\\|")
			def homonimCount = items.size()
			def posHomonimCount = items.collect { it.split(":", 2)[0] }.unique().size()
			
			def str = String.sprintf("%6d\t%d\t%d\t%s\n", v, homonimCount, posHomonimCount, k)
			
			sb.append(str)
		}
		
		return sb.toString()
	}


	static void main(String[] argv) {

		def cli = new CliBuilder()

		cli.i(longOpt: 'input', args:1, required: true, 'Input file')
		cli.o(longOpt: 'output', args:1, required: true, 'Output file')
		cli.m(longOpt: 'homonims', 'Print homonims')
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
		def analyzed
		if( options.m ) {
			analyzed = nlpUk.getHomonims(textToAnalyze)
		}
		else {
			analyzed = nlpUk.analyzeText(textToAnalyze)
		}
		return analyzed
	}

}
