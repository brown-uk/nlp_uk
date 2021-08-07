package org.nlp_uk.tools

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.nlp_uk.tools.LemmatizeText.LemmatizeOptions

class LemmatizeTextTest {
    
    @Test
    void test() {
        LemmatizeText lemmatizeText = new LemmatizeText(new LemmatizeOptions())
        def res = lemmatizeText.analyzeText("десь досі такі підходи")
        assertEquals "десь досі такий підхід|підход", res.tagged
        
        res = lemmatizeText.analyzeText("разом з іншими")
        assertEquals "разом з інший", res.tagged
    }
    
    @Test
    void testNewLine() {
        LemmatizeText lemmatizeText = new LemmatizeText(new LemmatizeOptions())
        def res = lemmatizeText.analyzeText("десь, \"такі\", такі[9]")
        assertEquals "десь такий такий", res.tagged

        res = lemmatizeText.analyzeText("десь\nтакі\nтакі.\nДесь")
        assertEquals "десь такий такий\nдесь", res.tagged
    }

    @Test
    void testHyphenParts() {
        LemmatizeText lemmatizeText = new LemmatizeText(new LemmatizeOptions())
        def res = lemmatizeText.analyzeText("десь-то")
        assertEquals "десь то", res.tagged
    }

    @Test
    void testFirstLemma() {
        // 1st lemma by POS tag
        LemmatizeText lemmatizeText = new LemmatizeText(new LemmatizeOptions(firstLemmaOnly: true))
        def res = lemmatizeText.analyzeText("десь досі такі підходи далі")
        assertEquals "десь досі такий підхід далі", res.tagged

        // 1st lemma by POS tag
        res = lemmatizeText.analyzeText("салати")
        assertEquals "салат", res.tagged

        // 1st lemma by frequency
        res = lemmatizeText.analyzeText("стала")
        assertEquals "стати", res.tagged
    }
    
}
