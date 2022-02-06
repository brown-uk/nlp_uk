package org.nlp_uk.tools

import static org.junit.jupiter.api.Assertions.*
import static org.junit.jupiter.api.Assumptions.assumeTrue

import groovy.json.JsonOutput
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.nlp_uk.tools.TagText.OutputFormat
import org.nlp_uk.tools.TagText.TagOptions
import org.nlp_uk.tools.TagText.TagResult
import org.nlp_uk.tools.TagText.TaggedToken

public class TagTextPerfTest {

    static String text
    static tagText = new TagText()
    
    @BeforeEach
    void before() {
        assumeTrue(Boolean.getBoolean("performance.tests"))
        
        if( text == null ) {
            text = new File("text1.txt").getText('UTF-8')

            // warm it up and make sure it works
            tagText.setOptions(new TagOptions(outputFormat: OutputFormat.xml))

            TagResult tagged = tagText.tagText('текст')
            assertTrue tagged.tagged.length() > 0
        }
    }
    
        
    @Test
    void testXmlPerf() {
        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.xml))

        bench(16756)
    }

    @Test
    void testJsonPerf() {
        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.json))
        
        bench(19640)
    }

    void bench(int baseline) {
        println "baseline: $baseline"
        
        long tm1 = System.currentTimeMillis()
        
        TagResult tagged = tagText.tagText(text)

        long tm2 = System.currentTimeMillis()
        def tm = tm2-tm1
        println "time: $tm, d: ${(tm-baseline)*100/baseline}%"

        assertTrue tagged.tagged.length() > 0
        
        new File("text1." + tagText.options.outputFormat ).setText(tagged.tagged, 'UTF-8')
    }
    
}
