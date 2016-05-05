package org.nlp_uk.tools

@Grab(group='org.languagetool', module='language-uk', version='3.3')
@Grab(group='commons-cli', module='commons-cli', version='1.3')

import java.net.*
import org.languagetool.*
import org.languagetool.rules.*
import org.languagetool.tokenizers.*
import org.languagetool.language.*
import org.languagetool.uk.*
import org.languagetool.tokenizers.uk.*

class TokenizeText {
	//	JLanguageTool langTool = new JLanguageTool(new Ukrainian())
	//JLanguageTool langTool = new MultiThreadedJLanguageTool(new Ukrainian());
    SRXSentenceTokenizer sentTokenizer = new SRXSentenceTokenizer(new Ukrainian())
	UkrainianWordTokenizer wordTokenizer = new UkrainianWordTokenizer()

	def splitSentences(String text) {
	    List<String> tokenized = sentTokenizer.tokenize(text);
	    
		def sb = new StringBuilder()
	    for (String sent: tokenized) {
			sb.append(sent.replaceAll("\n", "\\\\n")).append("\n")
		}
		
		return sb.toString()
	}

	def splitWords(String text) {
		List<String> stokenized = sentTokenizer.tokenize(text);
		
		def sb = new StringBuilder()
		for (String sent: stokenized) {
			def words = wordTokenizer.tokenize(sent)
			
			for(String word: words) {
//				sb.append("\"").append(word.replaceAll("\n", "\\\\n")).append("\"").append(" ")
				sb.append(word.replaceAll("\n", "\\\\n").replaceAll("\t", "\\\t")).append("|")
			}
			sb.append("\n")
		}
		
		return sb.toString()
	}

	static void main(String[] argv) {

		def cli = new CliBuilder()
		
		cli.i(longOpt: 'input', args:1, required: true, 'Input file')
		cli.o(longOpt: 'output', args:1, required: true, 'Output file')
		cli.w(longOpt: 'words', 'Tokenize into words')
		cli.s(longOpt: 'sentences', 'Tokenize into sentences (default)')
		cli.h(longOpt: 'help', 'Help - Usage Information')


		def options = cli.parse(argv)

		if (!options) {
			System.exit(0)
		}

		if ( options.h ) {
			cli.usage()
			System.exit(0)
		}

		def nlpUk = new TokenizeText()

		def textToAnalyze = new File(options.input).text
		def outputFile = new File(options.output)

		def analyzed
		if( options.w ) {
			analyzed = nlpUk.splitWords(textToAnalyze)
		}
		else {
			analyzed = nlpUk.splitSentences(textToAnalyze)
		}
		
		outputFile.text = analyzed
	}

}
