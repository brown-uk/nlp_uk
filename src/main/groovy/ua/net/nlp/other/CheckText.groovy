#!/usr/bin/env groovy

package ua.net.nlp.other

// This script checks the text with LanguageTool 
// NOTE: it disables some rules, like spelling, double whitespace etc

@Grab(group='org.languagetool', module='languagetool-core', version='6.3')
@Grab(group='org.languagetool', module='language-uk', version='6.3')
@Grab(group='ch.qos.logback', module='logback-classic', version='1.4.+')
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
	

    List<RuleMatch> check(String text, boolean force, List<String> errorLines) {
        if( ! force && text.trim().isEmpty() ) 
            return
        
        List<RuleMatch> matches = langTool.check(text);
        
        if( matches.size() > 0 ) {
            printMatches(matches, text, errorLines)
        }
        
        return matches
    }


    @CompileStatic    
    void printMatches(List<RuleMatch> matches, String text, List<String> errorLines) {

        def i = 0
        def total = 0
        
        def lines = text.split("\n")
        
        for (RuleMatch match : matches) {
            errorLines << "Rule ID:  ${match.getRule().getId()}".toString()
            errorLines << "Message:  " + match.getMessage().replace("<suggestion>", "«").replace("</suggestion>", "»")

            def chunkOffset = 0
            def leftOff = 40
            def rightOff = 40
            def posInSent = match.getFromPos() - leftOff
            def posToInSent = match.getToPos() + rightOff

            def prefix = ""
            def suffix = ""
            if( posInSent <= 0 ) {
                posInSent = 0
            }
            else {
                prefix = "…"
                chunkOffset = 1
            }
            if( posToInSent >= text.length() ) {
                posToInSent = text.length()
            }
            else {
                suffix = "…"
            }

            def sample = text[posInSent..<posToInSent]
            sample = "$prefix${sample}$suffix"
            sample = sample.toString().replace("\n", ' ')
            
            errorLines << "Chunk   : " + sample
            errorLines << "Position: " + ' '.multiply(match.getFromPos()-posInSent + chunkOffset) + "^".multiply(match.toPos-match.fromPos)
            
            if( match.getSuggestedReplacements() ) {
                errorLines << "Suggestn: " + match.getSuggestedReplacements().join("; ")
            }
            errorLines << ""
        }
    }


    static class CheckOptions {
        @Option(names = ["-i", "--input"], arity="1", description = ["Input file"], required=true)
        String input
//        @Option(names = ["-o", "--output"], arity="1", description = ["Output file (default: <input file> - .txt + .tagged.txt/.xml)"])
//        String output
        boolean quiet
        @Option(names= ["-h", "--help"], usageHelp= true, description= "Show this help message and exit.")
        boolean helpRequested
    }
    
    @CompileStatic
    static CheckOptions parseOptions(String[] argv) {
        CheckOptions options = new CheckOptions()
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

        CheckOptions options = parseOptions(argv)


		def nlpUk = new CheckText()
		nlpUk.langTool.disableRules(Arrays.asList(RULES_TO_IGNORE.split(",")))

		def textToAnalyze = new File(options.input).text

		def paragraphs = textToAnalyze.split("\n\n")
		
		long tm1 = System.currentTimeMillis()
		
		
		paragraphs.each { para ->
		    def errors = []
			nlpUk.check(para, false, errors)
			errors.each { println it }
		}

        def errors = []
		nlpUk.check("", false, [])
        errors.each { println it }
		
		long tm2 = System.currentTimeMillis()
		
		println String.format("Check time: %d ms, (%d chars/sec), %d paragraphs", 
			tm2-tm1, (int)(textToAnalyze.length()*1000/(tm2-tm1)), paragraphs.size())
	}

}
