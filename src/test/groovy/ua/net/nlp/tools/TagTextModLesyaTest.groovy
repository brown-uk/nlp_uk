#!/bin/env groovy

package ua.net.nlp.tools

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import ua.net.nlp.tools.tag.TagOptions
import ua.net.nlp.tools.tag.TagTextCore
import ua.net.nlp.tools.tag.TagTextCore.TagResult


class TagTextModLesyaTest {
	def options = new TagOptions()

	static TagTextCore tagText = new TagTextCore()
	
	@BeforeEach
	void before() {
        tagText.setOptions(new TagOptions(unknownStats: true, modules: ["lesya"]))
	}

	def file() { return new File("/dev/null") }



    @Test
    public void testLesyaOrphograph() {

        TagResult tagged = tagText.tagText("звичайі нашоі націі")
        def expected =
"""<sentence>
  <tokenReading>
    <token value="звичайі" lemma="звичай" tags="noun:inanim:m:v_mis:alt" />
    <token value="звичайі" lemma="звичай" tags="noun:inanim:p:v_kly:alt" />
    <token value="звичайі" lemma="звичай" tags="noun:inanim:p:v_naz:alt" />
    <token value="звичайі" lemma="звичай" tags="noun:inanim:p:v_zna:alt" />
  </tokenReading>
  <tokenReading>
    <token value="нашоі" lemma="наш" tags="adj:f:v_rod:&amp;pron:pos:alt" />
  </tokenReading>
  <tokenReading>
    <token value="націі" lemma="нація" tags="noun:inanim:f:v_dav:alt" />
    <token value="націі" lemma="нація" tags="noun:inanim:f:v_mis:alt" />
    <token value="націі" lemma="нація" tags="noun:inanim:f:v_rod:alt" />
    <token value="націі" lemma="нація" tags="noun:inanim:p:v_kly:alt" />
    <token value="націі" lemma="нація" tags="noun:inanim:p:v_naz:alt" />
    <token value="націі" lemma="нація" tags="noun:inanim:p:v_zna:alt" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
        
        assertEquals "[:]", tagged.stats.unknownMap.toString()
    }
    
    @Test
    public void testLesyaOrphoepic() {

        TagResult tagged = tagText.tagText("завжді")
        def expected =
"""<sentence>
  <tokenReading>
    <token value="завжді" lemma="завжди" tags="adv:&amp;pron:gen:alt" />
  </tokenReading>
</sentence>
"""

        tagged = tagText.tagText("идолянин")
        expected =
"""<sentence>
  <tokenReading>
    <token value="идолянин" lemma="ідолянин" tags="noun:anim:m:v_naz:alt" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("історічний")
        expected =
"""<sentence>
  <tokenReading>
    <token value="історічний" lemma="історичний" tags="adj:m:v_kly:alt" />
    <token value="історічний" lemma="історичний" tags="adj:m:v_naz:alt" />
    <token value="історічний" lemma="історичний" tags="adj:m:v_zna:rinanim:alt" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged

        tagged = tagText.tagText("лікарь")
        expected =
"""<sentence>
  <tokenReading>
    <token value="лікарь" lemma="лікар" tags="noun:anim:m:v_naz:alt" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("головнійше")
        expected =
"""<sentence>
  <tokenReading>
    <token value="головнійше" lemma="головніший" tags="adj:n:v_kly:compc:alt" />
    <token value="головнійше" lemma="головніший" tags="adj:n:v_naz:compc:alt" />
    <token value="головнійше" lemma="головніший" tags="adj:n:v_zna:compc:alt" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("остілько")
        expected =
"""<sentence>
  <tokenReading>
    <token value="остілько" lemma="остільки" tags="adv:alt" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("чорниї")
        expected =
"""<sentence>
  <tokenReading>
    <token value="чорниї" lemma="чорний" tags="adj:p:v_kly:compb:long:alt" />
    <token value="чорниї" lemma="чорний" tags="adj:p:v_naz:compb:long:alt" />
    <token value="чорниї" lemma="чорний" tags="adj:p:v_zna:rinanim:compb:long:alt" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
    }

    @Test
    public void testLesyaGram() {

        TagResult tagged = tagText.tagText("християне")
        def expected =
"""<sentence>
  <tokenReading>
    <token value="християне" lemma="християнин" tags="noun:anim:p:v_kly:alt" />
    <token value="християне" lemma="християнин" tags="noun:anim:p:v_naz:alt" />
    <token value="християне" lemma="християнин" tags="noun:anim:p:v_zna:rare:alt" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged

        tagged = tagText.tagText("прикростів")
        expected =
"""<sentence>
  <tokenReading>
    <token value="прикростів" lemma="прикрість" tags="noun:inanim:p:v_rod:alt" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged

        tagged = tagText.tagText("фабрикантови")
        expected =
"""<sentence>
  <tokenReading>
    <token value="фабрикантови" lemma="фабрикант" tags="noun:anim:m:v_dav:alt" />
    <token value="фабрикантови" lemma="фабрикант" tags="noun:anim:m:v_mis:alt" />
    <token value="фабрикантови" lemma="фабрикантів" tags="adj:p:v_kly:alt" />
    <token value="фабрикантови" lemma="фабрикантів" tags="adj:p:v_naz:alt" />
    <token value="фабрикантови" lemma="фабрикантів" tags="adj:p:v_zna:rinanim:alt" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged

        tagged = tagText.tagText("завоюваннів")
        expected =
"""<sentence>
  <tokenReading>
    <token value="завоюваннів" lemma="завоювання" tags="noun:inanim:p:v_rod:alt" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
    }
    
    
    @Test
    public void testLesyaAvRod() {

        TagResult tagged = tagText.tagText("всесвіта")
        def expected =
"""<sentence>
  <tokenReading>
    <token value="всесвіта" lemma="всесвіт" tags="noun:inanim:m:v_dav:alt" />
    <token value="всесвіта" lemma="всесвіт" tags="noun:inanim:m:v_mis:alt" />
    <token value="всесвіта" lemma="всесвіт" tags="noun:inanim:m:v_rod:alt" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
    }
}
