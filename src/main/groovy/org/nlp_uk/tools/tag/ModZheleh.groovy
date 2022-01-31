package org.nlp_uk.tools.tag

import org.languagetool.AnalyzedToken
import org.languagetool.AnalyzedTokenReadings
import org.languagetool.JLanguageTool

import groovy.transform.CompileStatic

class ModZheleh {
    JLanguageTool langTool
    
    ModZheleh(JLanguageTool langTool) {
        this.langTool = langTool
    }
    
    @CompileStatic
    private static String adjustZheleh(String text) {
        // найпаскуднїшою
        text = text.replaceAll(/(?ui)([бвгґджзклмнпрстфхцчшщ])ї/, '$1і')
        // і у сімї
        text = text.replaceAll(/(?ui)([бвпмфр])([юяє])/, '$1\'$2')
        text = text.replaceAll(/(?ui)([сцз])ь([бвпмф])([ія])/, '$1$2$3')
        // next are tagged as ":bad" in tagger
//        text = text.replaceAll(/(?ui)ь([сц])(к)/, '$1ь$2')
//        text = text.replaceAll(/(?ui)-(же|ж|би|б)/, ' $1')
    }

    @CompileStatic
    AnalyzedTokenReadings adjustTokensWithZheleh(AnalyzedTokenReadings tokenReadings, AnalyzedTokenReadings[] tokens, int idx) {
        AnalyzedToken origAnalyzedToken = tokenReadings.getReadings().get(0)
        boolean syaIsNext = idx < tokens.size()-1 && tokens[idx+1].getToken() == 'ся'

        if( ( tokenReadings.isPosTagUnknown() || syaIsNext )
            && origAnalyzedToken.token =~ /[а-яіїєґА-ЯІЇЄҐ]/ ) {

            String adjustedToken = adjustZheleh(origAnalyzedToken.token)

            if( syaIsNext ) {
                tokenReadings = langTool.getLanguage().getTagger().tag([adjustedToken + 'ся']).get(0)
                // println "trying verb:rev $adjustedToken " + tokenReadings.getReadings()
            }

            if( tokenReadings.isPosTagUnknown() ) {
                tokenReadings = langTool.getLanguage().getTagger().tag([adjustedToken]).get(0)
            }

            // put back original word
            for(int i=0; i<tokenReadings.getReadings().size(); i++) {
                AnalyzedToken token = tokenReadings.getReadings().get(i);

                String posTag = token.getPOSTag()

                tokenReadings.getReadings().set(i, new AnalyzedToken(origAnalyzedToken.token, posTag, token.lemma))
            }
        }

        return tokenReadings
    }

}
