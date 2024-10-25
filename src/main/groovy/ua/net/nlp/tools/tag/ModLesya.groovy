package ua.net.nlp.tools.tag

import org.languagetool.AnalyzedToken
import org.languagetool.AnalyzedTokenReadings
import org.languagetool.JLanguageTool
import org.languagetool.tagging.uk.PosTagHelper

import groovy.transform.CompileStatic

class ModLesya {
    JLanguageTool langTool
    
    ModLesya(JLanguageTool langTool) {
        this.langTool = langTool
    }
    
    @CompileStatic
    AnalyzedTokenReadings tagWord(String word) {
        langTool.getLanguage().getTagger().tag([word]).get(0)
    }

    @CompileStatic
    AnalyzedTokenReadings adjustTokens(AnalyzedTokenReadings tokenReadings, AnalyzedTokenReadings[] tokens, int idx) {
        AnalyzedToken origAnalyzedToken = tokenReadings.getReadings().get(0)
//        boolean syaIsNext = idx < tokens.size()-1 && tokens[idx+1].getToken() == 'ся'
        
        if( ! tokenReadings.isPosTagUnknown() )
            return tokenReadings

        String originalToken = tokenReadings.cleanToken
            
        String adjustedToken = originalToken
        
        tokenReadings = tagWord(adjustedToken)

        if( tokenReadings.isPosTagUnknown() ) {
            adjustedToken = adjustedToken.replaceAll(/(?ui)йі/, 'ї')
            adjustedToken = adjustedToken.replaceAll(/(?Ui)([іо])і\b/, '$1ї')
            tokenReadings = tagWord(adjustedToken)
        }
        if( tokenReadings.isPosTagUnknown() ) {
            adjustedToken = originalToken.replaceAll(/(?ui)(.)і/, '$1и')
            tokenReadings = tagWord(adjustedToken)
        }
        if( tokenReadings.isPosTagUnknown() ) {
            adjustedToken = originalToken.replaceAll(/(?Ui)\bи/, 'і')
            tokenReadings = tagWord(adjustedToken)
        }
        if( tokenReadings.isPosTagUnknown() ) {
            adjustedToken = originalToken.replaceAll(/(?Ui)рь\b/, 'р')
            tokenReadings = tagWord(adjustedToken)
        }
        if( tokenReadings.isPosTagUnknown() ) {
            adjustedToken = originalToken.replaceAll(/(?ui)ійш/, 'іш')
            tokenReadings = tagWord(adjustedToken)
        }
        if( tokenReadings.isPosTagUnknown() ) {
            adjustedToken = originalToken.replaceAll(/(?Ui)лько\b/, 'льки')
            tokenReadings = tagWord(adjustedToken)
        }
        if( tokenReadings.isPosTagUnknown() ) {
            adjustedToken = originalToken.replaceAll(/(?Ui)иї\b/, 'ії')
            tokenReadings = tagWord(adjustedToken)
        }
        if( tokenReadings.isPosTagUnknown() ) {
            adjustedToken = originalToken.replaceAll(/(?Ui)([яа][нр])е\b/, '$1и')
            tokenReadings = tagWord(adjustedToken)
        }
        if( tokenReadings.isPosTagUnknown() ) {
            adjustedToken = originalToken.replaceAll(/(?Ui)ів\b/, 'ей')
            tokenReadings = tagWord(adjustedToken)
        }
        if( tokenReadings.isPosTagUnknown() ) {
            adjustedToken = originalToken.replaceAll(/(?Ui)([ндтч])\1ів\b/, '$1ь')
            tokenReadings = tagWord(adjustedToken)
        }
        if( tokenReadings.isPosTagUnknown() ) {
            adjustedToken = originalToken.replaceAll(/(?Ui)жу\b/, 'джу')
            tokenReadings = tagWord(adjustedToken)
        }
        if( tokenReadings.isPosTagUnknown() ) {
            adjustedToken = originalToken.replaceAll(/(?Ui)ови\b/, 'ові')
            tokenReadings = tagWord(adjustedToken)
        }
        if( tokenReadings.isPosTagUnknown() ) {
            adjustedToken = originalToken.replaceAll(/(?Ui)ов\b/, 'ів')
            tokenReadings = tagWord(adjustedToken)
        }
        if( tokenReadings.isPosTagUnknown() ) {
            adjustedToken = originalToken.replaceAll(/(?Ui)а\b/, 'у')
            def tokenReadings2 = tagWord(adjustedToken)
            if( PosTagHelper.hasPosTagPart(tokenReadings2, ":m:v_rod")) {
                tokenReadings = tokenReadings2
            }
        }

        // put back original word
        if( ! tokenReadings.isPosTagUnknown() ) {
            for(int i=0; i<tokenReadings.getReadings().size(); i++) {
                AnalyzedToken token = tokenReadings.getReadings().get(i);

                String posTag = token.getPOSTag()
                if( posTag != null && ! posTag.contains(":alt") ) {
                    posTag += ":alt"
                }

                tokenReadings.getReadings().set(i, new AnalyzedToken(origAnalyzedToken.token, posTag, token.lemma))
            }
        }

        return tokenReadings
    }

}
