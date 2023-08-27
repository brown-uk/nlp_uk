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
        tagText.setOptions(new TagOptions(modules: ["zheleh"]))
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

    
    @Test
    public void testZhelehExtraWords() {

        TagResult tagged = tagText.tagText("мїжь")
        def expected =
"""<sentence>
  <tokenReading>
    <token value="мїжь" lemma="мїжь" tags="prep" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
    }

    
    @Test
    public void testZhelehSubs() {

        TagResult tagged = tagText.tagText("польованє")
        def expected =
"""<sentence>
  <tokenReading>
    <token value="польованє" lemma="полювання" tags="noun:inanim:n:v_kly" />
    <token value="польованє" lemma="полювання" tags="noun:inanim:n:v_naz" />
    <token value="польованє" lemma="полювання" tags="noun:inanim:n:v_rod" />
    <token value="польованє" lemma="полювання" tags="noun:inanim:n:v_zna" />
    <token value="польованє" lemma="полювання" tags="noun:inanim:p:v_kly" />
    <token value="польованє" lemma="полювання" tags="noun:inanim:p:v_naz" />
    <token value="польованє" lemma="полювання" tags="noun:inanim:p:v_zna" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("пізнїйше")
        expected =
"""<sentence>
  <tokenReading>
    <token value="пізнїйше" lemma="пізніше" tags="adv:compc:&amp;predic" />
    <token value="пізнїйше" lemma="пізніше" tags="prep" />
    <token value="пізнїйше" lemma="пізніший" tags="adj:n:v_kly:compc" />
    <token value="пізнїйше" lemma="пізніший" tags="adj:n:v_naz:compc" />
    <token value="пізнїйше" lemma="пізніший" tags="adj:n:v_zna:compc" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("йім")
        expected =
"""<sentence>
  <tokenReading>
    <token value="йім" lemma="їсти" tags="verb:imperf:pres:s:1" />
    <token value="йім" lemma="вони" tags="noun:unanim:p:v_dav:&amp;pron:pers:3" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged

        assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("Поднїпровя")
        expected =
"""<sentence>
  <tokenReading>
    <token value="Поднїпровя" lemma="Подніпров'я" tags="noun:inanim:n:v_kly:prop:geo" />
    <token value="Поднїпровя" lemma="Подніпров'я" tags="noun:inanim:n:v_naz:prop:geo" />
    <token value="Поднїпровя" lemma="Подніпров'я" tags="noun:inanim:n:v_rod:prop:geo" />
    <token value="Поднїпровя" lemma="Подніпров'я" tags="noun:inanim:n:v_zna:prop:geo" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged

        tagged = tagText.tagText("літопись")
        expected =
"""<sentence>
  <tokenReading>
    <token value="літопись" lemma="літопис" tags="noun:inanim:m:v_naz" />
    <token value="літопись" lemma="літопис" tags="noun:inanim:m:v_zna" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("розпорядженєм")
        expected =
"""<sentence>
  <tokenReading>
    <token value="розпорядженєм" lemma="розпорядження" tags="noun:inanim:n:v_oru" />
    <token value="розпорядженєм" lemma="розпорядження" tags="noun:inanim:p:v_dav" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("Італїйцї")
        expected =
"""<sentence>
  <tokenReading>
    <token value="Італїйцї" lemma="італійка" tags="noun:anim:f:v_dav" />
    <token value="Італїйцї" lemma="італійка" tags="noun:anim:f:v_mis" />
    <token value="Італїйцї" lemma="італієць" tags="noun:anim:m:v_mis" />
    <token value="Італїйцї" lemma="італієць" tags="noun:anim:p:v_kly" />
    <token value="Італїйцї" lemma="італієць" tags="noun:anim:p:v_naz" />
    <token value="Італїйцї" lemma="італієць" tags="noun:anim:p:v_zna:rare" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("засїданя")
        expected =
"""<sentence>
  <tokenReading>
    <token value="засїданя" lemma="засідання" tags="noun:inanim:n:v_kly" />
    <token value="засїданя" lemma="засідання" tags="noun:inanim:n:v_naz" />
    <token value="засїданя" lemma="засідання" tags="noun:inanim:n:v_rod" />
    <token value="засїданя" lemma="засідання" tags="noun:inanim:n:v_zna" />
    <token value="засїданя" lemma="засідання" tags="noun:inanim:p:v_kly" />
    <token value="засїданя" lemma="засідання" tags="noun:inanim:p:v_naz" />
    <token value="засїданя" lemma="засідання" tags="noun:inanim:p:v_zna" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
    }

    @Test
    public void testZhelehSubsMultitag() {

        TagResult tagged = tagText.tagText("А-ле")
        def expected =
"""<sentence>
  <tokenReading>
    <token value="А-ле" lemma="а-ле" tags="conj:coord" />
    <token value="А-ле" lemma="а-ле" tags="intj" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
    }
}