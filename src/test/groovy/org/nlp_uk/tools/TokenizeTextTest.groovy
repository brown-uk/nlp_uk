package org.nlp_uk.tools

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import groovy.json.JsonSlurper
import org.nlp_uk.tools.TokenizeText.TokenizeOptions
import org.nlp_uk.tools.TokenizeText.OutputFormat


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

    @Test
    void testJson() {
        def jsonSlurper = new JsonSlurper()

        TokenizeText tokenizeText = new TokenizeText(new TokenizeOptions(outputFormat: OutputFormat.json))

        def object = jsonSlurper.parseText("[" + tokenizeText.getAnalyzed(",десь \"такі\" підходи").tagged + "]")

        // Question: should we append \n to the tokenized sentence for json as we did for txt?
        assertEquals [",десь \"такі\" підходи"], object
    }

    @Test
    void testWordsOnlyJson() {
        def jsonSlurper = new JsonSlurper()

        TokenizeText tokenizeText = new TokenizeText(new TokenizeOptions(
            outputFormat: OutputFormat.json, onlyWords: true, words: true
        ))

        def object = jsonSlurper.parseText("[" + tokenizeText.getAnalyzed(",десь \"такі\" підхо\u0301ди[9]").tagged + "]")

        assertEquals [["десь", "такі", "підходи"]], object
    }

    @Test
    void testHyphenPartsJson() {
        def jsonSlurper = new JsonSlurper()

        TokenizeText tokenizeText = new TokenizeText(new TokenizeOptions(
            outputFormat: OutputFormat.json, onlyWords: true, words: true
        ))

        def object = jsonSlurper.parseText("[" + tokenizeText.getAnalyzed("Сідай-но").tagged + "]")
        assertEquals [["Сідай", "-но"]], object

        object = jsonSlurper.parseText("[" + tokenizeText.getAnalyzed("десь\u2013таки").tagged + "]")
        assertEquals [["десь", "-таки"]], object
    }

    @Test
    void testNewLineJson() {
        def jsonSlurper = new JsonSlurper()

        def options = new TokenizeOptions(outputFormat: OutputFormat.json)
        TokenizeText tokenizeText = new TokenizeText(options)

        def object = jsonSlurper.parseText("[" + tokenizeText.getAnalyzed("десь \"такі\"\nпідходи").tagged + "]")
        assertEquals ["десь \"такі\" підходи"], object

        options.newLine = "<br>"

        object = jsonSlurper.parseText("[" + tokenizeText.getAnalyzed("десь такі\nпідходи").tagged + "]")
        assertEquals ["десь такі<br>підходи"], object
    }

    @Test
    void testSentenceSplitJson() {
        def jsonSlurper = new JsonSlurper()

        TokenizeText tokenizeText = new TokenizeText(new TokenizeOptions(outputFormat: OutputFormat.json))

        def object = jsonSlurper.parseText("[" + tokenizeText.getAnalyzed(
            "Автомагістраль-Південь, наш 'видатний' автобан. Став схожий на диряве корито").tagged + "]")
        assertEquals ["Автомагістраль-Південь, наш 'видатний' автобан. ", "Став схожий на диряве корито"], object
    }

    @Test
    void testWordSplitJson() {
        def jsonSlurper = new JsonSlurper()

        TokenizeText tokenizeText = new TokenizeText(new TokenizeOptions(
            outputFormat: OutputFormat.json, words: true)
        )

        def object = jsonSlurper.parseText("[" + tokenizeText.getAnalyzed(
            "Автомагістраль-Південь, наш 'видатний' автобан. Став схожий на диряве корито").tagged + "]")
        assertEquals [
            ["Автомагістраль-Південь", ",", " ", "наш", " ", "'", "видатний", "'", " ", "автобан", ".", " "],
            ["Став", " ", "схожий", " ", "на", " ", "диряве", " ", "корито"]], object
    }

    @Test
    void testQuotesSplitJson() {
        def jsonSlurper = new JsonSlurper()

        TokenizeText tokenizeText = new TokenizeText(new TokenizeOptions(
            outputFormat: OutputFormat.json, words: true)
        )

        def object = jsonSlurper.parseText("[" + tokenizeText.getAnalyzed(
            "ТОВ «ЛАБЄАН-хісв»").tagged + "]")

        assertEquals [["ТОВ", " ", "«", "ЛАБЄАН-хісв", "»"]], object
    }
}
