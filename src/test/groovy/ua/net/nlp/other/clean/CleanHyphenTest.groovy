#!/usr/bin/env groovy

package ua.net.nlp.other.clean

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.junit.jupiter.api.Assumptions.assumeTrue

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

import groovy.transform.CompileStatic
import ua.net.nlp.other.clean.CleanOptions
import ua.net.nlp.other.clean.CleanOptions.MarkOption
import ua.net.nlp.other.clean.CleanOptions.ParagraphDelimiter
import ua.net.nlp.other.clean.CleanTextCore


@CompileStatic
class CleanHyphenTest {
    CleanOptions options = new CleanOptions("wordCount": 0, "debug": true)

    CleanTextCore cleanText = new CleanTextCore( options )

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
    
    @BeforeEach
    public void init() {
        cleanText.out.out = new PrintStream(outputStream)
    }
            
    @CompileStatic
    String clean(String str) {
        str = str.replace('_', '')
        cleanText.cleanText(str, null, null, cleanText.out)
    }

        
    @Test
    public void testWordWrap() {
		assertEquals "урахування\n", clean("ураху-\nвання")
        assertEquals "Прем’єр-ліги\n", clean("Прем’єр-\nліги")
//        assertEquals "інформаційно\u2013звітний\n", clean("інформаційно\u2013\nзвітний")
        
		assertEquals "екс-«депутат»\n", clean("екс-\n«депутат»")

		assertEquals "\"депутат\" H'''", clean("''депутат'' H'''")

        assertEquals "Інтерфакс-Україна\n", clean("Інтерфакс-\nУкраїна")

        def result = clean('просто-\nрово-часового')
        assert result == "просторово-часового\n"
        //result = clean('двох-\nсторонній', file, [])
        //assert result == "двохсторонній\n"
        
        //TODO:
        result = clean("минулого-сучасного-май-\nбутнього")
        assert result == "минулого-сучасного-майбутнього\n"

        result = clean("благо-\nдійної")
        assert result == "благодійної\n"
        
        // with space
        result = clean("кудись- \nінде")
        assert result == "кудись-інде\n"
        
        // with 2 new lines
        result = clean("сукуп-\n \n ність")
        assertEquals "сукупність\n ", result

//        result = clean("сукуп-\n --- \n ність")
//        assertEquals "сукупність\n ", result
        
        assertEquals "сьо-годні", clean("сьо-годні")
        
        assertEquals "єдності\n", clean("єднос‑\nті")
        
        outputStream.reset()
        def txt = "кількістю макро-\n та мікропор.\nу таблиці 1.\n---\n  Для"
        assertEquals txt, clean(txt)
//        println ":: " + new String(outputStream.toByteArray())
        assertFalse(new String(outputStream.toByteArray()).contains("---"))
    }

    @Test
    public void testRemove00AD() {
        assertEquals "Залізнична", clean("За\u00ADлізнична")
        assertEquals "АБ", clean("А\u200BБ")
        assertEquals "А  Б", clean("А \u200BБ")
        assertEquals "14-го", clean("14\u00ADго")
        assertEquals "необов’язковий\n", clean("необов’\u00AD\nязковий")
        assertEquals "Івано-франківський", clean("Івано-\u00ADфранківський")
    }

    @Test
    public void testRemove001D() {
        assertEquals "Баренцово-Карського", clean("Баренцово\u001DКарського")
        assertEquals "прогинання\n ", clean("прогинан\u001D\n ня")
//        assertEquals "Азово-Чорноморського\n", clean("Азово\u001D\nЧорноморського")
        assertEquals "Азово-Чорноморського\n", clean("Азово\u001DЧорно\u001D\nморського")
    }

    @Test
    public void testRemove00AC() {
        assertEquals "загальновідоме", clean("загальновідо¬ме")
        assertEquals "по-турецьки", clean("по¬турецьки")
        assertEquals "10-11", clean("10¬11")
        assertEquals "о¬е", clean("о¬е")
        assertEquals "екс-глава", clean("екс¬глава")
        assertEquals "конкурент", clean("конку¬ рент")
        // too hard for now
//        assertEquals "загальновідоме", clean("загально¬відо¬ме")
    }
    
    @Test
    public void testLeadingHyphen() {
        assertEquals "- Агов", clean("-Агов")
        assertEquals "-УВАТ(ИЙ)", clean("-УВАТ(ИЙ)")

        assertEquals "- архієпископ\n- Дитина", clean("-архієпископ\n-Дитина")
        assertEquals "-то ", clean("-то ")
        
        assertEquals "сказав він. - Подорожчання викликане", clean("сказав він. -Подорожчання викликане")
        assertEquals "люба моя,- Євген.", clean("люба моя,-Євген.")
        assertEquals "заперечив Денетор. - Я вже лічу", clean("заперечив Денетор. -Я вже лічу")
        def t = "Т. 2. -С. 212"
        assertEquals t, clean(t)

        assertEquals "будь-яким", clean("будь -яким")
        assertEquals "будь-що-будь", clean("будь - що - будь")

        // skip
        assertEquals("Слова на -овець", clean("Слова на -овець"))
    }

    @Disabled
    @Test
    public void testHyphenWithSpace() {
        assertEquals "свободи", clean("сво- боди")
        //FP
        assertEquals "теле- і радіопрограм", clean("теле- і радіопрограм")
    }

    @Test
    public void testHyphenWithSpace2() {
        assertEquals "150-річчя", clean("150- річчя")
        assertEquals "150-річчя", clean("150 \n- річчя")
        assertEquals "5-го  листопада", clean("5  -го  листопада")
        assertEquals "д-р", clean("д - р")
        // skip
        assertEquals "від - роб", clean("від - роб")
    }

    
    @Test
    public void testTilda() {
        def result = clean("по~християнськи")
        assertEquals "по-християнськи", result

        result = clean("для~мене")
        assertEquals "для мене", result
    }

}
