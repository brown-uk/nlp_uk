#!/bin/env groovy

// This script checks the text with LanguageTool 
// and prints a error rating (along with count and total # of words)
//
// NOTE: it disables some rules, like spelling, double whitespace etc


package org.nlp_uk.tools

@Grab(group='org.languagetool', module='language-uk', version='3.5-SNAPSHOT')

import org.codehaus.groovy.util.StringUtil;
import org.languagetool.*
import org.languagetool.rules.*
import org.languagetool.tokenizers.*
import org.languagetool.language.*
import org.languagetool.uk.*
import org.languagetool.JLanguageTool.ParagraphHandling
import org.languagetool.markup.*


class CheckText {
    private static final String RULES_TO_IGNORE="MORFOLOGIK_RULE_UK_UA,COMMA_PARENTHESIS_WHITESPACE,WHITESPACE_RULE," \
    + "EUPHONY_PREP_V_U,EUPHONY_CONJ_I_Y,EUPHONY_PREP_Z_IZ_ZI,EUPHONY_PREP_O_OB" \
    + "DATE_WEEKDAY1,DASH,UK_HIDDEN_CHARS,UPPER_INDEX_FOR_M,DEGREES_FOR_C,DIGITS_AND_LETTERS," \
    + "UK_MIXED_ALPHABETS" //,UK_SIMPLE_REPLACE,UK_SIMPLE_REPLACE_SOFT,INVALID_DATE,YEAR_20001," \
    
    
    final JLanguageTool langTool = new MultiThreadedJLanguageTool(new Ukrainian());
    final SRXSentenceTokenizer stokenizer = new SRXSentenceTokenizer(new Ukrainian());
    final List<Rule> allRules = langTool.getAllRules()
    int sentenceCount = 0;
    
    CheckText() {
    }

    def sentenceBuffer = []
    
    def check(text, force, errorLines) {
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
            printMatches(matches, sentenceBuffer, text, errorLines)
        }
        
        sentenceBuffer.clear()
        
        return matches
    }

    
    def printMatches(matches, sentences, text, errorLines) {

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
            errorLines << "Message:  " + match.getMessage().replace("<suggestion>", "«").replace("</suggestion>", "»")

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
            
            errorLines << "Sentence: " + sentence
            errorLines << "Position: " + ' '.multiply(posInSent) + "^".multiply(match.toPos-match.fromPos)
            
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


        def nlpUk = new CheckText()
        nlpUk.langTool.disableRules(Arrays.asList(RULES_TO_IGNORE.split(",")))


        def ratings = []
        new File("$outDir/ratings.txt").text = ""

        new File(dir).eachFile { file->
            if( ! file.name.endsWith(".txt") )
                return


            def text = file.text
            def errorLines = []


            println("checking $file.name " + text.size() + " " + word_count(text))

            def paragraphs = text.split("\n\n")

            int matchCnt = 0

            try {

            paragraphs.each { para ->
                def matches = nlpUk.check(para, false, errorLines)
                if( matches )
                    matchCnt += matches.size()
            }

            def matches = nlpUk.check("", true, errorLines)
            if( matches )
                   matchCnt += matches.size()

            def wc = word_count(text)
            def rating = Math.round(matchCnt * 10000 / wc)/100
            ratings << rating + " " + matchCnt + " " + wc + " " + file.name

            new File(outDir + "/" + file.name.replace(".txt", ".err.txt")).text = errorLines.join("\n")

            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    
        new File("$outDir/ratings.txt").text = ratings.join("\n")

    }

}

