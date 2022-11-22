#!/usr/bin/env groovy

package ua.net.nlp.other

import org.junit.jupiter.api.Test
import ua.net.nlp.other.CleanText.CleanOptions
import ua.net.nlp.other.CleanText.MarkOption
import ua.net.nlp.other.CleanText.ParagraphDelimiter

import static org.junit.jupiter.api.Assertions.assertArrayEquals
import static org.junit.jupiter.api.Assertions.assertEquals



class CleanTextTest {
    CleanOptions options = [ "wordCount": 0, "debug": true ]

    CleanText cleanText = new CleanText( options )

    def file() { return new File("/dev/null") }

    String clean(String str) {
        // println "---------"
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
        
        assertEquals "За віщо йому таке. Вул. Залізнична 3а.", clean("3а віщо йому таке. Вул. 3алізнична 3а.")
        
        assertEquals "концентрація CO та CO2", clean("концентрація СO та CО2")
        
        assertEquals "загальновідоме", clean("загальновідо¬ме")
        assertEquals "по-турецьки", clean("по¬турецьки")
        assertEquals "10-11", clean("10¬11")
        assertEquals "о¬е", clean("о¬е")
        assertEquals "екс-глава", clean("екс¬глава")
	}
    
    @Test
    public void testWrap() {
		assertEquals "урахування\n", clean("ураху-\nвання")
        assertEquals "Прем’єр-ліги\n", clean("Прем’єр-\nліги")
//        assertEquals "інформаційно\u2013звітний\n", clean("інформаційно\u2013\nзвітний")
        
		assertEquals "екс-«депутат»\n", clean("екс-\n«депутат»")

		assertEquals "\"депутат\" H'''", clean("''депутат'' H'''")

        assertEquals "- Агов", clean("-Агов")
        assertEquals "-УВАТ(ИЙ)", clean("-УВАТ(ИЙ)")

        assertEquals "- архієпископ\n- Дитина", clean("-архієпископ\n-Дитина")
        assertEquals "-то ", clean("-то ")

        assertEquals "Інтерфакс-Україна\n", clean("Інтерфакс-\nУкраїна")

        def result = cleanText.cleanUp('просто-\nрово-часового', file(), new CleanOptions(), file())
        assert result == "просторово-часового\n"
        //result = cleanText.cleanUp('двох-\nсторонній', file, [])
        //assert result == "двохсторонній\n"
        
        //TODO:
        result = cleanText.cleanUp("минулого-сучасного-май-\nбутнього", file(), new CleanOptions(), file())
        assert result == "минулого-сучасного-майбутнього\n"

        result = cleanText.cleanUp("благо-\nдійної", file(), new CleanOptions(), file())
        assert result == "благодійної\n"
    }

    
	@Test
	void test2() {
        def result = cleanText.cleanUp("новоствореноі", file(), new CleanOptions(), file())
        assert result == "новоствореної"

        // don't touch abbreviations or short words
        result = cleanText.cleanUp("МОІ", file(), new CleanOptions(), file())
        assert result == "МОІ"

        result = cleanText.cleanUp("Північноірландські", file(), new CleanOptions(), file())
        assert result == "Північноірландські"

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

        // latin y
        orig = "з полком y поміч"
        result = cleanText.cleanUp(orig, file(), new CleanOptions(), file())
        assert result != orig
        assert result == "з полком у поміч"

        orig = "da y Вoreckomu"
        result = cleanText.cleanUp(orig, file(), new CleanOptions(), file())
        assert result == orig
	}

	@Test
	public void testDosNewline() {
		assertEquals "брат\r\n", clean("б_p_ат\r\n")
	}

    
    @Test
    public void testMarkLanguage() {
        options.markLanguages = MarkOption.mark 
        
        String expected=
'''<span lang="ru" rate="1.0">Выйдя из развозки, Дима остановился у кафе, раздумывая, а не посидеть ли ему тут с полчасика?.</span>

<span lang="ru" rate="0.8">удерживала его теперь на месте, не позволяя голове принимать какие–либо резкие решения</span>

<span lang="ru" rate="0.73">Да да, ему дали все, как положено все дали под розпись.</span>
'''
        
        String text =
"""Выйдя из развозки, Дима остановился у кафе, раздумывая, а не посидеть ли ему тут с полчасика?.

удерживала его теперь на месте, не позволяя голове принимать какие–либо резкие решения

Да да, ему дали все, как положено все дали под розпись.
"""

        assertEquals expected, clean(text)
        
//        assertEquals '<span lang="ru" rate="0.64">Кому я продался в подмастерья?</span>', clean("Кому я продался в подмастерья?")
        
        def ukrSent = "Депутате Хмаро, я закликаю вас до порядку."
        assertEquals ukrSent, clean(ukrSent)
        
        def ukrSent1 = "– Прим. ред."
        assertEquals ukrSent1, clean(ukrSent1)
        
        def ukrSent2 = "Classmark Р 382.c.367.2."
        assertEquals ukrSent2, clean(ukrSent2)

        def expectedRates = [(double)0.5, (double)0.1]
        assertEquals(expectedRates, cleanText.evalChunk("дерзаючий озером, а голова просто"))

        expectedRates = [(double)0.8, (double)0.2]
        assertEquals(expectedRates, cleanText.evalChunk("енергозбереженню прийшов повний розгром але зелений друг виручив його в складний момент"))

        def rates = cleanText.evalChunk("Arsenal по\u2013царськи")
        assertEquals(1.0, rates[0], 1E-2)
        assertEquals(0.0, rates[1], 1E-2)
        
//        def ukrSent3 = 
//'''Фінал передбачується на початку оповіді у незначних зауваженнях: «Стены 
//казались несокрушимыми» [7; 65] («Призывающий Зверя»), «Петр Антонович вспомнил прочитанную им вчера в журнале 
//министерства народного просвещения статью, из которой ему особенно почему-то запомнилось в коротких словах 
//пересказанное предание о лесной волшебнице Турандине.'''
//        assertEquals ukrSent3, clean(ukrSent3)

        text = "Лариса ГУТОРОВА"
        assertEquals text, clean(text)
        
        text = "(«Главком»)"
        assertEquals text, clean(text)

        text = "1939 — нар. Олександр Пороховщиков, рос. актор"
        assertEquals text, clean(text)

        text = "Танк «Оплот» поставлять «на рейки»"
        assertEquals text, clean(text)
        
        text = "- Хай буде щедро!"
        assertEquals text, clean(text)
    }

    @Test
    public void testMarkLanguagePara() {
        options.markLanguages = MarkOption.mark
        options.paragraphDelimiter = ParagraphDelimiter.auto
        
        String expected=
'''<span lang="ru" rate="1.0">Выйдя из развозки, Дима остановился у кафе, раздумывая, а не посидеть ли ему тут с полчасика?.</span>\r
\r
<span lang="ru" rate="0.8">удерживала его теперь на месте, не позволяя голове принимать какие–либо резкие решения.\r
Да еще.</span>\r
\r
<span lang="ru" rate="0.73">Да да, ему дали все, как положено все дали под розпись.</span>\r
'''
        
        String text =
"""Выйдя из развозки, Дима остановился у кафе, раздумывая, а не посидеть ли ему тут с полчасика?.\r

удерживала его теперь на месте, не позволяя голове принимать какие–либо резкие решения.\r
Да еще.\r

Да да, ему дали все, как положено все дали под розпись.\r
"""

        assertEquals expected, clean(text)
    }
    
    @Test
    public void testMarkLanguageCut() {
        options.markLanguages = MarkOption.cut 
        
        String expected=
'''Десь там за горою.

<span lang="ru">---</span>

<span lang="ru">---</span>


<span lang="ru">---</span>
'''

        String text =
"""Десь там за горою.

Выйдя из развозки, Дима остановился у кафе, раздумывая, а не посидеть ли ему тут с полчасика?.

Да да, ему дали все, как положено все дали под розпись.


<span lang=\"ru\">---</span>
"""

        assertEquals expected, clean(text)

        options.markLanguages = MarkOption.mark
    }

    @Test
    public void testMarkLanguageCutSingleNlPara() {
        options.markLanguages = MarkOption.cut
        options.paragraphDelimiter = ParagraphDelimiter.single_nl
        
        String expected=
'''Десь там за горою.
<span lang="ru">---</span>
<span lang="ru">---</span>
<span lang="ru">---</span>
'''

        String text =
"""Десь там за горою.
Выйдя из развозки, Дима остановился у кафе, раздумывая, а не посидеть ли ему тут с полчасика?.
Да да, ему дали все, как положено все дали под розпись.
<span lang=\"ru\">---</span>
"""

        assertEquals expected, clean(text)
    }
    
    @Test
    public void testSplitHttp() {
        
        String expected=
'''фото з http://www.rvps.kiev.ua/'''

        assertEquals expected, clean("фото зhttp://www.rvps.kiev.ua/")
    }
}
