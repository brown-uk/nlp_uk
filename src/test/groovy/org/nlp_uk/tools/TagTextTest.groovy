#!/bin/env groovy

package org.nlp_uk.tools

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.nlp_uk.tools.TagText.TagResult


class TagTextTest {
	def options = [ : ]

	TagText tagText = new TagText()
	
	@BeforeEach
	void before() {
		tagText.setOptions(options)
	}

	def file() { return new File("/dev/null") }


	@Test
	public void test() {
		TagResult tagged = tagText.tagText("Слово.")
		def expected = "Слово[слово/noun:inanim:n:v_naz,слово/noun:inanim:n:v_zna].[</S><P/>]\n"
		assertEquals expected, tagged.tagged
	}
	

	@Test
	public void testXml() {
		tagText.setOptions(["xmlOutput": true])

		TagResult tagged = tagText.tagText("Слово.")
		def expected =
"""<sentence>
  <tokenReading>
    <token value='Слово' lemma='слово' tags='noun:inanim:n:v_naz' />
    <token value='Слово' lemma='слово' tags='noun:inanim:n:v_zna' />
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
		tagText.setOptions(["xmlOutput": true])

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
		tagText.setOptions(["ignoreOtherLanguages": true, "xmlOutput": true])

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
    <token value='.' tags='punct' whitespaceBefore='false' />
  </tokenReading>
</sentence>

"""
		
		tagText.setOptions(["semanticTags": true, "xmlOutput": true] )
		TagResult tagged = tagText.tagText("Слово.")
		assertEquals expected, tagged.tagged
	}


	@Test
	public void testStats() {
		tagText.setOptions(["unknownStats": true, "output": "-"])

		TagResult tagged = tagText.tagText("десь брарарат")

		assertEquals 1, tagged.stats.knownCnt
		assertEquals 1, tagged.stats.unknownMap.values().sum()
		
		tagged.stats.printUnknownStats()
	}

}


