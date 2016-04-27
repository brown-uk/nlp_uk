package org.nlp_uk.tools

@Grab(group='org.languagetool', module='language-uk', version='3.4-SNAPSHOT', changing=true)

import java.net.*
import org.languagetool.*
import org.languagetool.language.*
import org.languagetool.uk.*
import org.languagetool.synthesis.uk.*



class Inflect {
    UkrainianSynthesizer synth = new UkrainianSynthesizer();


	def inflectWord(String word, String tag) {
        def token = new AnalyzedToken("", "", word);
        return synth.synthesize(token, tag);
	}

	static void main(String[] argv) {

        if( argv.length != 2 ) {
            System.err.println("Usage: Inflect.groovy <lemma> <tag>")
            System.err.println("e.g.: Inflect.groovy місто noun:inanim:n:v_rod")
            System.exit(1)
        }

		def nlpUk = new Inflect()

        println nlpUk.inflectWord(argv[0], argv[1])
	}

}
