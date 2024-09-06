#!/bin/env groovy

package ua.net.nlp.tools.tag

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse

import org.apache.commons.text.StringEscapeUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

import groovy.json.JsonSlurper
import ua.net.nlp.tools.TextUtils.OutputFormat
import ua.net.nlp.tools.tag.TagOptions
import ua.net.nlp.tools.tag.TagStats
import ua.net.nlp.tools.tag.TagTextCore
import ua.net.nlp.tools.tag.TagTextCore.TTR
import ua.net.nlp.tools.tag.TagTextCore.TagResult


class TagTextVerticalOutputTest {
	def options = new TagOptions()

	static TagTextCore tagText = new TagTextCore()
	
	@BeforeEach
	void before() {
		tagText.setOptions(options)
	}

	def file() { return new File("/dev/null") }


	@Test
	public void testTxtFormat() {
        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.vertical))
		TagResult tagged = tagText.tagText("Десь, там за горою ходила Галя. А далі - озеро...")

        def expected = 
"""<s>
Десь\tadv:&pron:ind\tдесь
<g/>
,\tpunct\t,
там\tadv:&pron:dem\tтам
за\tprep\tза
горою\tadv\tгорою
ходила\tverb:imperf:past:f\tходити
Галя\tnoun:anim:f:v_naz:prop:fname\tГаля
<g/>
.\tpunct\t.
</s>

<s>
А\tconj:coord\tа
далі\tadv:compc:&predic\tдалі
-\tpunct\t-
озеро\tnoun:inanim:n:v_naz\tозеро
<g/>
...\tpunct\t...
</s>
"""

		assertEquals expected, tagged.tagged
	}

    @Test
    public void testTxtFormatWithSemantic() {
        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.vertical, semanticTags: true))
        TagResult tagged = tagText.tagText("А далі - озеро...")

        def expected =
"""<s>
А\tconj:coord\tа\t
далі\tadv:compc:&predic\tдалі\t1:dist:2:time
-\tpunct\t-\t
озеро\tnoun:inanim:n:v_naz\tозеро\t
<g/>
...\tpunct\t...\t
</s>
"""

        assertEquals expected, tagged.tagged
    }

    
}
