#!/bin/env groovy

// This script checks the text with LanguageTool 
// and prints a error rating (along with count and total # of words)
//
// NOTE: it disables some rules, like spelling, double whitespace etc

package ua.net.nlp.other

import org.languagetool.JLanguageTool
import org.languagetool.MultiThreadedJLanguageTool
import org.languagetool.language.Ukrainian
import org.languagetool.rules.Rule
import org.languagetool.rules.RuleMatch
import org.languagetool.tokenizers.SRXSentenceTokenizer

import groovy.transform.CompileStatic


class EvaluateText {
    private static final String RULES_TO_IGNORE="MORFOLOGIK_RULE_UK_UA,COMMA_PARENTHESIS_WHITESPACE,WHITESPACE_RULE," \
    + "EUPHONY_PREP_V_U,EUPHONY_CONJ_I_Y,EUPHONY_PREP_Z_IZ_ZI,EUPHONY_PREP_O_OB" \
    + "DATE_WEEKDAY1,DASH,UK_HIDDEN_CHARS,UPPER_INDEX_FOR_M,DEGREES_FOR_C,DIGITS_AND_LETTERS," \
    + "UK_MIXED_ALPHABETS,UK_SIMPLE_REPLACE_SOFT"
    //,UK_SIMPLE_REPLACE,INVALID_DATE,YEAR_20001,"


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

    static def word_count(String text) {
        text.split(/[ \t\n,;.]/).findAll { it ==~ /[а-яіїєґА-ЯІЇЄҐ'ʼ’-]+/ }.size()
    }


    static void main(String[] args) {

        def dir = args.length > 0 ? args[0] : "."
        def outDir = "$dir/err"

        def outDirFile = new File(outDir)
        if( ! outDirFile.isDirectory() ) {
            System.err.println "Output dir $outDir does not exists"
            return
        }


        def nlpUk = new EvaluateText()
        nlpUk.langTool.disableRules(Arrays.asList(RULES_TO_IGNORE.split(",")))


        def ratings = ["коеф помил унік  слів файл"]
        new File("$outDir/ratings.txt").text = ""

        new File(dir).eachFile { file->
            if( ! file.name.endsWith(".txt") )
                return


            def text = file.text
            List<String> errorLines = []


            println(String.format("checking $file.name, words: %d, size: %d", word_count(text), text.size()))

            def paragraphs = text.split("\n\n")

            int matchCnt = 0
            int uniqueRules = 0

            try {
                paragraphs.each { String para ->
                    def matches = nlpUk.check(para, false, errorLines)
                    if( matches ) {
                        matchCnt += matches.size()
                        uniqueRules += getUniqueRuleCount(matches)
                    }
                }

                def matches = nlpUk.check("", true, errorLines)
                if( matches ) {
                    matchCnt += matches.size()
                    uniqueRules += getUniqueRuleCount(matches)
                }

                def wc = word_count(text)
                def rating = Math.round(matchCnt * 10000 / wc)/100
                ratings << String.format("%1.2f %4d %4d %6d %s", rating, matchCnt, uniqueRules, wc, file.name)

                new File(outDir + "/" + file.name.replace(".txt", ".err.txt")).text = errorLines.join("\n")
                errorLines.clear()            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }

        new File("$outDir/ratings.txt").text = ratings.join("\n")

    }


    static getUniqueRuleCount(matches) {
        matches.collect{ it.rule.id == "UK_SIMPLE_REPLACE" ? it.message : it.rule.id }.unique().size()
    }
}

