#!/usr/bin/env groovy

package ua.net.nlp.other

import org.languagetool.AnalyzedToken
import org.languagetool.language.Ukrainian
import org.languagetool.synthesis.uk.UkrainianSynthesizer

@GrabConfig(systemClassLoader=true)
@Grab(group='org.languagetool', module='languagetool-core', version='6.5')
@Grab(group='org.languagetool', module='language-uk', version='6.6')
@Grab(group='ch.qos.logback', module='logback-classic', version='1.4.+')
@Grab(group='info.picocli', module='picocli', version='4.6.+')

import groovy.util.Eval


class Inflect {
    @groovy.transform.SourceURI
    static SOURCE_URI
    static SCRIPT_DIR=new File(SOURCE_URI).parent

    // easy way to include a class without forcing classpath to be set
    static textUtils = Eval.me(new File("$SCRIPT_DIR/../tools/TextUtils.groovy").text + "\n new TextUtils()")

    Ukrainian ukLanguage = new Ukrainian() {
        @Override
        protected synchronized List<?> getPatternRules() { return [] }
    }

    UkrainianSynthesizer synth = new UkrainianSynthesizer(ukLanguage);


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
