#!/usr/bin/env groovy

package org.nlp_uk.other

import org.junit.jupiter.api.Test
import org.nlp_uk.other.CleanText.CleanOptions

import static org.junit.jupiter.api.Assertions.assertEquals



class CleanTextTest {
    CleanOptions options = [ "wordCount": 0, "debug": true ]

    CleanText cleanText = new CleanText( options )

    def file() { return new File("/dev/null") }

    String clean(String str) {
        println "---------"
        str = str.replace('_', '')
        cleanText.cleanUp(str, file(), options)
    }

	
	@Test
	public void test() {
		assertEquals "брат", clean("б_p_ат")

		assertEquals "труба", clean("тр_y_ба")

		assertEquals "on throughпортал в", clean("on throughпортал в")

		assertEquals "урахування\n", clean("ураху-\nвання")

		assertEquals "екс-«депутат»\n", clean("екс-\n«депутат»")

		assertEquals "\"депутат\" H'''", clean("''депутат'' H'''")

        assertEquals "- Агов", clean("-Агов")
        assertEquals "-УВАТ(ИЙ)", clean("-УВАТ(ИЙ)")

        assertEquals "- архієпископ\n- Дитина", clean("-архієпископ\n-Дитина")
        assertEquals "-то ", clean("-то ")
    }

	@Test
	void test2() {
		def result = cleanText.cleanUp('просто-\nрово-часового', file(), new CleanOptions())
		assert result == "просторово-часового\n"
		//result = cleanText.cleanUp('двох-\nсторонній', file, [])
		//assert result == "двохсторонній\n"
		
		//TODO:
		result = cleanText.cleanUp("минулого-сучасного-май-\nбутнього", file(), new CleanOptions())
		assert result == "минулого-сучасного-майбутнього\n"
	}

	@Test
	public void testDosNewline() {
		assertEquals "брат\r\n", clean("б_p_ат\r\n")
	}

    
    @Test
    public void testMarkLanguage() {
        options.markLanguages = true
        
        String expected=
'''Десь там.
<span lang="ru" rate="1.0">Где-то такой</span>

<span lang="ru" rate="0.8">Да да</span> 
'''
        
        assertEquals expected, clean("Десь там.\nГде-то такой\n\nДа да \n")
    }
}
