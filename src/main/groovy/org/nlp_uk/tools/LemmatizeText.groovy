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

import static org.nlp_uk.tools.LemmatizeText.TagOptions.OutputFormat.txt

import org.languagetool.*
import org.languagetool.language.*

import groovy.util.Eval


class LemmatizeText {
    @groovy.transform.SourceURI
    static SOURCE_URI
    static SCRIPT_DIR=new File(SOURCE_URI).parent

    // easy way to include a class without forcing classpath to be set
    static textUtils = Eval.me(new File("$SCRIPT_DIR/TextUtils.groovy").text + "\n new TextUtils()")


    JLanguageTool langTool = new MultiThreadedJLanguageTool(new Ukrainian());

    def options

    LemmatizeText(options) {
        this.options = options
    }
    
    static class Analyzed {
        String tagged
    }

    def analyzeText(String text) {
        List<AnalyzedSentence> analyzedSentences = langTool.analyzeText(text);

        def sb = new StringBuilder()
        for (AnalyzedSentence analyzedSentence : analyzedSentences) {
            analyzedSentence.getTokens().each { AnalyzedTokenReadings readings->
                if( readings.isWhitespace() || readings.getAnalyzedToken(0).lemma == null ) {
                    sb.append(readings.token)
                }
                else {
                    def lemmas = options.firstLemmaOnly ? readings.readings[0].getLemma() : readings*.lemma.unique().join("|")
                    sb.append(lemmas)
                }
            }
        }
        return new Analyzed(tagged: sb.toString())
    }

    def process() {
        textUtils.processByParagraph(options, { buffer->
            return analyzeText(buffer)
        }, {})
    }


    static class TagOptions {
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

        if( ! options.output ) {
            String fileExt = ".txt"
            String outfile = options.input == '-' ? '-' : options.input.replaceFirst(/\.txt$/, '') + ".lemmatized" + fileExt
            options.output = outfile
        }
        options
    }
    
    static void main(String[] argv) {

        TagOptions options = parseOptions(argv)

        def nlpUk = new LemmatizeText(options)

        nlpUk.process()
    }

}
