package ua.net.nlp.tools

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import groovy.json.JsonSlurper
import ua.net.nlp.tools.TextUtils.OutputFormat
import ua.net.nlp.tools.tokenize.TokenizeTextCore
import ua.net.nlp.tools.tokenize.TokenizeTextCore.TokenizeOptions


class TokenizeTextTest {
    
    @Test
    void test() {
        TokenizeTextCore TokenizeTextCore = new TokenizeTextCore(new TokenizeOptions())
        def res = TokenizeTextCore.getAnalyzed(",десь \"такі\" підходи")
        assertEquals ",десь \"такі\" підходи\n", res.tagged
    }
    
    @Test
    void testWords() {
        TokenizeTextCore TokenizeTextCore = new TokenizeTextCore(new TokenizeOptions(words: true))
        def res = TokenizeTextCore.getAnalyzed("Автомагістраль-Південь, наш 'видатний' автобан. Став схожий на диряве корито")
        assertEquals "Автомагістраль-Південь|,|наш|'|видатний|'|автобан|.\nСтав|схожий|на|диряве|корито\n", res.tagged
    }

    @Test
    void testWordsWithSpace() {
        TokenizeTextCore TokenizeTextCore = new TokenizeTextCore(new TokenizeOptions(words: true, preserveWhitespace: true))
        def res = TokenizeTextCore.getAnalyzed("Автомагістраль-Південь, наш 'видатний' автобан. Став схожий на диряве корито")
        assertEquals "Автомагістраль-Південь|,| |наш| |'|видатний|'| |автобан|.| \nСтав| |схожий| |на| |диряве| |корито\n", res.tagged
    }

    @Test
    void testWordsOnly() {
        TokenizeTextCore TokenizeTextCore = new TokenizeTextCore(new TokenizeOptions(onlyWords: true, words: true))
        def res = TokenizeTextCore.getAnalyzed(",десь \"такі\" підхо\u0301ди[9]")
        assertEquals "десь такі підходи\n", res.tagged
    }

    @Test
    void testHyphenParts() {
        TokenizeTextCore TokenizeTextCore = new TokenizeTextCore(new TokenizeOptions(onlyWords: true, words: true))
        def res = TokenizeTextCore.getAnalyzed("Сідай-но")
        assertEquals "Сідай -но\n", res.tagged

        res = TokenizeTextCore.getAnalyzed("десь\u2013таки")
        assertEquals "десь -таки\n", res.tagged
    }

    @Test
    void testNewLine() {
        def options = new TokenizeOptions()
        TokenizeTextCore TokenizeTextCore = new TokenizeTextCore(options)
        def res = TokenizeTextCore.getAnalyzed("десь \"такі\"\nпідходи")
        assertEquals "десь \"такі\" підходи\n", res.tagged
        
        options.newLine = "<br>"
        res = TokenizeTextCore.getAnalyzed("десь такі\nпідходи")
        assertEquals "десь такі<br>підходи\n", res.tagged
    }

    @Test
    void testJson() {
        def jsonSlurper = new JsonSlurper()

        TokenizeTextCore TokenizeTextCore = new TokenizeTextCore(new TokenizeOptions(outputFormat: OutputFormat.json))

        def object = jsonSlurper.parseText("[" + TokenizeTextCore.getAnalyzed(",десь \"такі\" підходи").tagged + "]")

        // Question: should we append \n to the tokenized sentence for json as we did for txt?
        assertEquals ([",десь \"такі\" підходи"], object)
    }

    @Test
    void testWordsOnlyJson() {
        def jsonSlurper = new JsonSlurper()

        TokenizeTextCore TokenizeTextCore = new TokenizeTextCore(new TokenizeOptions(
            outputFormat: OutputFormat.json, onlyWords: true, words: true
        ))

        def object = jsonSlurper.parseText("[" + TokenizeTextCore.getAnalyzed(",десь \"такі\" підхо\u0301ди[9]").tagged + "]")

        assertEquals ([["десь", "такі", "підходи"]], object)
    }

    @Test
    void testHyphenPartsJson() {
        def jsonSlurper = new JsonSlurper()

        TokenizeTextCore TokenizeTextCore = new TokenizeTextCore(new TokenizeOptions(
            outputFormat: OutputFormat.json, onlyWords: true, words: true
        ))

        def object = jsonSlurper.parseText("[" + TokenizeTextCore.getAnalyzed("Сідай-но").tagged + "]")
        assertEquals ([["Сідай", "-но"]], object)

        object = jsonSlurper.parseText("[" + TokenizeTextCore.getAnalyzed("десь\u2013таки").tagged + "]")
        assertEquals ([["десь", "-таки"]], object)
    }

    @Test
    void testNewLineJson() {
        def jsonSlurper = new JsonSlurper()

        def options = new TokenizeOptions(outputFormat: OutputFormat.json)
        TokenizeTextCore TokenizeTextCore = new TokenizeTextCore(options)

        def object = jsonSlurper.parseText("[" + TokenizeTextCore.getAnalyzed("десь \"такі\"\nпідходи").tagged + "]")
        assertEquals (["десь \"такі\" підходи"], object)

        options.newLine = "<br>"

        object = jsonSlurper.parseText("[" + TokenizeTextCore.getAnalyzed("десь такі\nпідходи").tagged + "]")
        assertEquals (["десь такі<br>підходи"], object)
    }

    @Test
    void testSentenceSplitJson() {
        def jsonSlurper = new JsonSlurper()

        TokenizeTextCore TokenizeTextCore = new TokenizeTextCore(new TokenizeOptions(outputFormat: OutputFormat.json))

        def object = jsonSlurper.parseText("[" + TokenizeTextCore.getAnalyzed(
            "Автомагістраль-Південь, наш 'видатний' автобан. Став схожий на диряве корито").tagged + "]")
        assertEquals (["Автомагістраль-Південь, наш 'видатний' автобан.", "Став схожий на диряве корито"], object)
    }

    @Test
    void testWordSplitJson() {
        def jsonSlurper = new JsonSlurper()

        TokenizeTextCore TokenizeTextCore = new TokenizeTextCore(new TokenizeOptions(
            outputFormat: OutputFormat.json, words: true)
        )

        def object = jsonSlurper.parseText("[" + TokenizeTextCore.getAnalyzed(
            "Автомагістраль-Південь, наш 'видатний' автобан. Став схожий на диряве корито").tagged + "]")
        assertEquals ([
            ["Автомагістраль-Південь", ",", "наш", "'", "видатний", "'", "автобан", "."],
            ["Став", "схожий", "на", "диряве", "корито"]], object)
    }

    @Test
    void testQuotesSplitJson() {
        def jsonSlurper = new JsonSlurper()

        TokenizeTextCore TokenizeTextCore = new TokenizeTextCore(new TokenizeOptions(
            outputFormat: OutputFormat.json, words: true)
        )

        def object = jsonSlurper.parseText("[" + TokenizeTextCore.getAnalyzed(
            "ТОВ «ЛАБЄАН-хісв»").tagged + "]")

        assertEquals ([["ТОВ", "«", "ЛАБЄАН-хісв", "»"]], object)
    }

    @Test
    void testQuotesSplitJsonWithWhitespace() {
        def jsonSlurper = new JsonSlurper()

        TokenizeTextCore TokenizeTextCore = new TokenizeTextCore(new TokenizeOptions(
            outputFormat: OutputFormat.json, words: true, preserveWhitespace: true)
        )

        def object = jsonSlurper.parseText("[" + TokenizeTextCore.getAnalyzed(
            "ТОВ «ЛАБЄАН-хісв»").tagged + "]")

        assertEquals ([["ТОВ", " ", "«", "ЛАБЄАН-хісв", "»"]], object)
    }
}
