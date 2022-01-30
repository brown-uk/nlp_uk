package org.nlp_uk.tools

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.nlp_uk.tools.TokenizeText.TokenizeOptions

class TokenizeTextTest {
    
    @Test
    void test() {
        TokenizeText tokenizeText = new TokenizeText(new TokenizeOptions())
        def res = tokenizeText.getAnalyzed(",десь \"такі\" підходи")
        assertEquals ",десь \"такі\" підходи\n", res.tagged
    }
    
    @Test
    void testWords() {
        TokenizeText tokenizeText = new TokenizeText(new TokenizeOptions(words: true))
        def res = tokenizeText.getAnalyzed("Автомагістраль-Південь, наш 'видатний' автобан. Став схожий на диряве корито")
        assertEquals "Автомагістраль-Південь|,| |наш| |'|видатний|'| |автобан|.| |\nСтав| |схожий| |на| |диряве| |корито|\n", res.tagged
    }

    @Test
    void testWordsOnly() {
        TokenizeText tokenizeText = new TokenizeText(new TokenizeOptions(onlyWords: true, words: true))
        def res = tokenizeText.getAnalyzed(",десь \"такі\" підхо\u0301ди[9]")
        assertEquals "десь такі підходи\n", res.tagged
    }

    @Test
    void testHyphenParts() {
        TokenizeText tokenizeText = new TokenizeText(new TokenizeOptions(onlyWords: true, words: true))
        def res = tokenizeText.getAnalyzed("Сідай-но")
        assertEquals "Сідай -но\n", res.tagged

        res = tokenizeText.getAnalyzed("десь\u2013таки")
        assertEquals "десь -таки\n", res.tagged
    }

    @Test
    void testNewLine() {
        def options = new TokenizeOptions()
        TokenizeText tokenizeText = new TokenizeText(options)
        def res = tokenizeText.getAnalyzed("десь \"такі\"\nпідходи")
        assertEquals "десь \"такі\" підходи\n", res.tagged
        
        options.newLine = "<br>"
        res = tokenizeText.getAnalyzed("десь такі\nпідходи")
        assertEquals "десь такі<br>підходи\n", res.tagged
    }

}
