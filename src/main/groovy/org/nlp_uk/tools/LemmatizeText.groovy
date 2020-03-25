#!/usr/bin/env groovy

package org.nlp_uk.tools

@Grab(group='org.languagetool', module='language-uk', version='4.9')
@Grab(group='ch.qos.logback', module='logback-classic', version='1.2.3')
@Grab(group='commons-cli', module='commons-cli', version='1.4')

import org.languagetool.*
import org.languagetool.rules.*
import org.languagetool.tokenizers.*
import org.languagetool.language.*
import org.languagetool.uk.*
import groovy.lang.Closure
import groovy.util.Eval


class LemmatizeText {
    @groovy.transform.SourceURI
    static SOURCE_URI
    static SCRIPT_DIR=new File(SOURCE_URI).parent

    // easy way to include a class without forcing classpath to be set
    static textUtils = Eval.me(new File("$SCRIPT_DIR/TextUtils.groovy").text)


    JLanguageTool langTool = new MultiThreadedJLanguageTool(new Ukrainian());

    def options

    LemmatizeText(options) {
        this.options = options
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
        return sb.toString()
    }

    def process() {
        textUtils.processByParagraph(options, { buffer->
            return analyzeText(buffer)
        })
    }


    static void main(String[] argv) {

        def cli = new CliBuilder()

        cli.i(longOpt: 'input', args:1, required: true, 'Input file')
        cli.o(longOpt: 'output', args:1, required: false, 'Output file (default: <input file> - .txt + .lemmatized.txt)')
        cli.f(longOpt: 'firstLemmaOnly', 'print only first lemma with first set of tags'
            + ' (note: this mode is not recommended as first lemma/tag is almost random, this may be improved later with statistical analysis)')
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

            def outfile = options.input == '-' ? '-' : options.input.replaceFirst(/\.txt$/, '') + ".lemmatized.txt"
            argv2 << "-o" << outfile

            options = cli.parse(argv2)
        }


        def nlpUk = new LemmatizeText(options)

        nlpUk.process()

    }

}
