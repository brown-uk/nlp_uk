#!/bin/env groovy

package org.nlp_uk.tools

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.nlp_uk.tools.TagText.OutputFormat
import org.nlp_uk.tools.TagText.TagOptions
import org.nlp_uk.tools.TagText.TagResult


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
        // def orig = "Де[де/adv:&pron:int:rel,де/noninfl:foreign,де/part,де можна/<adv>] можна[можна/noninfl:&predic,</S>де можна/<adv>,<P/>]"
        def expected = "Де[де/adv:&pron:int:rel,де/noninfl:foreign,де/part] можна[можна/noninfl:&predic,</S><P/>]"
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
	public void testForeign() {
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
	public void testJsonParallel() {
		
		File file = File.createTempFile("tag_input",".tmp")
		file.deleteOnExit()
		file.setText("Слово X.\n\nДіло\".Швидко.\n\n", "UTF-8")

		File outFile = File.createTempFile("tag_output",".tmp")
		outFile.deleteOnExit()
		outFile.text = ''

		
		tagText.setOptions(new TagOptions(outputFormat: "json", input: file.path, output: outFile.path, singleThread: false))
		
		tagText.process()

		def expected =
"""{
  "sentences": [
    {
      "tokenReadings": [
        {
          "tokens": [
            { "value": "Слово", "lemma": "слово", "tags": "noun:inanim:n:v_naz" },
            { "value": "Слово", "lemma": "слово", "tags": "noun:inanim:n:v_zna" }
          ]
        },
        {
          "tokens": [
            { "value": "X", "lemma": "X", "tags": "number:latin" }
          ]
        },
        {
          "tokens": [
            { "value": ".", "lemma": ".", "tags": "punct", "whitespaceBefore": false }
          ]
        }
      ]
    },
    {
      "tokenReadings": [
        {
          "tokens": [
            { "value": "Діло", "lemma": "діло", "tags": "noun:inanim:n:v_naz" },
            { "value": "Діло", "lemma": "діло", "tags": "noun:inanim:n:v_zna" }
          ]
        },
        {
          "tokens": [
            { "value": "\\\"", "lemma": "\\\"", "tags": "punct", "whitespaceBefore": false }
          ]
        },
        {
          "tokens": [
            { "value": ".", "lemma": ".", "tags": "punct", "whitespaceBefore": false }
          ]
        }
      ]
    },
    {
      "tokenReadings": [
        {
          "tokens": [
            { "value": "Швидко", "lemma": "швидко", "tags": "adv:compb" }
          ]
        },
        {
          "tokens": [
            { "value": ".", "lemma": ".", "tags": "punct", "whitespaceBefore": false }
          ]
        }
      ]
    }
  ]
}
"""
		assertEquals expected, outFile.getText("UTF-8")
	}

    @Test
    public void testDisambigStats() {
        tagText.setOptions(new TagOptions(xmlOutput: true, disambiguateByStats: true))

        TagResult tagged = tagText.tagText("а")

        def expected =
"""<sentence>
  <tokenReading>
    <token value="а" lemma="а" tags="conj:coord" />
    <token value="а" lemma="а" tags="part" />
    <token value="а" lemma="а" tags="intj" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged

        TagResult tagged2 = tagText.tagText("Тому")
        
                def expected2 =
"""<sentence>
  <tokenReading>
    <token value="Тому" lemma="тому" tags="adv" />
    <token value="Тому" lemma="тому" tags="conj:subord" />
    <token value="Тому" lemma="той" tags="adj:m:v_dav:&amp;pron:dem" />
    <token value="Тому" lemma="Тома" tags="noun:anim:m:v_zna:prop:fname" />
    <token value="Тому" lemma="Том" tags="noun:anim:m:v_dav:prop:fname" />
    <token value="Тому" lemma="те" tags="noun:inanim:n:v_dav:&amp;pron:dem" />
    <token value="Тому" lemma="той" tags="adj:n:v_dav:&amp;pron:dem" />
  </tokenReading>
</sentence>
"""
        assertEquals expected2, tagged2.tagged
        
    }

    
    @Test
    public void testDisambigStatsSingleTokenFormat() {
        tagText.setOptions(new TagOptions(xmlOutput: true, disambiguateByStats: true, singleTokenFormat: true))

        TagResult tagged = tagText.tagText("а")

        def expected =
"""<sentence>
  <token value="а" lemma="а" tags="conj:coord" q="0.993">
    <alts>
      <token value="а" lemma="а" tags="part" q="0.006" />
      <token value="а" lemma="а" tags="intj" q="0" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged

        TagResult tagged2 = tagText.tagText("А")

        def expected2 =
"""<sentence>
  <token value="А" lemma="а" tags="conj:coord" q="0.758">
    <alts>
      <token value="А" lemma="а" tags="part" q="0.239" />
      <token value="А" lemma="а" tags="intj" q="0.002" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
        assertEquals expected2, tagged2.tagged
    }

    
    @Test
    public void testDisambigStatsSingleTokenFormatWithCtx() {
        tagText.setOptions(new TagOptions(xmlOutput: true, disambiguateByStats: true, singleTokenFormat: true))

//        TagResult tagged = tagText.tagText("на нього")

        def expected =
"""<sentence>
  <token value="на" lemma="на" tags="prep" />
  <token value="нього" lemma="він" tags="noun:unanim:m:v_rod:&amp;pron:pers:3" q="0.56">
    <alts>
      <token value="нього" lemma="він" tags="noun:unanim:m:v_zna:&amp;pron:pers:3" q="0.256" />
      <token value="нього" lemma="воно" tags="noun:unanim:n:v_rod:&amp;pron:pers:3" q="0.14" />
      <token value="нього" lemma="воно" tags="noun:unanim:n:v_zna:&amp;pron:pers:3" q="0.042" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
//        assertEquals expected, tagged.tagged
        
        TagResult tagged2 = tagText.tagText("в окремих")
        
        def expected2 =
"""<sentence>
  <token value="в" lemma="в" tags="prep" />
  <token value="окремих" lemma="окремий" tags="adj:p:v_rod" q="0.695">
    <alts>
      <token value="окремих" lemma="окремий" tags="adj:p:v_mis" q="0.304" />
      <token value="окремих" lemma="окремий" tags="adj:p:v_zna:ranim" q="0" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
                assertEquals expected2, tagged2.tagged
        
    }
        

    @Test
    public void testFirstToken() {
        tagText.setOptions(new TagOptions(xmlOutput: true, singleTokenFormat: true))

        TagResult tagged = tagText.tagText("а")

        def expected =
"""<sentence>
  <token value="а" lemma="а" tags="conj:coord">
    <alts>
      <token value="а" lemma="а" tags="intj" />
      <token value="а" lemma="а" tags="part" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }

    @Test
    public void testDisambigStatsFirstTokenOnly() {
        tagText.setOptions(new TagOptions(xmlOutput: true, disambiguateByStats: true, singleTokenOnly: true))

        TagResult tagged = tagText.tagText("відлетіла")

        def expected =
"""<sentence>
  <token value="відлетіла" lemma="відлетіти" tags="verb:perf:past:f" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
        
        def expected2 =
"""<sentence>
  <token value="маркітні" lemma="маркітний" tags="adj:p:v_naz" />
</sentence>
<paragraph/>
"""
        tagged = tagText.tagText("маркітні")

        assertEquals expected2, tagged.tagged

        def expected3 =
"""<sentence>
  <token value="Заняття" lemma="заняття" tags="noun:inanim:n:v_naz:xp1" />
</sentence>
<paragraph/>
"""
        tagged = tagText.tagText("Заняття")

        assertEquals expected3, tagged.tagged

        def expected4 =
"""<sentence>
  <token value="Чорні" lemma="чорний" tags="adj:p:v_naz:compb" />
</sentence>
<paragraph/>
"""
        tagged = tagText.tagText("Чорні")

        assertEquals expected4, tagged.tagged
    }
}



