#!/usr/bin/env groovy

package ua.net.nlp.tools.tokenize

import java.util.regex.Pattern
import groovy.json.JsonGenerator
import groovy.transform.CompileStatic

import org.languagetool.language.*
import org.languagetool.tokenizers.*
import org.languagetool.tokenizers.uk.*

import picocli.CommandLine
import picocli.CommandLine.Option
import picocli.CommandLine.ParameterException

import ua.net.nlp.tools.TextUtils
import ua.net.nlp.tools.TextUtils.OptionsBase
import ua.net.nlp.tools.TextUtils.OutputFormat
import ua.net.nlp.tools.TextUtils.ResultBase


@CompileStatic
class TokenizeTextCore {

    // easy way to include a class without forcing classpath to be set
//    static textUtils = new TextUtils()
    static Pattern WORD_PATTERN = ~/[а-яіїєґА-ЯІЇЄҐa-zA-Z0-9]/
    static Pattern FOOTER_PATTERN = ~/\[[0-9]{1,3}\]/

    JsonGenerator jsonUnicodeBuilder = new JsonGenerator.Options().disableUnicodeEscaping().build()

    Ukrainian language = new Ukrainian() {
        @Override
        protected synchronized List<?> getPatternRules() { return [] }
    }

    SRXSentenceTokenizer sentTokenizer = new SRXSentenceTokenizer(language)
    UkrainianWordTokenizer wordTokenizer = new UkrainianWordTokenizer()
    TokenizeOptions options

    TokenizeTextCore(TokenizeOptions options) {
        this.options = options
    }

    String splitSentences(String text) {
        List<String> tokenized = sentTokenizer.tokenize(text)

        tokenized = tokenized.collect { it.replace('\n', options.newLine) }
        
//        if( options.words ) {
            tokenized = tokenized.collect { sent ->
                sent.replaceFirst(/\s$/, '')
            };
//        }

        switch (options.outputFormat) {
            case OutputFormat.txt: 
                return tokenized.join("\n") + "\n"
            case OutputFormat.xml: 
                return tokenized.collect { "<sentence>$it</sentence>" }.join("\n")
            case OutputFormat.json:
                return jsonUnicodeBuilder.toJson(tokenized).trim()[1..-2]
        } 
    }

    ResultBase getAnalyzed(String textToAnalyze) {
        String processed
        if( options.words ) {
            processed = splitWords(textToAnalyze, options.onlyWords)
        }
        else {
            processed = splitSentences(textToAnalyze)
        }
        return new ResultBase(processed)
    }

    def process() {
        TextUtils.processByParagraph(options, { String buffer ->
            return getAnalyzed(buffer)
        }, {})
    }

    String splitWords(String text, boolean onlyWords) {
        def processedSentences = splitWordsInternal(text, onlyWords)

        switch (options.outputFormat ) {
            case OutputFormat.txt:
                return processedSentences.collect { sent -> 
                    sent.join(onlyWords ? " " : "|")
                }.join("\n") + "\n"

            case OutputFormat.xml: 
                return processedSentences.collect {
                    def s = it.collect { w -> "<token>$w</token>" }.join(" ")
                    "<sentence>$s</sentence>" 
                }.join("\n")

            case OutputFormat.json:
                return jsonUnicodeBuilder.toJson(processedSentences).trim()[1..-2]
        }
    }
    
    List<List<String>> splitWordsInternal(String text, boolean onlyWords) {
        
        if( onlyWords ) {
            text = FOOTER_PATTERN.matcher(text).replaceAll('')
        }
        
        List<String> sentences = sentTokenizer.tokenize(text);

//        ParallelEnhancer.enhanceInstance(sentences)

        List<List<String>> processedSentences = sentences.collect { sent ->
            def words = wordTokenizer.tokenize(sent)

            if ( onlyWords ) {
                words = words.collect { it.replace('\u0301', '') } 
                words = words.findAll { WORD_PATTERN.matcher(it) }
                TextUtils.adjustTokens(words, true)
            }
            else {
                TextUtils.adjustTokens(words, true).collect { word ->
                    word.replace("\n", options.newLine).replace("\t", " ")
                };
            }
        }
    }
    
    
    static class TokenizeOptions extends OptionsBase {
        @Option(names = ["-w", "--words"], description = ["Tokenize into words"])
        boolean words
        @Option(names = ["-u", "--onlyWords"], description = ["Remove non-words (assumes \"-w\")"])
        boolean onlyWords
        @Option(names = ["-s", "--sentences"], description = "Tokenize into sentences (default)")
        boolean sentences

        // internal for now
        String newLine = ' '
        
        TokenizeOptions() {
            outputFormat = OutputFormat.txt
        }
    }
    
    @CompileStatic
    static TokenizeOptions parseOptions(String[] argv) {
        TokenizeOptions options = new TokenizeOptions()
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
            String fileExt = "." + options.outputFormat
            String outfile = options.input == '-' ? '-' : options.input.replaceFirst(/\.txt$/, '') + ".tokenized" + fileExt
            options.output = outfile
        }
        if( options.onlyWords && ! options.words ) {
            options.words = true
        }
        options
    }


    static void main(String[] argv) {

        TokenizeOptions options = parseOptions(argv)

        def nlpUk = new TokenizeTextCore(options)

        nlpUk.process()
    }

}
