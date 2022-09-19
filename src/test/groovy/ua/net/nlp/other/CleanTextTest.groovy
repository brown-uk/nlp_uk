#!/usr/bin/env groovy

package ua.net.nlp.other

import org.junit.jupiter.api.Test
import ua.net.nlp.other.CleanText.CleanOptions
import ua.net.nlp.other.CleanText.MarkOption

import static org.junit.jupiter.api.Assertions.assertArrayEquals
import static org.junit.jupiter.api.Assertions.assertEquals



class CleanTextTest {
    CleanOptions options = [ "wordCount": 0, "debug": true ]

    CleanText cleanText = new CleanText( options )

    def file() { return new File("/dev/null") }

    String clean(String str) {
        println "---------"
        str = str.replace('_', '')
        cleanText.cleanUp(str, file(), options, file())
    }

	
	@Test
	public void test() {
		assertEquals "брат", clean("б_p_ат")

		assertEquals "труба", clean("тр_y_ба")

        assertEquals "\"труба", clean("\\\"трyба")

        assertEquals "п'яний", clean("п\\'яний")
        
		assertEquals "on throughпортал в", clean("on throughпортал в")

		assertEquals "урахування\n", clean("ураху-\nвання")
        assertEquals "Прем’єр-ліги\n", clean("Прем’єр-\nліги")
//        assertEquals "інформаційно\u2013звітний\n", clean("інформаційно\u2013\nзвітний")
        
		assertEquals "екс-«депутат»\n", clean("екс-\n«депутат»")

		assertEquals "\"депутат\" H'''", clean("''депутат'' H'''")

        assertEquals "- Агов", clean("-Агов")
        assertEquals "-УВАТ(ИЙ)", clean("-УВАТ(ИЙ)")

        assertEquals "- архієпископ\n- Дитина", clean("-архієпископ\n-Дитина")
        assertEquals "-то ", clean("-то ")
        
        assertEquals "За віщо йому таке. Вул. Залізнична 3а.", clean("3а віщо йому таке. Вул. 3алізнична 3а.")
        
        assertEquals "концентрація CO та CO2", clean("концентрація СO та CО2")
        
        assertEquals "загальновідоме", clean("загальновідо¬ме")
        assertEquals "по-турецьки", clean("по¬турецьки")
        assertEquals "10-11", clean("10¬11")
        assertEquals "о¬е", clean("о¬е")
    }

	@Test
	void test2() {
		def result = cleanText.cleanUp('просто-\nрово-часового', file(), new CleanOptions(), file())
		assert result == "просторово-часового\n"
		//result = cleanText.cleanUp('двох-\nсторонній', file, [])
		//assert result == "двохсторонній\n"
		
		//TODO:
		result = cleanText.cleanUp("минулого-сучасного-май-\nбутнього", file(), new CleanOptions(), file())
		assert result == "минулого-сучасного-майбутнього\n"
        
        result = cleanText.cleanUp("новоствореноі", file(), new CleanOptions(), file())
        assert result == "новоствореної"

        // don't touch abbreviations or short words
        result = cleanText.cleanUp("МОІ", file(), new CleanOptions(), file())
        assert result == "МОІ"

        result = cleanText.cleanUp("Північноірландські", file(), new CleanOptions(), file())
        assert result == "Північноірландські"

        result = cleanText.cleanUp("благо-\nдійної", file(), new CleanOptions(), file())
        assert result == "благодійної\n"

        result = cleanText.cleanUp("Нацполіціі", file(), new CleanOptions(), file())
        assert result == "Нацполіції"

        //TODO:
//        result = cleanText.cleanUp("Зе- ленський", file(), new CleanOptions())
//        assert result == "Зеленський"

        result = cleanText.cleanUp("чоло-віка", file(), new CleanOptions(), file())
        assert result == "чоловіка"

        // latin i
        def orig = "чоловіка i жінки"
        result = cleanText.cleanUp(orig, file(), new CleanOptions(), file())
        assert result != orig
        assert result == "чоловіка і жінки"
	}

	@Test
	public void testDosNewline() {
		assertEquals "брат\r\n", clean("б_p_ат\r\n")
	}

    
    @Test
    public void testMarkLanguage() {
        options.markLanguages = MarkOption.mark 
        
        String expected=
'''Десь там.

<span lang="ru" rate="1.0">Где-то такой</span>

<span lang="ru" rate="0.8">Да да</span> 
'''
        
        assertEquals expected, clean("Десь там.\n\nГде-то такой\n\nДа да \n")
        
        assertEquals '<span lang="ru" rate="0.8">Кому я продался?</span>', clean("Кому я продался?")
        
        def ukrSent = "Депутате Хмаро, я закликаю вас до порядку."
        assertEquals ukrSent, clean(ukrSent)
        
        def ukrSent1 = "– Прим. ред."
        assertEquals ukrSent1, clean(ukrSent1)
        
        def ukrSent2 = "Classmark Р 382.c.367.2."
        assertEquals ukrSent2, clean(ukrSent2)

        def expectedRates = [(double)0.8, (double)0.0]
        assertEquals(expectedRates, cleanText.evalChunk("дерзаючий"))

//        expectedRates = [(double)0.8, (double)0.0]
        assertEquals(expectedRates, cleanText.evalChunk("енергозбереженню"))

        def rates = cleanText.evalChunk("Arsenal по\u2013царськи")
        assertEquals(1.0, rates[0], 1E-2)
        assertEquals(0.0, rates[1], 1E-2)
        
//        def ukrSent3 = 
//'''Фінал передбачується на початку оповіді у незначних зауваженнях: «Стены 
//казались несокрушимыми» [7; 65] («Призывающий Зверя»), «Петр Антонович вспомнил прочитанную им вчера в журнале 
//министерства народного просвещения статью, из которой ему особенно почему-то запомнилось в коротких словах 
//пересказанное предание о лесной волшебнице Турандине.'''
//        assertEquals ukrSent3, clean(ukrSent3)

    }

    @Test
    public void testMarkLanguageCut() {
        options.markLanguages = MarkOption.cut 
        
        String expected=
'''Десь там.

<span lang="ru">---</span>

<span lang="ru">---</span>

<span lang="ru">---</span>
'''

        assertEquals expected, clean("Десь там.\n\nГде-то такой\n\nДа да \n\n<span lang=\"ru\">---</span>\n")
    }

    @Test
    public void testSplitHttp() {
        
        String expected=
'''фото з http://www.rvps.kiev.ua/'''

        assertEquals expected, clean("фото зhttp://www.rvps.kiev.ua/")
    }
}
