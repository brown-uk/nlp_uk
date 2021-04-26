#!/bin/env groovy

package org.nlp_uk.tools

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.nlp_uk.tools.TagText.TagOptions
import org.nlp_uk.tools.TagText.TagResult


class TagTextTest {
	def options = new TagOptions()

	TagText tagText = new TagText()
	
	@BeforeEach
	void before() {
		tagText.setOptions(options)
	}

	def file() { return new File("/dev/null") }


	@Test
	public void test() {
		TagResult tagged = tagText.tagText("Слово.")
		def expected = "Слово[слово/noun:inanim:n:v_naz,слово/noun:inanim:n:v_zna].[</S><P/>]"
		assertEquals expected, tagged.tagged
	}
	

	@Test
	public void testXml() {
		tagText.setOptions(new TagOptions(xmlOutput: true))

		TagResult tagged = tagText.tagText("Слово 1,5 раза.")

		def expected =
"""<sentence>
  <tokenReading>
    <token value='Слово' lemma='слово' tags='noun:inanim:n:v_naz' />
    <token value='Слово' lemma='слово' tags='noun:inanim:n:v_zna' />
  </tokenReading>
  <tokenReading>
    <token value='1,5' lemma='1,5' tags='number' />
  </tokenReading>
  <tokenReading>
    <token value='раза' lemma='раз' tags='noun:inanim:m:v_rod' />
  </tokenReading>
  <tokenReading>
    <token value='.' tags='punct' whitespaceBefore='false' />
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
    <token value='На' lemma='на' tags='prep' />
  </tokenReading>
  <tokenReading>
    <token value='Україна' lemma='Україна' tags='noun:inanim:f:v_naz:prop:geo' />
    <token value='Україна' lemma='Україна' tags='&lt;prop_noun&gt;' />
  </tokenReading>
  <tokenReading>
    <token value='—' tags='punct' whitespaceBefore='true' />
  </tokenReading>
  <tokenReading>
    <token value='Іспанія' lemma='Іспанія' tags='noun:inanim:f:v_naz:prop:geo' />
    <token value='Іспанія' lemma='Іспанія' tags='&lt;/prop_noun&gt;' />
  </tokenReading>
  <tokenReading>
    <token value='.' tags='punct' whitespaceBefore='false' />
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
    <token value='Crow' tags='noninfl:foreign' />
  </tokenReading>
</sentence>
"""
		assertEquals expected, tagged.tagged
	}


	@Disabled
	@Test
	public void testIgnoreLang() {
		tagText.setOptions(new TagOptions(ignoreOtherLanguages: true, xmlOutput: true))

		TagResult tagged = tagText.tagText("<span lang='ru'>Слво.</span>")
		assertEquals "<foreign>Слво.</foreign>\n\n", tagged

		def expected=
"""<sentence>
  <tokenReading>
    <token value='Газета' lemma='газета' tags='noun:inanim:f:v_naz' />
  </tokenReading>
  <foreign>Дело</foreign>
  <tokenReading>
    <token value='.' tags='punct' whitespaceBefore='false' />
  </tokenReading>
</sentence>

"""
		
		tagged = tagText.tagText("Газета <span lang='ru'>Дело</span>.")
		assertEquals expected, tagged.tagged
	}

	
	@Test
	public void testSemantic() {
		def expected=
"""<sentence>
  <tokenReading>
    <token value='Слово' lemma='слово' tags='noun:inanim:n:v_naz' semtags='1:conc:speech:2:abst:speech:3:conc:text' />
    <token value='Слово' lemma='слово' tags='noun:inanim:n:v_zna' semtags='1:conc:speech:2:abst:speech:3:conc:text' />
  </tokenReading>
  <tokenReading>
    <token value='усе' lemma='усе' tags='adv' semtags='1:dur:max' />
    <token value='усе' lemma='усе' tags='conj:coord' />
    <token value='усе' lemma='усе' tags='noun:inanim:n:v_naz:&amp;pron:gen' />
    <token value='усе' lemma='усе' tags='noun:inanim:n:v_zna:&amp;pron:gen' />
    <token value='усе' lemma='усе' tags='part' />
    <token value='усе' lemma='увесь' tags='adj:n:v_naz:&amp;pron:gen' semtags='1:quantit:max' />
    <token value='усе' lemma='увесь' tags='adj:n:v_zna:&amp;pron:gen' semtags='1:quantit:max' />
  </tokenReading>
  <tokenReading>
    <token value='голова' lemma='голова' tags='noun:anim:f:v_naz:xp1' semtags='1:conc:hum&amp;hierar' />
    <token value='голова' lemma='голова' tags='noun:anim:m:v_naz:xp1' semtags='1:conc:hum&amp;hierar' />
    <token value='голова' lemma='голова' tags='noun:inanim:f:v_naz:xp2' semtags='1:conc:body:part:2:abst:ment:3:abst:unit' />
  </tokenReading>
  <tokenReading>
    <token value='.' tags='punct' whitespaceBefore='false' />
  </tokenReading>
</sentence>
"""
		
		tagText.setOptions(new TagOptions(semanticTags: true, xmlOutput: true))
		TagResult tagged = tagText.tagText("Слово усе голова.")
		assertEquals expected, tagged.tagged
	}


	@Test
	public void testStats() {
		tagText.setOptions(new TagOptions(unknownStats: true, output: "-"))

		TagResult tagged = tagText.tagText("десь брарарат")

		assertEquals 1, tagged.stats.knownCnt
		assertEquals 1, tagged.stats.unknownMap.values().sum()
		
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
    <token value='Слово' lemma='слово' tags='noun:inanim:n:v_naz' />
    <token value='Слово' lemma='слово' tags='noun:inanim:n:v_zna' />
  </tokenReading>
  <tokenReading>
    <token value='X' lemma='X' tags='number:latin' />
  </tokenReading>
  <tokenReading>
    <token value='.' tags='punct' whitespaceBefore='false' />
  </tokenReading>
</sentence>

<sentence>
  <tokenReading>
    <token value='Діло' lemma='діло' tags='noun:inanim:n:v_naz' />
    <token value='Діло' lemma='діло' tags='noun:inanim:n:v_zna' />
  </tokenReading>
  <tokenReading>
    <token value='&apos;' tags='punct' whitespaceBefore='false' />
  </tokenReading>
  <tokenReading>
    <token value='.' tags='punct' whitespaceBefore='false' />
  </tokenReading>
</sentence>

<sentence>
  <tokenReading>
    <token value='Мабуть' lemma='мабуть' tags='adv:&amp;insert' />
  </tokenReading>
  <tokenReading>
    <token value='кх' lemma='' />
  </tokenReading>
  <tokenReading>
    <token value='.' tags='punct' whitespaceBefore='false' />
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

		
		tagText.setOptions(new TagOptions(input: file.path, output: outFile.path))
		
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
            { "value": ".", "tags": "punct", "whitespaceBefore": false }
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
            { "value": "\\\"", "tags": "punct", "whitespaceBefore": false }
          ]
        },
        {
          "tokens": [
            { "value": ".", "tags": "punct", "whitespaceBefore": false }
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
            { "value": ".", "tags": "punct", "whitespaceBefore": false }
          ]
        }
      ]
    }
  ]
}
"""
		assertEquals expected, outFile.getText("UTF-8")
	}

}



