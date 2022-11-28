#!/bin/env groovy

package ua.net.nlp.tools

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import ua.net.nlp.tools.tag.TagOptions
import ua.net.nlp.tools.tag.TagTextCore
import ua.net.nlp.tools.tag.TagTextCore.TagResult


public class TagTextUnknownTest {
	def options = new TagOptions()

	static TagTextCore tagText = new TagTextCore()
	
	@BeforeAll
	static void before() {
        tagText.setOptions(new TagOptions(tagUnknown: true, disambiguate: true, setLemmaForUnknown: true))
	}


    @Test
    public void testAdjNoun() {

        TagResult tagged = tagText.tagText("адюльтерівськими")
        
        def expected =
"""<sentence>
  <tokenReading>
    <token value="адюльтерівськими" lemma="адюльтерівський" tags="adj:p:v_oru" q="-0.6" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged

        tagged = tagText.tagText("аквамарафон")
                
        expected =
"""<sentence>
  <tokenReading>
    <token value="аквамарафон" lemma="аквамарафон" tags="noun:inanim:m:v_zna" q="-0.5" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged

        tagged = tagText.tagText("Біоміметикою")
                
        expected =
"""<sentence>
  <tokenReading>
    <token value="Біоміметикою" lemma="біоміметика" tags="noun:inanim:f:v_oru" q="-0.5" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
        
        
        tagged = tagText.tagText("дентиносупергенезом")
                
        expected =
"""<sentence>
  <tokenReading>
    <token value="дентиносупергенезом" lemma="дентиносупергенез" tags="noun:inanim:m:v_oru" q="-0.5" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
    }

    @Test
    public void testPropNoun() {

        def tagged = tagText.tagText("Арешонков")

        def expected =
"""<sentence>
  <tokenReading>
    <token value="Арешонков" lemma="Арешонков" tags="noun:anim:m:v_naz:prop:lname" q="-0.6" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("Басуріна")

        expected =
"""<sentence>
  <tokenReading>
    <token value="Басуріна" lemma="Басурін" tags="noun:anim:m:v_rod:prop:lname" q="-0.5" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged

        tagged = tagText.tagText("змієвич")
       
        // the only suggestion is :prop:lname 
        expected =
        """<sentence>
  <tokenReading>
    <token value="змієвич" lemma="змієвич" tags="noun:anim:m:v_naz" q="-0.6" />
  </tokenReading>
</sentence>
"""
                assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("Андрій Гнідовський")

        expected =
"""<sentence>
  <tokenReading>
    <token value="Андрій" lemma="Андрій" tags="noun:anim:m:v_naz:prop:fname" />
  </tokenReading>
  <tokenReading>
    <token value="Гнідовський" lemma="Гнідовський" tags="noun:anim:m:v_naz:prop:lname" q="-0.5" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("Тетяни Дихановської")

        expected =
"""<sentence>
  <tokenReading>
    <token value="Тетяни" lemma="Тетяна" tags="noun:anim:f:v_rod:prop:fname" />
  </tokenReading>
  <tokenReading>
    <token value="Дихановської" lemma="Дихановська" tags="noun:anim:f:v_rod:prop:lname" q="-0.5" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("Натан Кунсель")

        expected =
"""<sentence>
  <tokenReading>
    <token value="Натан" lemma="Натан" tags="noun:anim:m:v_naz:prop:fname" />
  </tokenReading>
  <tokenReading>
    <token value="Кунсель" lemma="Кунсель" tags="noun:anim:m:v_naz:prop:lname" q="-0.5" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged

        // Вісьньовському -> lname

//        tagged = tagText.tagText("Вілінська-Маркович")
//
//        expected =
//"""<sentence>
//  <tokenReading>
//    <token value="Вілінська-Маркович" lemma="Вілінська-Маркович" tags="noun:anim:m:v_rod:prop:lname" />
//  </tokenReading>
//</sentence>
//"""
    }

    
    @Test
    public void testAbbr() {
        def tagged = tagText.tagText("АУФТ")

        def expected =
"""<sentence>
  <tokenReading>
    <token value="АУФТ" lemma="АУФТ" tags="noninfl:abbr" q="-0.7" />
  </tokenReading>
</sentence>
"""

        assertEquals expected, tagged.tagged
    }
    
    @Test
    public void testDotAbbr() {
        def tagged = tagText.tagText("І. В. Збарськ")

        def expected =
"""<sentence>
  <tokenReading>
    <token value="І." lemma="І." tags="noninf:abbr" />
  </tokenReading>
  <tokenReading>
    <token value="В." lemma="В." tags="noninf:abbr" />
  </tokenReading>
  <tokenReading>
    <token value="Збарськ" lemma="Збарськ" tags="noun:inanim:m:v_naz:prop:geo" q="-0.5" />
  </tokenReading>
</sentence>
"""

        assertEquals expected, tagged.tagged
    }

    
    @Test
    public void testVerb() {
        def tagged = tagText.tagText("популяв")

        def expected =
"""<sentence>
  <tokenReading>
    <token value="популяв" lemma="популяти" tags="verb:imperf:past:m" q="-0.6" />
  </tokenReading>
</sentence>
"""

        assertEquals expected, tagged.tagged

        //TODO: detect as perf (check prefix?)
        tagged = tagText.tagText("популявся")

        expected =
"""<sentence>
  <tokenReading>
    <token value="популявся" lemma="популятися" tags="verb:rev:imperf:past:m" q="-0.5" />
  </tokenReading>
</sentence>
"""

        assertEquals expected, tagged.tagged

        tagged = tagText.tagText("популяється")

        expected =
"""<sentence>
  <tokenReading>
    <token value="популяється" lemma="популятися" tags="verb:rev:imperf:pres:s:3" q="-0.6" />
  </tokenReading>
</sentence>
"""

        assertEquals expected, tagged.tagged
    }

}
