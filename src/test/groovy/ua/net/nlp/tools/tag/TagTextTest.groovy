#!/bin/env groovy

package ua.net.nlp.tools.tag

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse

import org.apache.commons.text.StringEscapeUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import ua.net.nlp.tools.TextUtils.OutputFormat
import ua.net.nlp.tools.tag.TagOptions
import ua.net.nlp.tools.tag.TagStats
import ua.net.nlp.tools.tag.TagTextCore
import ua.net.nlp.tools.tag.TagTextCore.TTR
import ua.net.nlp.tools.tag.TagTextCore.TagResult
import ua.net.nlp.tools.tag.TagTextCore.TaggedSentence

@CompileStatic
class TagTextTest {
	TagOptions options = new TagOptions()

	static TagTextCore tagText = new TagTextCore()
	
	@BeforeEach
	void before() {
		tagText.setOptions(options)
	}

	def file() { return new File("/dev/null") }


	@Test
	public void testTxtFormat() {
        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.txt))
		TagResult tagged = tagText.tagText("Слово. Діло.")
		def expected = "Слово[слово/noun:inanim:n:v_naz,слово/noun:inanim:n:v_zna].[./punct]\n" \
            + "Діло[діло/noun:inanim:n:v_naz,діло/noun:inanim:n:v_zna].[./punct]"
		assertEquals expected, tagged.tagged
	}

    @Test
    public void testTxtFormatLemmaOnly() {
        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.txt, lemmaOnly: true, disambiguate: true))
        TagResult tagged = tagText.tagText("І словами її мала. Ділами швидше № 1")
        def expected = 
"""і слово вона малий.
діло швидше № 1
"""
        assertEquals expected, tagged.tagged

        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.txt, lemmaOnly: true, singleNewLineAsParagraph: true))
        
        def text=
"""(До Надежди.).
ВІРШІ ПРО ТИШУ,"""

        tagged = tagText.tagText(text)
        expected = 
"""( до Надежда.).
вірш ПРО тиша,
"""
        assertEquals expected, tagged.tagged
    }

    @Test
    public void testOmitMultiwordTag() {
        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.txt))
        TagResult tagged = tagText.tagText("Де можна")
        def expected = "Де[де/adv:&pron:int:rel,де/part,де/part:pers] можна[можна/noninfl:&predic]"
        assertEquals expected, tagged.tagged
    }


	@Test
	public void testXml() {
		tagText.setOptions(new TagOptions())

		TagResult tagged = tagText.tagText("Слово 1,5 раза.")

		def expected =
"""<sentence>
  <tokenReading>
    <token value="Слово" lemma="слово" tags="noun:inanim:n:v_naz" />
    <token value="Слово" lemma="слово" tags="noun:inanim:n:v_zna" />
  </tokenReading>
  <tokenReading>
    <token value="1,5" lemma="1,5" tags="number" />
  </tokenReading>
  <tokenReading>
    <token value="раза" lemma="раз" tags="noun:inanim:m:v_rod" />
  </tokenReading>
  <tokenReading>
    <token value="." lemma="." tags="punct" />
  </tokenReading>
</sentence>
"""
		assertEquals expected, tagged.tagged
	}

    
    @Test
    public void testXml2() {
        tagText.setOptions(new TagOptions())

        TagResult tagged = tagText.tagText("На Україна — Іспанія.")

        def expected =
"""<sentence>
  <tokenReading>
    <token value="На" lemma="на" tags="prep" />
  </tokenReading>
  <tokenReading>
    <token value="Україна" lemma="Україна" tags="noun:inanim:f:v_naz:prop:geo" />
  </tokenReading>
  <tokenReading>
    <token value="—" lemma="—" tags="punct" />
  </tokenReading>
  <tokenReading>
    <token value="Іспанія" lemma="Іспанія" tags="noun:inanim:f:v_naz:prop:geo" />
  </tokenReading>
  <tokenReading>
    <token value="." lemma="." tags="punct" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
    }

    
    @Test
    public void testXml3() {
        tagText.setOptions(new TagOptions())

        TagResult tagged = tagText.tagText("4 Ви - Василь")

        def expected =
"""<sentence>
  <tokenReading>
    <token value="4" lemma="4" tags="number" />
  </tokenReading>
  <tokenReading>
    <token value="Ви" lemma="ви" tags="noun:anim:p:v_naz:&amp;pron:pers:2" />
  </tokenReading>
  <tokenReading>
    <token value="-" lemma="-" tags="punct" />
  </tokenReading>
  <tokenReading>
    <token value="Василь" lemma="Василь" tags="noun:anim:m:v_naz:prop:fname" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
    }
    
	
	@Test
	public void testUnclass() {
		tagText.setOptions(new TagOptions())

		TagResult tagged = tagText.tagText("Crow")
		def expected =
"""<sentence>
  <tokenReading>
    <token value="Crow" lemma="Crow" tags="unclass" />
  </tokenReading>
</sentence>
"""
		assertEquals expected, tagged.tagged

        tagged = tagText.tagText("-сніг")
        expected =
"""<sentence>
  <tokenReading>
    <token value="-сніг" lemma="-сніг" tags="unclass" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
	}
    
    @Test
    public void testSymbols() {
        tagText.setOptions(new TagOptions())

        TagResult tagged = tagText.tagText(", €")
        def expected =
"""<sentence>
  <tokenReading>
    <token value="," lemma="," tags="punct" />
  </tokenReading>
  <tokenReading>
    <token value="€" lemma="€" tags="symb" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
    

        TagResult tagged2 = tagText.tagText("13-17°")
        def expected2 =
"""<sentence>
  <tokenReading>
    <token value="13-17" lemma="13-17" tags="number" />
  </tokenReading>
  <tokenReading>
    <token value="°" lemma="°" tags="symb" />
  </tokenReading>
</sentence>
"""
        assertEquals expected2, tagged2.tagged

    }

    @Test
    public void testNoMultiword() {
        tagText.setOptions(new TagOptions())

        TagResult tagged = tagText.tagText("від малку")
        def expected =
"""<sentence>
  <tokenReading>
    <token value="від" lemma="від" tags="prep" />
  </tokenReading>
  <tokenReading>
    <token value="малку" lemma="малку" tags="noninfl" />
    <token value="малку" lemma="малка" tags="noun:inanim:f:v_zna" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
    }

    @Test
    public void testUnknown() {
        tagText.setOptions(new TagOptions())

        TagResult tagged = tagText.tagText("житєє")
        def expected =
"""<sentence>
  <tokenReading>
    <token value="житєє" lemma="" tags="unknown" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged

        def expected2 =
"""<sentence>
  <tokenReading>
    <token value="триивожний" lemma="триивожний" tags="unknown" />
  </tokenReading>
</sentence>
"""
        tagText.setOptions(new TagOptions(setLemmaForUnknown: true))
        tagged = tagText.tagText("триивожний")

        assertEquals expected2, tagged.tagged
    }


	@Test
	public void testStats() {
		tagText.setOptions(new TagOptions(unknownStats: true, homonymStats: true, output: "-"))

		TagResult tagged = tagText.tagText("десь брарарат")

		assertEquals 1, tagged.stats.knownCnt
		assertEquals 1, tagged.stats.unknownMap.values().sum()
        assertEquals Arrays.asList("десь"), tagged.stats.homonymTokenMap.values().flatten()

        tagged = tagText.tagText("ОУН\u2013УПА")
        assertEquals 1, tagged.stats.knownCnt
        assertEquals Arrays.asList("ОУН-УПА"), tagged.stats.homonymTokenMap.values().flatten()
        assertFalse tagged.stats.homonymTokenMap.keySet().toString().contains("\u2013")
        
		tagged.stats.printUnknownStats()
	}
	
	
	@Test
	public void testXmlParallel() {
		
		File file = File.createTempFile("tag_input",".tmp")
		file.deleteOnExit()
		file.setText("Слово X.\n\nДіло'.\n\nМабуть кх.", "UTF-8")

		File outFile = File.createTempFile("tag_output",".tmp")
		outFile.deleteOnExit()
		outFile.text = ''

		
		tagText.setOptions(new TagOptions(input: file.path, output: outFile.path))
		
		tagText.process()

		def expected =
"""<?xml version="1.0" encoding="UTF-8"?>
<text>

<sentence>
  <tokenReading>
    <token value="Слово" lemma="слово" tags="noun:inanim:n:v_naz" />
    <token value="Слово" lemma="слово" tags="noun:inanim:n:v_zna" />
  </tokenReading>
  <tokenReading>
    <token value="X" lemma="X" tags="number:latin" />
  </tokenReading>
  <tokenReading>
    <token value="." lemma="." tags="punct" />
  </tokenReading>
</sentence>

<sentence>
  <tokenReading>
    <token value="Діло" lemma="діло" tags="noun:inanim:n:v_naz" />
    <token value="Діло" lemma="діло" tags="noun:inanim:n:v_zna" />
  </tokenReading>
  <tokenReading>
    <token value="'" lemma="'" tags="punct" />
  </tokenReading>
  <tokenReading>
    <token value="." lemma="." tags="punct" />
  </tokenReading>
</sentence>

<sentence>
  <tokenReading>
    <token value="Мабуть" lemma="мабуть" tags="adv:&amp;insert" />
  </tokenReading>
  <tokenReading>
    <token value="кх" lemma="кх" tags="noninfl:onomat" />
  </tokenReading>
  <tokenReading>
    <token value="." lemma="." tags="punct" />
  </tokenReading>
</sentence>


</text>
"""
		assertEquals expected, outFile.getText("UTF-8")

        // make sure we're ok with streams
                
        tagText.setOptions(new TagOptions(input: file.path, output: '-'))
        
        tagText.process()
	}

	
	@Test
	public void testTxtParallel() {
		
		File file = File.createTempFile("tag_input",".tmp")
		file.deleteOnExit()
		file.setText("Слово швидко.\n\nДіло.\n\nШвидко.\n\n", "UTF-8")

		File outFile = File.createTempFile("tag_output",".tmp")
		outFile.deleteOnExit()
		outFile.text = ''

		
		tagText.setOptions(new TagOptions(input: file.path, output: outFile.path, outputFormat: OutputFormat.txt))
		
		tagText.process()

		def expected =
"""Слово[слово/noun:inanim:n:v_naz,слово/noun:inanim:n:v_zna] швидко[швидко/adv:compb].[./punct]
Діло[діло/noun:inanim:n:v_naz,діло/noun:inanim:n:v_zna].[./punct]
Швидко[швидко/adv:compb].[./punct]"""
		assertEquals expected, outFile.getText("UTF-8")
	}

    @Test
    public void testSingleTokenFormatJson() {
        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.json, tokenFormat: true))

        TagResult tagged = tagText.tagText("Слово")

        def alts = [ [value:"Слово", lemma: "слово", tags: "noun:inanim:n:v_zna"] ]
        def expected = [ [tokens: [ [value:"Слово", lemma: "слово", tags: "noun:inanim:n:v_naz", alts: alts] ]] ]
        assertEquals expected, new JsonSlurper().parseText(tagged.tagged)
    }
    

    @Disabled
	@Test
	public void testJsonParallel() {
		
		File file = File.createTempFile("tag_input",".tmp")
		file.deleteOnExit()
		file.setText("Слово X.\n\nДіло\".Швидко.\n\n", "UTF-8")

		File outFile = File.createTempFile("tag_output",".tmp")
		outFile.deleteOnExit()
		outFile.text = ''

		
		tagText.setOptions(new TagOptions(outputFormat: OutputFormat.json, input: file.path, output: outFile.path, singleThread: false))
		
		tagText.process()

		def expected = new JsonSlurper().parseText(
"""{
  "sentences": [
  {"tokenReadings":[
        {"tokens":[
            {"value":"Слово","tags":"noun:inanim:n:v_naz","lemma":"слово"},
            {"value":"Слово","tags":"noun:inanim:n:v_zna","lemma":"слово"}
  ]},   {"tokens":[
            {"value":"X","tags":"number:latin","lemma":"X"}]},
        {"tokens":[
            {"value":".","tags":"punct","lemma":"."}]}]},
  {"tokenReadings":[{"tokens":[
            {"value":"Діло","tags":"noun:inanim:n:v_naz","lemma":"діло"},
            {"value":"Діло","tags":"noun:inanim:n:v_zna","lemma":"діло"}]},
        {"tokens":[
            {"value":"\\"","tags":"punct","lemma":"\\""}]},
        {"tokens":[
            {"value":".","tags":"punct","lemma":"."}]}]},
  {"tokenReadings":[{"tokens":[
            {"value":"Швидко","tags":"adv:compb","lemma":"швидко"}]},
        {"tokens":[
            {"value":".","tags":"punct","lemma":"."}]}]}
  ]
}
""")

		assertEquals expected, new JsonSlurper().parseText(outFile.getText("UTF-8"))
	}

    
    @Test
    public void testTagCore() {
        tagText.setOptions(new TagOptions(setLemmaForUnknown: true))
        
        List<TaggedSentence> tagged = tagText.tagTextCore("десь брарарат.\n\nковбасу.", new TagStats(options: options))

        def expected = [['десь', 'брарарат', '.'], ['ковбаса', '.']]
        assertEquals expected, tagged.collect { it.tokens.collect { TTR ttr -> ttr.tokens[0].lemma } }

        expected = [['десь/adv', 'брарарат/unknown', './punct'], ['ковбаса/noun', './punct']]
        assertEquals expected, tagged.collect { it.tokens.collect { TTR ttr -> ttr.tokens[0].lemma + "/" + ttr.tokens[0].tags.replaceFirst(/:.*/, '')} }
    }

    
    @Test
    public void testTagCoreNoStats() {
        tagText.setOptions(new TagOptions(setLemmaForUnknown: true))
        
        List<TaggedSentence> tagged = tagText.tagTextCore("десь брарарат.\n\nковбасу.", null)

        def expected = [['десь', 'брарарат', '.'], ['ковбаса', '.']]
        assertEquals expected, tagged.collect { it.tokens.collect { TTR ttr -> ttr.tokens[0].lemma } }

        expected = [['десь/adv', 'брарарат/unknown', './punct'], ['ковбаса/noun', './punct']]
        assertEquals expected, tagged.collect { it.tokens.collect { TTR ttr -> ttr.tokens[0].lemma + "/" + ttr.tokens[0].tags.replaceFirst(/:.*/, '')} }
    }

    
    @Test
    public void testPartsSeparate() {
        tagText.setOptions(new TagOptions())

        def expected =
"""<sentence>
  <tokenReading>
    <token value="сідай" lemma="сідати" tags="verb:imperf:impr:s:2" />
  </tokenReading>
  <tokenReading>
    <token value="-но" lemma="но" tags="part" />
  </tokenReading>
</sentence>
"""
        TagResult tagged = tagText.tagText("сідай-но")
        assertEquals expected, tagged.tagged

    
        def expected2 =
"""<sentence>
  <tokenReading>
    <token value="сякий" lemma="сякий" tags="adj:m:v_naz:&amp;pron:def" />
    <token value="сякий" lemma="сякий" tags="adj:m:v_zna:rinanim:&amp;pron:def" />
  </tokenReading>
  <tokenReading>
    <token value="-то" lemma="то" tags="part" />
  </tokenReading>
</sentence>
"""
    
        tagged = tagText.tagText("сякий\u2013то")
        assertEquals expected2, tagged.tagged
        
        def expected4 =
"""<sentence>
  <tokenReading>
    <token value="десь" lemma="десь" tags="adv:&amp;pron:ind" />
  </tokenReading>
  <tokenReading>
    <token value="-то" lemma="то" tags="part" />
  </tokenReading>
</sentence>
"""
        tagged = tagText.tagText("десь-то")
        assertEquals expected4, tagged.tagged

        def expected3 =
        """<sentence>
  <tokenReading>
    <token value="себ-то" lemma="себ-то" tags="conj:subord:arch" />
  </tokenReading>
</sentence>
"""
            
        tagged = tagText.tagText("себ-то")
        assertEquals expected3, tagged.tagged

        def expected5 =
"""<sentence>
  <tokenReading>
    <token value="Взагалі" lemma="взагалі" tags="adv:&amp;insert" />
  </tokenReading>
  <tokenReading>
    <token value="-то" lemma="то" tags="part" />
  </tokenReading>
</sentence>
"""
        tagged = tagText.tagText("Взагалі-то")
        assertEquals expected5, tagged.tagged
    }

    @Disabled
    @Test
    public void testAbbrevDotSeparate() {
        tagText.setOptions(new TagOptions(singleTokenOnly: true, separateDotAbbreviation: true))

        def expected =
"""<sentence>
  <token value="англ" lemma="англ" tags="adj:f:v_dav:nv:abbr" />
  <token value="." lemma="." tags="punct" />
  <token value="file" lemma="file" tags="unclass" />
</sentence>
<paragraph/>
"""
        TagResult tagged = tagText.tagText("англ. file")
        assertEquals expected, tagged.tagged

        expected =
"""<sentence>
  <token value="8" lemma="8" tags="number" />
  <token value="." lemma="." tags="punct" />
  <token value="1" lemma="1" tags="number" />
  <token value="." lemma="." tags="punct" />
</sentence>
<paragraph/>
"""
        tagged = tagText.tagText("8.1.")
        assertEquals expected, tagged.tagged
    }

    @Test
    public void testTagTextStream() {
        def bas = new ByteArrayOutputStream(1024)
        def os = new BufferedOutputStream(bas)
        
        tagText.tagTextStream(new ByteArrayInputStream("Від малку. Дня.\n\nДесь".getBytes()), os)
        
        def expected =
"""<sentence>
  <tokenReading>
    <token value="Від" lemma="від" tags="prep" />
  </tokenReading>
  <tokenReading>
    <token value="малку" lemma="малку" tags="noninfl" />
    <token value="малку" lemma="малка" tags="noun:inanim:f:v_zna" />
  </tokenReading>
  <tokenReading>
    <token value="." lemma="." tags="punct" />
  </tokenReading>
</sentence>
<sentence>
  <tokenReading>
    <token value="Дня" lemma="день" tags="noun:inanim:m:v_rod" />
  </tokenReading>
  <tokenReading>
    <token value="." lemma="." tags="punct" />
  </tokenReading>
</sentence>

<sentence>
  <tokenReading>
    <token value="Десь" lemma="десь" tags="adv:&amp;pron:ind" />
    <token value="Десь" lemma="десь" tags="part" />
  </tokenReading>
</sentence>

"""
        assertEquals expected, new String(bas.toByteArray(), "UTF-8")
    }
    
    @Test
    public void testSpecialChars() {
        
//        assertEquals "голо\u001Fва", StringEscapeUtils.escapeXml10("п'яна голо\u001Fва")
        
        tagText.setOptions(new TagOptions(singleTokenOnly: true))

        def expected =
"""<sentence>
  <token value="голо\u001Fва" lemma="голо\u001Fва" tags="unclass" />
</sentence>
<paragraph/>
"""
        TagResult tagged = tagText.tagText("голо\u001Fва")
        assertEquals expected, tagged.tagged
    }

    @Test
    public void testSingleLineAsSentence() {
        tagText.setOptions(new TagOptions(sentencePerLine: true, lemmaOnly: true))
        TagResult tagged = tagText.tagText("Слово.\n\n\nДіло.")
        def expected = 
"""слово.


діло.
"""

        assertEquals expected, tagged.tagged
    }

//    @Test
//    public void testDismbigUnify() {
//        tagText.setOptions(new TagOptions())
//        TagResult tagged = tagText.tagText("як одного цілого.")
//        def expected =
//"""слово.
//діло.
//"""
//
//        assertEquals expected, tagged.tagged
//    }
    
}
