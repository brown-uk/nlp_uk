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
class CleanTextBigFileTest {
    CleanOptions options = new CleanOptions("wordCount": 0)

    CleanTextCore cleanText = new CleanTextCore( options )
    CleanTextCore2 cleanTextCore2
    
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
    
    @BeforeEach
    public void init() {
        cleanText.out.out = new PrintStream(outputStream)
        cleanTextCore2 = new CleanTextCore2(cleanText.out, options, cleanText.ltModule)
    }
            
    @CompileStatic
    String clean(String str) {
        str = str.replace('_', '')
        cleanText.cleanText(str, null, null, cleanText.out)
    }

    @Test
    public void testSplitBigFile() {
        String w = "abc de\n"
        int cnt = (int)(CleanTextCore.CHUNK_LIMIT * 3 / 2 / w.length())
        String text = w.repeat(cnt);        

        assertEquals text.hashCode(), clean(text).hashCode()
    }
}
