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
import ua.net.nlp.other.clean.SpacingModule.Node


@CompileStatic
class CleanSpacingTest {
    
    CleanOptions options = new CleanOptions("wordCount": 0, "debug": true)

    CleanTextCore cleanText0 = new CleanTextCore( options )
    CleanTextCore2 cleanText
    
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
    
    @BeforeEach
    public void init() {
        cleanText0.out.out = new PrintStream(outputStream)
        cleanText = new CleanTextCore2(cleanText0.out, options, cleanText0.ltModule)
    }
            
    @CompileStatic
    String clean(String str) {
        str = str.replace('_', '')
        cleanText0.cleanText(str, null, null, cleanText.out)
    }

    
    @Test
    public void testGetText() {
        def txt = cleanText.spacingModule.getText(new Node(word: "голова"), "")
        assertEquals( ["голова"], txt )

        txt = cleanText.spacingModule.getText(new Node(word: "голова", 
            children: ([new Node(word:"його"), new Node(word:"її", 
                children: ([new Node(word: "кудлата")]))])), "")
        assertEquals (["голова його", "голова її кудлата"], txt)
    }
    
    @Test
    public void testSpacing() {
        // simple cases
        assertEquals "Сесійний зал Верховної Ради", clean("С е с і й н и й\u00A0 з а л\u00A0 В е р х о в н о ї\u00A0 Р а д и")

        // heuristics
        String text = cleanText.spacingModule.removeSpacing("м и м и л и р а м у")
        assertEquals("ми мили раму", text)
        
        text = cleanText.spacingModule.removeSpacing("В у г і л л я н е б у л о")
        assertEquals("Вугілля не було", text)

        text = cleanText.spacingModule.removeSpacing "Капітуляція ще п е р е д  б о є м ."
        assertEquals("Капітуляція ще перед боєм.", text)

        text = cleanText.spacingModule.removeSpacing """Капітуляція ще п е р е д  б о є м .
В у г і л л я н е б у л о
м и м и л и р а м у
"""
        assertEquals("""Капітуляція ще перед боєм.
Вугілля не було
ми мили раму
"""
            , text)

        
        text = cleanText.spacingModule.removeSpacing("с т а н ц і ю н а Д н і п р і н а н і ч н у з м і н у")
//        assertEquals("станцію на Дніпрі на нічну зміну", text)

        text = cleanText.spacingModule.removeSpacing("м и й м е н і в і д с і ч н я п р о ф . В . Д е р ж а в и н : н е п о к а з н а і в ж е л і т н я")
        assertEquals("мий мені від січня проф. В. Державин: непоказна і вже літня", text)
//        assertEquals("мий мені від січня проф. В. Державин: не показна і вже літня", text)

        def src = "— Г м . . . — м у р к н у в в і н ."
        text = cleanText.spacingModule.removeSpacing(src)
        assertEquals("— Гм... — муркнув він.", text)

        src = "с а м і з с в о ї м к о ч е г а р с ь к и м о б о в ’я з к о м"
        text = cleanText.spacingModule.removeSpacing(src)
        assertEquals("самі з своїм кочегарським обов’язком", text)

        def txt = getClass().getClassLoader().getResource("clean/spacing.txt").text
        println cleanText.spacingModule.removeSpacing(txt)

        // too slow
        if( false ) {
          src = "В о н и п о с к л и к а л и в с і х л о т и ш і в і л о т и ш о к і з і н ш и х к і м н а т і в с е т е ч у ж о м о в н е т о в а р и с т в о д и в и л о с я н а"
          text = cleanText.spacingModule.removeSpacing(src)
          assertEquals("Вони поскликали всіх лотишів і лотишок із інших кімнат і все те чужомовне товариство дивилося на", text)
        }

        // don't touch
        assertUntouched "senior'и"
    }

    @Test
    public void testSpacingText4() {
        assumeTrue Boolean.getBoolean("clean_spacing_full")
        
        cleanText.spacingModule.fullSpacing = true
        
        def txt = getClass().getClassLoader().getResource("clean/spacing4.txt").text
        new File("src/test/resources/clean/spacing4_done.txt").text = cleanText.spacingModule.removeSpacing(txt)
    }

    
    @Test
    public void testSpacingText() {
        assumeTrue Boolean.getBoolean("clean_spacing_full")
        
        cleanText.spacingModule.fullSpacing = true
        
        def txt = getClass().getClassLoader().getResource("clean/spacing0.txt").text
        new File("src/test/resources/clean/spacing0_done.txt").text = cleanText.spacingModule.removeSpacing(txt)
    }

    @CompileStatic
    void assertUntouched(String txt) {
        assertEquals txt, clean(txt)
    }

    @Test
    public void testMergeG() {
        assertEquals "Головний", clean("Г оловний")
        assertEquals "Г полова", clean("Г полова")
    }

    @Test
    public void testTtModule() {
        assertTrue cleanText.spacingModule.goodWord("лотиш")
        assertTrue cleanText.spacingModule.goodWord("лотишів")
    }
}
