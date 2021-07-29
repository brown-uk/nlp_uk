#!/usr/bin/env groovy

package org.nlp_uk.tools

import static org.nlp_uk.tools.TokenizeText.TagOptions.OutputFormat.txt

@GrabConfig(systemClassLoader=true)
@Grab(group='org.languagetool', module='language-uk', version='5.4')
@Grab(group='ch.qos.logback', module='logback-classic', version='1.2.3')
@Grab(group='info.picocli', module='picocli', version='4.6.+')

import org.languagetool.language.*
import org.languagetool.tokenizers.*
import org.languagetool.tokenizers.uk.*

import groovy.transform.CompileStatic
import picocli.CommandLine
import picocli.CommandLine.Option
import picocli.CommandLine.ParameterException


class TokenizeText {
    @groovy.transform.SourceURI
    static SOURCE_URI
    static SCRIPT_DIR=new File(SOURCE_URI).parent

    // easy way to include a class without forcing classpath to be set
    static textUtils = Eval.me(new File("$SCRIPT_DIR/TextUtils.groovy").text + "\n new TextUtils()")


    def WORD_PATTERN = ~/[а-яіїєґА-ЯІЇЄҐa-zA-Z0-9]/

    SRXSentenceTokenizer sentTokenizer = new SRXSentenceTokenizer(new Ukrainian())
    UkrainianWordTokenizer wordTokenizer = new UkrainianWordTokenizer()
    TagOptions options

    TokenizeText(options) {
        this.options = options
    }

    def splitSentences(String text) {
        List<String> tokenized = sentTokenizer.tokenize(text);

        def sb = new StringBuilder()
        for (String sent: tokenized) {
            sb.append(sent.replace("\n", "\\n")).append("\n")
        }

        return sb.toString()
    }

    def getAnalyzed(String textToAnalyze) {
        String txt
        if( options.words ) {
            txt = splitWords(textToAnalyze, options.onlyWords)
        }
        else {
            txt = splitSentences(textToAnalyze)
        }
        return ['tagged': txt]
    }

    def process() {
        textUtils.processByParagraph(options, { buffer ->
            return getAnalyzed(buffer)
        }, {})
    }

    def splitWords(String text, boolean onlyWords) {
        List<String> sentences = sentTokenizer.tokenize(text);

//        ParallelEnhancer.enhanceInstance(sentences)

        return sentences.collect { sent ->
            def words = wordTokenizer.tokenize(sent)

            def sb = new StringBuilder()

            if( onlyWords ) {
                words = words.findAll { WORD_PATTERN.matcher(it) }

                sb.append(words.join(" "))
            }
            else {
                words.each { word ->
                sb.append(word.replace("\n", "\\n").replace("\t", "\\t")).append('|')
                }
            }
            sb.toString()
        }.join("\n") + "\n"
    }
    
    static class TagOptions {
        @Option(names = ["-i", "--input"], arity="1", description = ["Input file"])
        String input
        @Option(names = ["-o", "--output"], arity="1", description = ["Output file (default: <input file> - .txt + .tagged.txt/.xml)"])
        String output
        @Option(names = ["-w", "--words"], description = ["Tokenize into words"])
        boolean words
        @Option(names = ["-u", "--onlyWords"], description = ["Remove non-words (assumes \"-w\")"])
        boolean onlyWords
        @Option(names = ["-s", "--sentences"], description = "Tokenize into sentences (default)")
        boolean sentences
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
            String outfile = options.input == '-' ? '-' : options.input.replaceFirst(/\.txt$/, '') + ".tokenized" + fileExt
            options.output = outfile
        }
        options
    }


    static void main(String[] argv) {

        TagOptions options = parseOptions(argv)

        if( options.onlyWords && ! options.words ) {
            options.words = true
        }

        def nlpUk = new TokenizeText(options)

        nlpUk.process()
    }

}
