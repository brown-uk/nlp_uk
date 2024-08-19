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
class MarkLanguageTest {
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
    public void testGetText() {
        def txt = cleanText.spacingModule.getText(new Node(word: "голова"), "")
        assertEquals( ["голова"], txt )

        txt = cleanText.spacingModule.getText(new Node(word: "голова", 
            children: ([new Node(word:"його"), new Node(word:"її", 
                children: ([new Node(word: "кудлата")]))])), "")
        assertEquals (["голова його", "голова її кудлата"], txt)
    }
    

    @CompileStatic
    void assertUntouched(String txt) {
        assertEquals txt, clean(txt)
    }

}
