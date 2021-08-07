#!/usr/bin/env groovy

package org.nlp_uk.tools

@GrabConfig(systemClassLoader=true)
@Grab(group='org.languagetool', module='language-uk', version='5.4')
@Grab(group='ch.qos.logback', module='logback-classic', version='1.2.3')
@Grab(group='info.picocli', module='picocli', version='4.6.+')

import groovy.transform.CompileStatic
import picocli.CommandLine
import picocli.CommandLine.Option
import picocli.CommandLine.ParameterException

import static org.nlp_uk.tools.LemmatizeText.LemmatizeOptions.OutputFormat.txt

import org.languagetool.*
import org.languagetool.language.*

import groovy.util.Eval
import java.util.regex.Pattern


class LemmatizeText {
    @groovy.transform.SourceURI
    static SOURCE_URI
    static SCRIPT_DIR=new File(SOURCE_URI).parent

    // easy way to include a class without forcing classpath to be set
    static textUtils = Eval.me(new File("$SCRIPT_DIR/TextUtils.groovy").text + "\n new TextUtils()")

    Pattern WORD_PATTERN = ~/[а-яіїєґА-ЯІЇЄҐa-zA-Z0-9]/
    Pattern FOOTER_PATTERN = ~/\[[0-9]{1,3}\]/
    
    def language = new Ukrainian() {
        @Override
        protected synchronized List<?> getPatternRules() { return [] }
    }

    JLanguageTool langTool = new MultiThreadedJLanguageTool(language)

    LemmatizeOptions options
    Map<String, Integer> lemmaFreqs

    LemmatizeText(options) {
        this.options = options
    }
    
    static class Analyzed {
        String tagged
    }

    @CompileStatic
    Analyzed analyzeText(String text) {
        text = FOOTER_PATTERN.matcher(text).replaceAll('')
        
        List<AnalyzedSentence> analyzedSentences = langTool.analyzeText(text);

        List<String> sentences = []
        for (AnalyzedSentence analyzedSentence : analyzedSentences) {
            List<String> sentLemmas = []
            analyzedSentence.getTokens().each { AnalyzedTokenReadings readings->
                String cleanToken = readings.getCleanToken()
                if( readings.isWhitespace() ) {
//                    cleanToken = cleanToken.replace("\t", " ")
//                    sb.append(cleanToken)
                }
                else {
                    if (readings.getAnalyzedToken(0).lemma == null ) {
                        if( WORD_PATTERN.matcher(cleanToken) ) {
                            sentLemmas << cleanToken
                        }
                    }
                    else {
                        def tokenReadings = readings.readings.findAll { it.getPOSTag() != null && ! (it.getPOSTag() =~ /_END|^</) }
                        String lemmas = options.firstLemmaOnly ? findSingleLemma(tokenReadings) : readings*.lemma.unique().join("|")
                        sentLemmas << lemmas
                    }
                }
            }
            sentLemmas = TextUtils.adjustTokens(sentLemmas, false)
            sentences << sentLemmas.join(" ")
        }
        return new Analyzed(tagged: sentences.join("\n"))
    }

    @CompileStatic
    String findSingleLemma(List<AnalyzedToken> readings) {
        if( lemmaFreqs == null ) {
            def freqFile = "org/nlp_uk/tools/lemma_ambig_freq.txt"
            InputStream freqResource = getClass().getResourceAsStream("/$freqFile")
            if( freqResource == null ) {
                freqResource = new File("$SCRIPT_DIR/../../../../resources/$freqFile").newInputStream()
            }
            lemmaFreqs = freqResource
                .readLines("UTF-8")
                .collectEntries{ def parts = it.trim().split(/\s+/); [parts[1], parts[0] as int] } 
            println "Loading ${lemmaFreqs.size()} lemma frequencies"
        }
        
        readings = filterTokens(readings, "arch")
        readings = filterTokens(readings, "alt")
        readings = filterTokens(readings, "bad")
        
        List<String> lemmas = readings.collect{ it.getLemma() }.unique()
        if( lemmas.size() > 1 ) {
            return lemmas.max{ Integer cnt = lemmaFreqs[it]; cnt ?: 0 } 
//            lemmas.sort { a,b -> 
//                def f1 = a in lemmaFreqs ? lemmaFreqs[a] : 0
//                def f2 = b in lemmaFreqs ? lemmaFreqs[b] : 0
//                f2 - f1
//            }
        }
        lemmas[0]
    }
    
    @CompileStatic
    List<AnalyzedToken> filterTokens(List<AnalyzedToken> readings, String tag) {
        def withOtherTags = readings.findAll { ! it.getPOSTag().contains(tag) }
        withOtherTags ? withOtherTags : readings
    }
    
    
    def process() {
        textUtils.processByParagraph(options, { buffer->
            return analyzeText(buffer)
        }, {})
    }


    static class LemmatizeOptions {
        @Option(names = ["-i", "--input"], arity="1", description = ["Input file"])
        String input
        @Option(names = ["-o", "--output"], arity="1", description = ["Output file (default: <input file> - .txt + .tagged.txt/.xml)"])
        String output
        @Option(names = ["-f", "--firstLemmaOnly"], description = ["print only first lemma with first set of tags"
            + " (note: this mode is not recommended as first lemma/tag is almost random, this may be improved later with statistical analysis)"])
        boolean firstLemmaOnly
        @Option(names = ["-q", "--quiet"], description = ["Less output"])
        boolean quiet
        @Option(names= ["-h", "--help"], usageHelp= true, description= "Show this help message and exit.")
        boolean helpRequested
        // just stubs
        boolean noTag
        boolean singleThread = true
        enum OutputFormat { txt }
        OutputFormat outputFormat = txt 
    }
    
    @CompileStatic
    static LemmatizeOptions parseOptions(String[] argv) {
        LemmatizeOptions options = new LemmatizeOptions()
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

        if( ! options.output ) {
            String fileExt = ".txt"
            String outfile = options.input == '-' ? '-' : options.input.replaceFirst(/\.txt$/, '') + ".lemmatized" + fileExt
            options.output = outfile
        }
        options
    }
    
    static void main(String[] argv) {

        LemmatizeOptions options = parseOptions(argv)

        def nlpUk = new LemmatizeText(options)

        nlpUk.process()
    }

}
