package ua.net.nlp.other.clean

import org.languagetool.AnalyzedToken
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
    RussianTagger ruTagger = { new RussianTagger() }()
    @Lazy
    EnglishTagger enTagger = { new EnglishTagger() }()

    Ukrainian ukLanguage = new Ukrainian() {
        @Override
        protected synchronized List<?> getPatternRules() { return [] }
    }
    UkrainianTagger ukTagger = ukLanguage.getTagger()
    SRXSentenceTokenizer ukSentTokenizer = ukLanguage.getSentenceTokenizer()
    UkrainianWordTokenizer ukWordTokenizer = ukLanguage.getWordTokenizer()

        
    @CompileStatic
    boolean knownWord(String word) {
        try {
            return ! tag(ukTagger, normalize(word))[0].hasNoTag()
        }
        catch (Exception e) {
            System.err.println("Failed on word: $word")
            throw e
        }
    }

    @CompileStatic
    boolean knownWordTwoLang(String word) {
        try {
            return ! tag(ukTagger, normalize(word))[0].hasNoTag() \
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
    List<AnalyzedToken> tag(Tagger tagger, String word) {
        tagger.tag(Arrays.asList(word)).get(0).getReadings()
    }
    
    @CompileStatic
    List<String> getLemmas(String word) {
        tag(ukTagger, normalize(word))*.getLemma()
    }
}
