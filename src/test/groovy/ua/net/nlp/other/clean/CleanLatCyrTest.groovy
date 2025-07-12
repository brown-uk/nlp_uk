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
class CleanLatCyrTest {
    
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
    public void testLatDigits() {
        assertEquals "XXI", clean("XХІ")
        assertEquals "XVII", clean("XVП")
        assertEquals "XVIII", clean("XVШ")
    }

    @Test
    public void testLatCyrcMix() {
        assertEquals "брат", clean("б_p_ат")
        
        assertEquals "труба", clean("тр_y_ба")

        assertEquals "baby", clean("b_а_b_у_")
        
        assertEquals "Abby", clean("А_bb_у_")

        assertEquals "сіс", clean("с_і_с")
        
        assertEquals "Corporation", clean("С_orporation")
        
        assertEquals "нашій Twitter", clean("нашійTwitter")

        assertEquals "ДонорUA", clean("ДонорUA")
        
        // do not touch
        assertEquals "Renault Kangoo", clean("Renault Kangoo")
        
        // do not touch
        assertEquals "FREЕДОМ", clean("FREЕДОМ")

        // do not touch
        assertEquals "квадрокоптери Aquila16-fpv-kit.", clean("квадрокоптери Aquila16-fpv-kit.")
        
        assertEquals "Insider розповів", clean("Insiderрозповів")
        
        // latin i
        def orig = "чоловіка i жінки"
        def result = clean(orig)
        assert result != orig
        assert result == "чоловіка і жінки"

        // latin y
        orig = "з полком y поміч"
        result = clean(orig)
        assert result != orig
        assert result == "з полком у поміч"

        assertEquals "концентрація CO та CO2", clean("концентрація СO та CО2")

        assertEquals "не всі в", clean("не всi в")
        assertEquals "ДАІ", clean("ДАI")
        
//        assertEquals "Бі–Бі–Сi", clean("Бі–Бі–Сi")
        
        assertEquals "розвиток ІТ", clean("розвиток IТ")
        assertEquals "На лаві", clean("Hа лаві")

        // Пальчикова і Кo
        
        assertEquals "о\u0301ргани", clean("óргани")
        
        assertEquals "агітаторів", clean("ariтаторів")
        
        // old spelling
        assertEquals "роздїлив", clean("p_оздїлив")
        
        assertEquals "Ł. Op. cit.", clean("Ł. Оp. cit.") // Cyrillic "Ор"
    }
    
    @Test
    public void testUntouched() {
        // leave as is
        outputStream.reset()
        assertEquals "margin'ом", clean("margin'ом")
        assertFalse(new String(outputStream.toByteArray()).contains("mix"))

        assertUntouched("Kurjeр-ї")
        
        outputStream.reset()
        assertEquals "ГогольFest", clean("ГогольFest")
        assertFalse(new String(outputStream.toByteArray()).contains("mix"))

        outputStream.reset()
        assertEquals "Narodow-ої", clean("Narodow-ої")
        assertFalse(new String(outputStream.toByteArray()).contains("mix"))

        assertEquals "скорhйше", clean("скорhйше")
        // mark but don't fix as most probably it's "ѣ"
//        assertFalse(new String(outputStream.toByteArray()).contains("mix"))

        def orig = "da y Вoreckomu"
        assertEquals orig, clean(orig)
        
        // don't touch
        assertUntouched "senior'и"
        assertUntouched "Велесової rниги"
        assertUntouched "дівчинrи."
        assertUntouched "ГогольTRAIN"
        
        assertUntouched "Xі"
        assertUntouched "Рi0" 
        assertUntouched "OАО"
    }
    
    @CompileStatic
    void assertUntouched(String txt) {
        assertEquals txt, clean(txt)
    }

}
