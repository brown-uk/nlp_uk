package org.nlp_uk.tools

@Grab(group='org.languagetool', module='language-uk', version='3.4')
@Grab(group='commons-cli', module='commons-cli', version='1.3')
//@Grab(group='org.codehaus.gpars', module='gpars', version='1.2.1')


import java.net.*
import org.languagetool.*
import org.languagetool.rules.*
import org.languagetool.tokenizers.*
import org.languagetool.language.*
import org.languagetool.uk.*
import org.languagetool.tokenizers.uk.*
import java.util.regex.*
//import groovyx.gpars.ParallelEnhancer

class TokenizeText {
	def WORD_PATTERN = Pattern.compile(/(?i)[а-яіїєґa-z'0-9-]/)

	SRXSentenceTokenizer sentTokenizer = new SRXSentenceTokenizer(new Ukrainian())
	UkrainianWordTokenizer wordTokenizer = new UkrainianWordTokenizer()

	def splitSentences(String text) {
		List<String> tokenized = sentTokenizer.tokenize(text);

		def sb = new StringBuilder()
		for (String sent: tokenized) {
			sb.append(sent.replace("\n", "\\n")).append("\n")
		}

		return sb.toString()
	}

	def splitWords(String text, boolean onlyWords) {
		List<String> sentences = sentTokenizer.tokenize(text);

//		ParallelEnhancer.enhanceInstance(sentences)
		
		return sentences.collect { sent ->
			def words = wordTokenizer.tokenize(sent)

			def sb = new StringBuilder()
			
			if( onlyWords ) {
			    words = words.findAll { WORD_PATTERN.matcher(it) }
			}
			
			def separator = onlyWords ? " " : "|"
			
			words.each { word ->
				sb.append(word.replace("\n", "\\n").replace("\t", "\\t")).append(separator)
			}
			sb.toString()
		}.join("\n") + "\n"
	}

	static void main(String[] argv) {

		def cli = new CliBuilder()

		cli.i(longOpt: 'input', args:1, required: true, 'Input file')
		cli.o(longOpt: 'output', args:1, required: true, 'Output file')
		cli.w(longOpt: 'words', 'Tokenize into words')
		cli.u(longOpt: 'only_words', 'Remove non-words')
		cli.s(longOpt: 'sentences', 'Tokenize into sentences (default)')
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

		def nlpUk = new TokenizeText()

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

	private static getAnalyzed(TokenizeText nlpUk, String textToAnalyze, options) {
		def analyzed
		if( options.w ) {
			analyzed = nlpUk.splitWords(textToAnalyze, options.only_words)
		}
		else {
			analyzed = nlpUk.splitSentences(textToAnalyze)
		}
	}

}
