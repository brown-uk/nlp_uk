#!/usr/bin/env groovy

package org.nlp_uk.tools

@Grab(group='org.languagetool', module='language-uk', version='5.2')
@Grab(group='ch.qos.logback', module='logback-classic', version='1.2.3')

import org.languagetool.*
import org.languagetool.language.*
import org.languagetool.uk.*
import org.languagetool.synthesis.uk.*
import groovy.util.Eval


class Inflect {
    @groovy.transform.SourceURI
    static SOURCE_URI
    static SCRIPT_DIR=new File(SOURCE_URI).parent

    // easy way to include a class without forcing classpath to be set
    static textUtils = Eval.me(new File("$SCRIPT_DIR/TextUtils.groovy").text + "\n new TextUtils()")


    UkrainianSynthesizer synth = new UkrainianSynthesizer();


    def inflectWord(String word, String tag, boolean regexp) {
        def token = new AnalyzedToken("", "", word);
        return synth.synthesize(token, tag, regexp);
    }

    static void main(String[] argv) {
        textUtils.warnOnWindows()


        if( argv.length != 2 ) {
            System.err.println("Використання: Inflect.groovy <lemma> <tag_regexp>")
            System.err.println("Повертає всі словоформи зі словника, що відповідають заданій лемі та виразу тегів")
            System.err.println("Опис тегів: https://github.com/brown-uk/dict_uk/blob/master/doc/tags.txt")
            System.err.println("Напр.: Inflect.groovy місто noun:inanim:n:v_rod")
            System.err.println("або: Inflect.groovy місто noun:inanim:n:v_.*")
            System.exit(1)
        }

        def nlpUk = new Inflect()

        println nlpUk.inflectWord(argv[0], argv[1], true)
    }

}
