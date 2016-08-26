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
import groovy.transform.Field;


@Field JLanguageTool langTool = new MultiThreadedJLanguageTool(new Ukrainian());
@Field def options


def cli = new CliBuilder()

cli.i(longOpt: 'input', args:1, required: true, 'Input file')
cli.o(longOpt: 'output', args:1, required: true, 'Output file')
cli.q(longOpt: 'quiet', 'Less output')
cli.h(longOpt: 'help', 'Help - Usage Information')


options = cli.parse(args)

if (!options) {
	System.exit(0)
}

if ( options.h ) {
	cli.usage()
	System.exit(0)
}



@Field homonimMap = [:]


processByParagraph(options, { buffer ->
	getHomonyms(buffer)
});


homonimMap = homonimMap.sort { -it.value }


def outputFile
if( options.output == "-" ) {
	outputFile = System.out
}
else {
	outputFile = new File(options.output)
	outputFile.setText('')	// to clear out output file
	outputFile = new PrintStream(outputFile)
}


outputFile.println("Час-та\tОм.\tЛем\tСлово\tОмоніми")

homonimMap.each{ k, v ->
	def items = k.split("\t")[1].split("\\|")
	def homonimCount = items.size()
	def posHomonimCount = items.collect { it.split(":", 2)[0] }.unique().size()
	
	def str = String.sprintf("%6d\t%d\t%d\t%s", v, homonimCount, posHomonimCount, k.replace("\t", "\t\t"))
	
	outputFile.println(str)
}



	
def getHomonyms(String text) {
	List<AnalyzedSentence> analyzedSentences = langTool.analyzeText(text);
	
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
}



static void processByParagraph(options, Closure closure) {
	
	if( ! options.quiet && options.input == "-" ) {
		System.err.println ("reading from stdin...")
	}

	def inputFile = options.input == "-" ? System.in : new File(options.input)


	def buffer = ""
	inputFile.eachLine('UTF-8', 0, { line ->
		buffer += line + "\n"

		if( buffer.endsWith("\n\n") ) {
			closure(buffer)
			buffer = ""
		}
	})
	
	if( buffer ) {
		closure(buffer)
	}

}
