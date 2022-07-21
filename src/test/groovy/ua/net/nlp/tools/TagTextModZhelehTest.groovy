#!/bin/env groovy

package ua.net.nlp.tools

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import ua.net.nlp.tools.tag.TagOptions
import ua.net.nlp.tools.tag.TagTextCore
import ua.net.nlp.tools.tag.TagTextCore.TagResult


class TagTextModZhelehTest {

	static TagTextCore tagText = new TagTextCore()
	
	@BeforeEach
	void before() {
        tagText.setOptions(new TagOptions(xmlOutput: true, modules: ["zheleh"]))
	}


    @Test
    public void testZheleh() {

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

    @Test
    public void testZheleh2() {

        TagResult tagged = tagText.tagText("називати ся")
        def expected =
"""<sentence>
  <tokenReading>
    <token value="називати" lemma="називатися" tags="verb:rev:imperf:inf" />
  </tokenReading>
  <tokenReading>
    <token value="ся" lemma="ся" tags="part:arch" />
    <token value="ся" lemma="сей" tags="adj:f:v_naz:&amp;pron:dem:arch" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
    }
}