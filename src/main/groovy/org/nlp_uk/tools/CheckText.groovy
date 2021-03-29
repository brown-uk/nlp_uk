#!/usr/bin/env groovy

package org.nlp_uk.tools

@Grab(group='org.languagetool', module='language-uk', version='5.3')
@Grab(group='ch.qos.logback', module='logback-classic', version='1.2.3')
@Grab(group='info.picocli', module='picocli', version='4.6.+')

import org.languagetool.*
import org.languagetool.rules.*
import org.languagetool.tokenizers.*
import org.languagetool.language.*

import groovy.transform.CompileStatic
import picocli.CommandLine
import picocli.CommandLine.Option
import picocli.CommandLine.ParameterException

import org.languagetool.JLanguageTool.ParagraphHandling
import org.languagetool.markup.*


class CheckText {
	private static final String RULES_TO_IGNORE="MORFOLOGIK_RULE_UK_UA,COMMA_PARENTHESIS_WHITESPACE,WHITESPACE_RULE," \
	+ "UK_MIXED_ALPHABETS,UK_SIMPLE_REPLACE,UK_SIMPLE_REPLACE_SOFT,EUPHONY_OTHER,EUPHONY_PREP_V_U,INVALID_DATE,YEAR_20001," \
	+ "DATE_WEEKDAY1,DASH,UK_HIDDEN_CHARS,UPPER_INDEX_FOR_M,DEGREES_FOR_C,OVKA_FOR_PROCESS"
	
	
	final JLanguageTool langTool = new MultiThreadedJLanguageTool(new Ukrainian());
	final SRXSentenceTokenizer stokenizer = new SRXSentenceTokenizer(new Ukrainian());
	final List<Rule> allRules = langTool.getAllRules()
	int sentenceCount = 0;
	
	CheckText() {
	}

	def sentenceBuffer = []
	
	def check(text, force) {
		if( ! force && text.trim().isEmpty() ) 
			return
		
		List<String> sentences = stokenizer.tokenize(text)
		
		sentenceBuffer += sentences
		
		if( ! force && sentenceBuffer.size() < 50 )
			return
			
		if( ! sentenceBuffer )
			return
		
			
		List<AnalyzedSentence> analyzedSentences = langTool.analyzeSentences(sentenceBuffer)

		sentenceCount += sentenceBuffer.size()
		
		def annotatedText = new AnnotatedTextBuilder().addText(text).build()
		List<RuleMatch> matches = langTool.performCheck(analyzedSentences, sentenceBuffer, allRules, ParagraphHandling.NORMAL, annotatedText)

		if( matches.size() > 0 ) {
			printMatches(matches, sentenceBuffer, text)
		}
		
		sentenceBuffer.clear()
	}

	
	def printMatches(matches, sentences, text) {

		def sentPosMap = [:]
		def i = 0
		def total = 0
		sentences.each { sent ->
			sentPosMap[i++] = total..<total+sent.size()
			total += sent.size()
		}
		
		def lines = text.split("\n")
		
		def snt = sentences.size() <=1 ? " == " + sentences.join(" | ") : ""
		
		
		for (RuleMatch match : matches) {
			println "Message:  " + match.getMessage();

			def posEnt = sentPosMap.find { k,v -> match.fromPos in v }
			def lineIdx = posEnt.key
			def posInSent = match.getFromPos() - sentPosMap[lineIdx].from
			def posToInSent = match.getToPos() - sentPosMap[lineIdx].from
			
			def sentence = sentences[lineIdx]
			if( sentence.endsWith("\n") ) {
				sentence = sentence.replaceAll("\n+", "")
			}
			if( sentence.size() > posToInSent + 20 ) {
				sentence = sentence[0..posToInSent + 20] + "…"
			}
			if( sentence.contains("\n") ) {
				sentence = sentence.replace('\n', '|')
			}
			
			println "Sentence: " + sentence
			println "Position: " + ' '.multiply(posInSent) + "^".multiply(match.toPos-match.fromPos)
			
			if( match.getSuggestedReplacements() ) {
				println "Suggestn: " + match.getSuggestedReplacements().join("; ")
			}
			println ""
		}
	}


    static class TagOptions {
        @Option(names = ["-i", "--input"], arity="1", description = ["Input file"])
        String input
//        @Option(names = ["-o", "--output"], arity="1", description = ["Output file (default: <input file> - .txt + .tagged.txt/.xml)"])
//        String output
        boolean quiet
        @Option(names= ["-h", "--help"], usageHelp= true, description= "Show this help message and exit.")
        boolean helpRequested
    }
    
    @CompileStatic
    static TagOptions parseOptions(String[] argv) {
        TagOptions options = new TagOptions()
        CommandLine commandLine = new CommandLine(options)
        try {
            commandLine.parseArgs(argv)
            if (options.helpRequested) {
                commandLine.usage(System.out)
                System.exit 0
            }
        } catch (ParameterException ex) {
            println ex.message
            commandLine.usage(System.out)
            System.exit 1
        }

        options
    }


    static void main(String[] argv) {

        TagOptions options = parseOptions(argv)


		def nlpUk = new CheckText()
		nlpUk.langTool.disableRules(Arrays.asList(RULES_TO_IGNORE.split(",")))

		def textToAnalyze = new File(options.input).text
//		def outputFile = new File(options.output)

//		outputFile.text = analyzed

		def paragraphs = textToAnalyze.split("\n\n")
		
		long tm1 = System.currentTimeMillis()
		
		
		paragraphs.each { para ->
			nlpUk.check(para, false)
		}

		nlpUk.check("", false)
		
		long tm2 = System.currentTimeMillis()
		
		println String.format("Check time: %d ms, %d sentences (%d sent/sec), %d paragraphs", 
			tm2-tm1, nlpUk.sentenceCount, (int)(nlpUk.sentenceCount*1000/(tm2-tm1)), paragraphs.size())
	}

}
