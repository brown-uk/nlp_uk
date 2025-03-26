package ua.net.nlp.other.clean

import org.languagetool.AnalyzedToken
import org.languagetool.AnalyzedTokenReadings
import org.languagetool.Languages
import org.languagetool.language.Ukrainian
import org.languagetool.tagging.Tagger
import org.languagetool.tagging.en.EnglishTagger
import org.languagetool.tagging.ru.RussianTagger
import org.languagetool.tagging.uk.UkrainianTagger
import org.languagetool.tokenizers.SRXSentenceTokenizer
import org.languagetool.tokenizers.uk.UkrainianWordTokenizer

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@PackageScope
//@CompileStatic
class LtModule {
    @Lazy
    RussianTagger ruTagger = { Languages.getLanguageForShortCode("ru").getTagger() }()
    @Lazy
    EnglishTagger enTagger = { Languages.getLanguageForShortCode("en").getTagger() }()

    Ukrainian ukLanguage = Languages.getLanguageForShortCode("uk")
    UkrainianTagger ukTagger = ukLanguage.getTagger()
//    SRXSentenceTokenizer ukSentTokenizer = ukLanguage.getSentenceTokenizer()
    UkrainianWordTokenizer ukWordTokenizer = ukLanguage.getWordTokenizer()


    @CompileStatic
    boolean knownWordUk(String word) {
        if( ! tag(ukTagger, normalize(word))[0].hasNoTag() ) {
            return true
        }
            
        return false
    }
    
    @CompileStatic
    List<AnalyzedToken> tagWord(String word) {
        return tag(ukTagger, normalize(word))
    }
    
    @CompileStatic
    boolean knownWord(String word) {
        try {
            return knownWordUk(word)
        }
        catch (Exception e) {
            System.err.println("Failed on word: $word")
            throw e
        }
    }

    @CompileStatic
    boolean knownWordTwoLang(String word) {
        try {
            return knownWordUk(word) \
                || ! tag(ruTagger, word)[0].hasNoTag()
        }
        catch (Exception e) {
            System.err.println("Failed dual lang on word: $word")
            throw e
        }
    }

    @CompileStatic
    boolean knownWordRu(String word) {
        try {
            return ! ruTagger.tag(Arrays.asList(word))[0][0].hasNoTag()
        }
        catch (Exception e) {
            System.err.println("Failed on word: $word")
            throw e
        }
    }
    
    @CompileStatic
    boolean knownWordEn(String word) {
        try {
            return ! enTagger.tag(Arrays.asList(word))[0][0].hasNoTag()
        }
        catch (Exception e) {
            System.err.println("Failed on word: $word")
            throw e
        }
    }

    @CompileStatic
    static String normalize(String word) {
        word.replace('\u2019', '\'')
                .replace('\u02BC', '\'')
                .replace('\u2018', '\'')
                .replace('\u0301', '')
    }

    @CompileStatic
    private List<AnalyzedToken> tag(Tagger tagger, String word) {
        tagger.tag(Arrays.asList(word)).get(0).getReadings()
    }
    
    @CompileStatic
    List<String> getLemmas(String word) {
        tag(ukTagger, normalize(word))*.getLemma()
    }

    @CompileStatic
    List<AnalyzedTokenReadings> tagSent(String sent) {
        def tk = ukWordTokenizer.tokenize(sent)
        return ukTagger.tag(tk)
    }
}
