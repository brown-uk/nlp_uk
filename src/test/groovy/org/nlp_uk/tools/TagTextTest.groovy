#!/bin/env groovy

package org.nlp_uk.tools

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.nlp_uk.tools.TagText.OutputFormat.json

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.nlp_uk.tools.TagText.OutputFormat
import org.nlp_uk.tools.TagText.TTR
import org.nlp_uk.tools.TagText.TagOptions
import org.nlp_uk.tools.TagText.TagResult
import org.nlp_uk.tools.TagText.TaggedToken
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.json.StringEscapeUtils


class TagTextTest {
	def options = new TagOptions()

	static TagText tagText = new TagText()
	
	@BeforeEach
	void before() {
		tagText.setOptions(options)
	}

	def file() { return new File("/dev/null") }


	@Test
	public void test() {
        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.txt))
		TagResult tagged = tagText.tagText("Слово.")
		def expected = "Слово[слово/noun:inanim:n:v_naz,слово/noun:inanim:n:v_zna].[</S><P/>]"
		assertEquals expected, tagged.tagged
	}

    @Test
    public void testOmitMultiwordTag() {
        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.txt))
        TagResult tagged = tagText.tagText("Де можна")
        def expected = "Де[де/adv:&pron:int:rel,де/conj:subord,де/part,де/part:pers] можна[можна/noninfl:&predic,</S><P/>]"
        assertEquals expected, tagged.tagged
    }


	@Test
	public void testXml() {
		tagText.setOptions(new TagOptions(xmlOutput: true))

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
    <token value="." lemma="." tags="punct" whitespaceBefore="false" />
  </tokenReading>
</sentence>
"""
		assertEquals expected, tagged.tagged
	}

    
    @Test
    public void testXml2() {
        tagText.setOptions(new TagOptions(xmlOutput: true))

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
    <token value="—" lemma="—" tags="punct" whitespaceBefore="true" />
  </tokenReading>
  <tokenReading>
    <token value="Іспанія" lemma="Іспанія" tags="noun:inanim:f:v_naz:prop:geo" />
  </tokenReading>
  <tokenReading>
    <token value="." lemma="." tags="punct" whitespaceBefore="false" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
    }

	
	@Test
	public void testUnclass() {
		tagText.setOptions(new TagOptions(xmlOutput: true))

		TagResult tagged = tagText.tagText("Crow")
		def expected =
"""<sentence>
  <tokenReading>
    <token value="Crow" lemma="Crow" tags="unclass" />
  </tokenReading>
</sentence>
"""
		assertEquals expected, tagged.tagged
	}
    
    @Test
    public void testSymbols() {
        tagText.setOptions(new TagOptions(xmlOutput: true))

        TagResult tagged = tagText.tagText(", €")
        def expected =
"""<sentence>
  <tokenReading>
    <token value="," lemma="," tags="punct" whitespaceBefore="false" />
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
        tagText.setOptions(new TagOptions(xmlOutput: true))

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
        tagText.setOptions(new TagOptions(xmlOutput: true))

        TagResult tagged = tagText.tagText("житєє")
        def expected =
"""<sentence>
  <tokenReading>
    <token value="житєє" lemma="" tags="unknown" />
  </tokenReading>
</sentence>
"""
        tagText.setOptions(new TagOptions(xmlOutput: true, setLemmaForUnknown: true))
        
        assertEquals expected, tagged.tagged

        def expected2 =
"""<sentence>
  <tokenReading>
    <token value="житєє" lemma="житєє" tags="unknown" />
  </tokenReading>
</sentence>
"""
    }

    
    @Test
    public void testZheleh() {
        tagText.setOptions(new TagOptions(xmlOutput: true, modules: ["zheleh"]))

        TagResult tagged = tagText.tagText("миготїнь купаєть ся житє і смерть")
        def expected =
"""<sentence>
  <tokenReading>
    <token value="миготїнь" lemma="миготіння" tags="noun:inanim:p:v_rod" />
  </tokenReading>
  <tokenReading>
    <token value="купаєть" lemma="купатися" tags="verb:rev:imperf:pres:s:3" />
  </tokenReading>
  <tokenReading>
    <token value="ся" lemma="ся" tags="part:arch" />
    <token value="ся" lemma="сей" tags="adj:f:v_naz:&amp;pron:dem:arch" />
  </tokenReading>
  <tokenReading>
    <token value="житє" lemma="житє" tags="noun:inanim:n:v_naz:alt" />
    <token value="житє" lemma="житє" tags="noun:inanim:n:v_zna:alt" />
  </tokenReading>
  <tokenReading>
    <token value="і" lemma="і" tags="conj:coord" />
    <token value="і" lemma="і" tags="part" />
  </tokenReading>
  <tokenReading>
    <token value="смерть" lemma="смерть" tags="noun:inanim:f:v_naz" />
    <token value="смерть" lemma="смерть" tags="noun:inanim:f:v_zna" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
    }


//	@Disabled
//	@Test
//	public void testIgnoreLang() {
//		tagText.setOptions(new TagOptions(ignoreOtherLanguages: true, xmlOutput: true))
//
//		TagResult tagged = tagText.tagText("<span lang=\"ru\"">Слво.</span>")
//		assertEquals "<foreign>Слво.</foreign>\n\n", tagged
//
//		def expected=
//"""<sentence>
//  <tokenReading>
//    <token value="Газета" lemma="газета" tags="noun:inanim:f:v_naz" />
//  </tokenReading>
//  <foreign>Дело</foreign>
//  <tokenReading>
//    <token value="." tags="punct" whitespaceBefore="false" />
//  </tokenReading>
//</sentence>
//
//"""
//		
//		tagged = tagText.tagText("Газета <span lang=\"ru\">Дело</span>.")
//		assertEquals expected, tagged.tagged
//	}

	
	@Test
	public void testSemantic() {
		def expected=
"""<sentence>
  <tokenReading>
    <token value="Слово" lemma="слово" tags="noun:inanim:n:v_naz" semtags="1:conc:speech:2:abst:speech:3:conc:text" />
    <token value="Слово" lemma="слово" tags="noun:inanim:n:v_zna" semtags="1:conc:speech:2:abst:speech:3:conc:text" />
  </tokenReading>
  <tokenReading>
    <token value="усе" lemma="усе" tags="adv" semtags="1:dur:max" />
    <token value="усе" lemma="усе" tags="conj:coord" />
    <token value="усе" lemma="усе" tags="noun:inanim:n:v_naz:&amp;pron:gen" semtags="1:quantif" />
    <token value="усе" lemma="усе" tags="noun:inanim:n:v_zna:&amp;pron:gen" semtags="1:quantif" />
    <token value="усе" lemma="усе" tags="part" />
    <token value="усе" lemma="увесь" tags="adj:n:v_naz:&amp;pron:gen" semtags="1:quantif" />
    <token value="усе" lemma="увесь" tags="adj:n:v_zna:&amp;pron:gen" semtags="1:quantif" />
  </tokenReading>
  <tokenReading>
    <token value="голова" lemma="голова" tags="noun:anim:f:v_naz" semtags="1:conc:hum&amp;hierar" />
    <token value="голова" lemma="голова" tags="noun:anim:m:v_naz" semtags="1:conc:hum&amp;hierar" />
    <token value="голова" lemma="голова" tags="noun:inanim:f:v_naz" semtags="1:conc:body:part:2:abst:ment:3:abst:unit" />
  </tokenReading>
  <tokenReading>
    <token value="аахенська" lemma="аахенський" tags="adj:f:v_kly" semtags="1:abst" />
    <token value="аахенська" lemma="аахенський" tags="adj:f:v_naz" semtags="1:abst" />
  </tokenReading>
  <tokenReading>
    <token value="Вашингтон" lemma="Вашингтон" tags="noun:anim:m:v_naz:prop:lname" semtags="1:conc:hum" />
    <token value="Вашингтон" lemma="Вашингтон" tags="noun:inanim:m:v_naz:prop:geo:xp1" semtags="1:conc:loc" />
    <token value="Вашингтон" lemma="Вашингтон" tags="noun:inanim:m:v_naz:prop:geo:xp2" semtags="1:conc:loc" />
    <token value="Вашингтон" lemma="Вашингтон" tags="noun:inanim:m:v_zna:prop:geo:xp1" semtags="1:conc:loc" />
    <token value="Вашингтон" lemma="Вашингтон" tags="noun:inanim:m:v_zna:prop:geo:xp2" semtags="1:conc:loc" />
  </tokenReading>
  <tokenReading>
    <token value="акту" lemma="акт" tags="noun:inanim:m:v_dav:xp1" semtags="1:conc:text" />
    <token value="акту" lemma="акт" tags="noun:inanim:m:v_dav:xp2" semtags="1:abst:part:2:abst:quantum" />
    <token value="акту" lemma="акт" tags="noun:inanim:m:v_rod:xp2" semtags="1:abst:part:2:abst:quantum" />
  </tokenReading>
  <tokenReading>
    <token value="." lemma="." tags="punct" whitespaceBefore="false" />
  </tokenReading>
</sentence>
"""

		tagText.setOptions(new TagOptions(semanticTags: true, xmlOutput: true))
		TagResult tagged = tagText.tagText("Слово усе голова аахенська Вашингтон акту.")
		assertEquals expected, tagged.tagged
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

		
		tagText.setOptions(new TagOptions(xmlOutput: true, input: file.path, output: outFile.path))
		
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
    <token value="." lemma="." tags="punct" whitespaceBefore="false" />
  </tokenReading>
</sentence>

<sentence>
  <tokenReading>
    <token value="Діло" lemma="діло" tags="noun:inanim:n:v_naz" />
    <token value="Діло" lemma="діло" tags="noun:inanim:n:v_zna" />
  </tokenReading>
  <tokenReading>
    <token value="'" lemma="'" tags="punct" whitespaceBefore="false" />
  </tokenReading>
  <tokenReading>
    <token value="." lemma="." tags="punct" whitespaceBefore="false" />
  </tokenReading>
</sentence>

<sentence>
  <tokenReading>
    <token value="Мабуть" lemma="мабуть" tags="adv:&amp;insert" />
  </tokenReading>
  <tokenReading>
    <token value="кх" lemma="" tags="unknown" />
  </tokenReading>
  <tokenReading>
    <token value="." lemma="." tags="punct" whitespaceBefore="false" />
  </tokenReading>
</sentence>


</text>
"""
		assertEquals expected, outFile.getText("UTF-8")
	}

	
	@Test
	public void testTxtParallel() {
		
		File file = File.createTempFile("tag_input",".tmp")
		file.deleteOnExit()
		file.setText("Слово.\n\nДіло.\n\nШвидко.\n\n", "UTF-8")

		File outFile = File.createTempFile("tag_output",".tmp")
		outFile.deleteOnExit()
		outFile.text = ''

		
		tagText.setOptions(new TagOptions(input: file.path, output: outFile.path, outputFormat: OutputFormat.txt))
		
		tagText.process()

		def expected =
"""Слово[слово/noun:inanim:n:v_naz,слово/noun:inanim:n:v_zna].<P/> 
Діло[діло/noun:inanim:n:v_naz,діло/noun:inanim:n:v_zna].<P/> 
Швидко[швидко/adv:compb].<P/> """
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
    

	@Test
	public void testJsonParallel() {
		
		File file = File.createTempFile("tag_input",".tmp")
		file.deleteOnExit()
		file.setText("Слово X.\n\nДіло\".Швидко.\n\n", "UTF-8")

		File outFile = File.createTempFile("tag_output",".tmp")
		outFile.deleteOnExit()
		outFile.text = ''

		
		tagText.setOptions(new TagOptions(outputFormat: json, input: file.path, output: outFile.path, singleThread: false))
		
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
            {"value":".","whitespaceBefore":false,"tags":"punct","lemma":"."}]}]},
  {"tokenReadings":[{"tokens":[
            {"value":"Діло","tags":"noun:inanim:n:v_naz","lemma":"діло"},
            {"value":"Діло","tags":"noun:inanim:n:v_zna","lemma":"діло"}]},
        {"tokens":[
            {"value":"\\"","whitespaceBefore":false,"tags":"punct","lemma":"\\""}]},
        {"tokens":[
            {"value":".","whitespaceBefore":false,"tags":"punct","lemma":"."}]}]},
  {"tokenReadings":[{"tokens":[
            {"value":"Швидко","tags":"adv:compb","lemma":"швидко"}]},
        {"tokens":[
            {"value":".","whitespaceBefore":false,"tags":"punct","lemma":"."}]}]}
  ]
}
""")

		assertEquals expected, new JsonSlurper().parseText(outFile.getText("UTF-8"))
	}

}