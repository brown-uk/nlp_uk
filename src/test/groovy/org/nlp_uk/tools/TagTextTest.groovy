#!/bin/env groovy

package org.nlp_uk.tools

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test


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
		def tagged = tagText.tagText("Слово.")
		assertEquals "Слово[слово/noun:inanim:n:v_naz,слово/noun:inanim:n:v_zna].[</S><P/>]\n", tagged
	}
	

	@Test
	public void testXml() {
		tagText.setOptions(["xmlOutput": true])

		def tagged = tagText.tagText("Слово.")
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
		assertEquals expected, tagged
	}

	
	@Test
	public void testForeign() {
		tagText.setOptions(["xmlOutput": true])

		def tagged = tagText.tagText("Crow")
		def expected =
"""<sentence>
  <tokenReading>
    <token value='Crow' tags='noninfl:foreign' />
  </tokenReading>
</sentence>

"""
		assertEquals expected, tagged
	}


	@Disabled
	@Test
	public void testIgnoreLang() {
		tagText.setOptions(["ignoreOtherLanguages": true, "xmlOutput": true])

		def tagged = tagText.tagText("<span lang='ru'>Слво.</span>")
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
		assertEquals expected, tagged
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
		def tagged = tagText.tagText("Слово.")
		assertEquals expected, tagged
	}
}



