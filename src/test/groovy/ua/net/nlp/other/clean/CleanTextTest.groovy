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
class CleanTextTest {
    final boolean NEW_TESTS = Boolean.getBoolean("ua.net.nlp.tests.new")
    
    CleanOptions options = new CleanOptions("wordCount": 0, "debug": true)

    CleanTextCore cleanText = new CleanTextCore( options )

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
    
    @BeforeEach
    public void init() {
//        cleanText.out.init()
        
        cleanText.out.out.set(new PrintStream(outputStream))
    }
            
    @CompileStatic
    String clean(String str) {
        str = str.replace('_', '')
        cleanText.cleanText(str, null, null)
    }

	
	@Test
	public void test() {
        assertEquals "\"труба", clean("\\\"трyба")

        assertEquals "п'яний", clean("п\\'яний")
        
		assertEquals "on throughпортал в", clean("on throughпортал в")
        
        assertEquals "загальновідоме", clean("загальновідо¬ме")
        assertEquals "по-турецьки", clean("по¬турецьки")
        assertEquals "10-11", clean("10¬11")
        assertEquals "о¬е", clean("о¬е")
        assertEquals "екс-глава", clean("екс¬глава")
        
        // don't touch
        def txt = '"Дїла"\n білїють'
        assertEquals txt, clean(txt)
	}

    @Test
    public void testTypos() {
        assertEquals "нагородження", clean("нагородженння")
        // don't touch
        assertEquals "Польттті", clean("Польттті")
    }
    
    @Test
    public void testControlChars() {
        assertEquals "екс-глава", clean("екс\u001Fглава")
        assertEquals "екс-глава", clean("екс\u001E\nглава")
        assertEquals "пузатий", clean("пуза\u0008\nтий")
        assertEquals "abc", clean("abc\u0008")
    }
    
    @Test
    public void testRtf() {
        def url = getClass().getClassLoader().getResource("clean/test.rtf")
        def file = new File(url.toURI())
        
        def byteStream = new ByteArrayOutputStream()
        try {
            cleanText.out.out.set(new PrintStream(byteStream))

            cleanText.cleanUp(file, options, null)
            String s = byteStream.toString()
            assertTrue(s.contains("RTF"))
        }
        finally {
            cleanText.out.out.set(System.out)
        }
    }
    
    @Test
    public void testCp1251() {
        def url = getClass().getClassLoader().getResource("clean/enc_cp1251.txt")
        def file = new File(url.toURI())
        
        def byteStream = new ByteArrayOutputStream()
        cleanText.out.out.set(new PrintStream(byteStream))

        String text = cleanText.cleanUp(file, options, null)
        assertEquals("десь там волоська голова\n", text)
    }

    @Test
    public void testUtf16() {
        def url = getClass().getClassLoader().getResource("clean/enc_utf-16.txt")
        def file = new File(url.toURI())
        
        def byteStream = new ByteArrayOutputStream()
        cleanText.out.out.set(new PrintStream(byteStream))

        String text = cleanText.cleanUp(file, options, null)
        assertEquals("Чистота й правильність української мови.\r\nВідповідь на запитання наших Читачів.\r\n", text)
    }

    @Test
    public void testNumForLetter() {
        assertEquals "За віщо йому таке. Вул. Залізнична 3а.", clean("3а віщо йому таке. Вул. 3алізнична 3а.")
    }

    @Test
    public void testApostrophe() {
        assertEquals "зв'язаний", clean("зв 'язаний")
    }
    
        
    @Test
    public void testTwoColumns() {
def text="""
десь там                квітки пахніли
хтось прийшов           на полі
зламав сайт             під високим
зламав пароль           сонцем
зламав вісь             і небом
""".trim()
        
        assertEquals null, clean(text)
    }

    @Test
	void testOi() {
        def result = clean("новоствореноі")
        assert result == "новоствореної"

        // don't touch abbreviations or short words
        result = clean("МОІ")
        assert result == "МОІ"

        result = clean("Північноірландські")
        assert result == "Північноірландські"

        result = clean("Нацполіціі")
        assert result == "Нацполіції"

        //TODO:
//        result = clea("Зе- ленський", file(), new CleanOptions())
//        assert result == "Зеленський"

//        result = clean("чоло-віка")
//        assert result == "чоловіка"
	}
    
    @Disabled
    @Test
    public void testStar() {
        def result = clean("На ду*ку генерала")
        assertEquals "На ду*ку генерала", result
        
        result = clean("Як*мій ум")
        assertEquals "Як мій ум", result
        
        result = clean("мати*вдова")
        assertEquals "мати-вдова", result
        
        result = clean("жив*е один хлопец")
        assertEquals "живе один хлопец", result
    }
    
    @Disabled
    @Test
    public void testUnderscore() {
        def result = clean("#сто_років_тому")
        assertEquals "#сто_років_тому", result
        
        result = clean("Не твоє, а н_а_ш_е!")
        assertEquals "Не твоє, а н_а_ш_е!", result

        result = clean("https://uk.wikipedia.org/wiki/Список_аеропортів_України")
        assertEquals "https://uk.wikipedia.org/wiki/Список_аеропортів_України", result

        result = clean("особа, яка_укладає_документи")
        assertEquals "особа, яка_укладає_документи", result
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
        assertEquals(expectedRates, cleanText.markLanguageModule.evalChunk("дерзаючий озером, а голова просто"))

        expectedRates = [(double)0.8, (double)0.2]
        assertEquals(expectedRates, cleanText.markLanguageModule.evalChunk("енергозбереженню прийшов повний розгром але зелений друг виручив його в складний момент"))

        def rates = cleanText.markLanguageModule.evalChunk("Arsenal по\u2013царськи")
        assertEquals(1.0d, rates[0], 1E-2d)
        assertEquals(0.0d, rates[1], 1E-2d)
        
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
        
        assumeTrue(NEW_TESTS)
        
        text = "ГОЛОВА. Борис Райков, будь ласка."
        assertEquals text, clean(text)
    }
    
    @Test
    public void testSplitHttp() {
        
        String expected=
'''фото з http://www.rvps.kiev.ua/'''

        assertEquals expected, clean("фото зhttp://www.rvps.kiev.ua/")
    }

    @Test
    public void testSpacing() {
        assertEquals "2008 року", clean("2008 р о к у")
        assertEquals "14 травня", clean("14 т р а в н я")
    }
        
    @Test
    public void testSplitBigFile() {
        String w = "abc de\n"
        int cnt = (int)(CleanTextCore.CHUNK_LIMIT * 3 / 2 / w.length())
        String text = w.repeat(cnt);        

        assertEquals text.hashCode(), clean(text).hashCode()
    }
}
