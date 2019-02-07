#!/usr/bin/env groovy

package org.nlp_uk.tools

@Grab(group='org.languagetool', module='language-uk', version='4.4')
@Grab(group='ch.qos.logback', module='logback-classic', version='1.2.3')
@Grab(group='commons-cli', module='commons-cli', version='1.3')


import org.languagetool.*
import org.languagetool.rules.*
import org.languagetool.tokenizers.*
import org.languagetool.language.*
import org.languagetool.uk.*

import groovy.lang.Closure

import org.languagetool.tokenizers.uk.*
import java.util.regex.*
import groovy.util.Eval


class TokenizeText {
    @groovy.transform.SourceURI
    static SOURCE_URI
    static SCRIPT_DIR=new File(SOURCE_URI).parent

    // easy way to include a class without forcing classpath to be set
    static textUtils = Eval.me(new File("$SCRIPT_DIR/TextUtils.groovy").text)


    def WORD_PATTERN = ~/[а-яіїєґА-ЯІЇЄҐa-zA-Z0-9]/

    SRXSentenceTokenizer sentTokenizer = new SRXSentenceTokenizer(new Ukrainian())
    UkrainianWordTokenizer wordTokenizer = new UkrainianWordTokenizer()
    def options

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
        if( options.w ) {
            return splitWords(textToAnalyze, options.only_words)
        }
        else {
            return splitSentences(textToAnalyze)
        }
    }

    def process() {
        textUtils.processByParagraph(options, { buffer ->
            return getAnalyzed(buffer)
        })
    }

    def splitWords(String text, boolean onlyWords) {
        List<String> sentences = sentTokenizer.tokenize(text);

//        ParallelEnhancer.enhanceInstance(sentences)

        return sentences.collect { sent ->
            def words = wordTokenizer.tokenize(sent)

            def sb = new StringBuilder()

            if( onlyWords ) {
//                System.err.println( words.findAll { it.trim() && ! WORD_PATTERN.matcher(it) }.join('\n') )
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

    static void main(String[] argv) {

        def cli = new CliBuilder()

        cli.i(longOpt: 'input', args:1, required: true, 'Input file')
        cli.o(longOpt: 'output', args:1, required: false, 'Output file (default: <input file> - .txt + .tokenized.txt)')
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

        // ugly way to define default value for output
        if( ! options.output ) {
            def argv2 = new ArrayList(Arrays.asList(argv))

            def outfile = options.input == '-' ? '-' : options.input.replaceFirst(/\.txt$/, '') + ".tokenized.txt"
            argv2 << "-o" << outfile

            options = cli.parse(argv2)
        }

        def nlpUk = new TokenizeText(options)

        nlpUk.process()
    }

}
