#!/bin/env groovy

package ua.net.nlp.tools

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.junit.jupiter.api.Assumptions.assumeTrue

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

import ua.net.nlp.tools.tag.TagOptions
import ua.net.nlp.tools.tag.TagTextCore
import ua.net.nlp.tools.tag.TagTextCore.TagResult


public class TagTextUnknownTest {
    final NEW_TESTS = Boolean.getBoolean("ua.net.nlp.tests.new")

    def options = new TagOptions()

	static TagTextCore tagText = new TagTextCore()
	
	@BeforeEach
	void before() {
        options.tagUnknown = true
        options.unknownRate = true
        options.disambiguate = true
        options.singleTokenOnly = true
        options.setLemmaForUnknown = true
        tagText.setOptions(options)
	}


    @Test
    public void testAdjNoun() {

        TagResult tagged = tagText.tagText("адюльтерівськими")
        
        def expected =
"""<sentence>
  <token value="адюльтерівськими" lemma="адюльтерівський" tags="adj:p:v_oru" q="-0.5" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged

        tagged = tagText.tagText("стратомарафон")
                
        expected =
"""<sentence>
  <token value="стратомарафон" lemma="стратомарафон" tags="noun:inanim:m:v_zna" q="-0.5" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged

        tagged = tagText.tagText("Біоміметикою")
                
        expected =
"""<sentence>
  <token value="Біоміметикою" lemma="біоміметика" tags="noun:inanim:f:v_oru" q="-0.5" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
        
        
        assumeTrue(NEW_TESTS)
        
        tagged = tagText.tagText("дентиносупергенезом")
                
        expected =
"""<sentence>
  <token value="дентиносупергенезом" lemma="дентиносупергенез" tags="noun:inanim:m:v_oru" q="-0.5" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }

    
    @Test
    public void testPrepAdj() {

        TagResult tagged = tagText.tagText("в доагломераційний")
        
        def expected =
"""<sentence>
  <token value="в" lemma="в" tags="prep" />
  <token value="доагломераційний" lemma="доагломераційний" tags="adj:m:v_zna:rinanim" q="-0.5" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }
    
    
    @Test
    public void testPropNoun() {

        def tagged = tagText.tagText("Арешонков")

        def expected =
"""<sentence>
  <token value="Арешонков" lemma="Арешонков" tags="noun:anim:m:v_naz:prop:lname" q="-0.6" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("Басуріна")

        expected =
"""<sentence>
  <token value="Басуріна" lemma="Басурін" tags="noun:anim:m:v_rod:prop:lname" q="-0.5" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged

        tagged = tagText.tagText("змієвич")
       
        // the only suggestion is :prop:lname 
        expected =
"""<sentence>
  <token value="змієвич" lemma="змієвич" tags="noun:anim:m:v_naz" q="-0.6" />
</sentence>
<paragraph/>
"""
                assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("Андрій Гнідовський")

        expected =
"""<sentence>
  <token value="Андрій" lemma="Андрій" tags="noun:anim:m:v_naz:prop:fname" />
  <token value="Гнідовський" lemma="Гнідовський" tags="noun:anim:m:v_naz:prop:lname" q="-0.5" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("Тетяни Дихановської")

        expected =
"""<sentence>
  <token value="Тетяни" lemma="Тетяна" tags="noun:anim:f:v_rod:prop:fname" />
  <token value="Дихановської" lemma="Дихановська" tags="noun:anim:f:v_rod:prop:lname" q="-0.5" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("Натан Кунсель")

        expected =
"""<sentence>
  <token value="Натан" lemma="Натан" tags="noun:anim:m:v_naz:prop:fname" />
  <token value="Кунсель" lemma="Кунсель" tags="noun:anim:m:v_naz:prop:lname" q="-0.5" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
        
//        tagged = tagText.tagText("пані Ференцовій")
//
//        expected =
//"""<sentence>
//  <token value="пані" lemma="паня" tags="noun:anim:p:v_naz:xp2" />
//  <token value="Ференцовій" lemma="Ференцова" tags="noun:anim:f:v_dav:prop:lname" q="-0.5" />
//</sentence>
//<paragraph/>
//"""
//        assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("Наталія Алібаренко")
        
                expected =
        """<sentence>
  <token value="Наталія" lemma="Наталія" tags="noun:anim:f:v_naz:prop:fname" />
  <token value="Алібаренко" lemma="Алібаренко" tags="noun:anim:f:v_naz:nv:prop:lname" q="-0.5" />
</sentence>
<paragraph/>
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
  <token value="АУФТ" lemma="АУФТ" tags="noninfl:abbr" q="-0.7" />
</sentence>
<paragraph/>
"""

        assertEquals expected, tagged.tagged
    }

        
    @Test
    public void testDotAbbr() {
        def tagged = tagText.tagText("І. В. Збарськ")

        def expected =
"""<sentence>
  <token value="І." lemma="І." tags="noninf:abbr" />
  <token value="В." lemma="В." tags="noninf:abbr" />
  <token value="Збарськ" lemma="Збарськ" tags="noun:inanim:m:v_naz:prop:geo" q="-0.5" />
</sentence>
<paragraph/>
"""

        assertEquals expected, tagged.tagged
    }
    

    @Disabled
    @Test
    public void testHpyphened() {
        def tagged = tagText.tagText("Оболонь-арені")

        def expected =
"""<sentence>
  <token value="Оболонь-арені" lemma="оболонь-арена" tags="noun:inanim:f:v_mis" q="-0.5" />
</sentence>
<paragraph/>
"""

        assertEquals expected, tagged.tagged
    }

    
    @Test
    public void testVerb() {
        def tagged = tagText.tagText("популяв")

        def expected =
"""<sentence>
  <token value="популяв" lemma="популяти" tags="verb:imperf:past:m" q="-0.6" />
</sentence>
<paragraph/>
"""

        assertEquals expected, tagged.tagged

        //TODO: detect as perf (check prefix?)
        tagged = tagText.tagText("популявся")

        expected =
"""<sentence>
  <token value="популявся" lemma="популятися" tags="verb:rev:imperf:past:m" q="-0.5" />
</sentence>
<paragraph/>
"""

        assertEquals expected, tagged.tagged

        tagged = tagText.tagText("популяється")

        expected =
"""<sentence>
  <token value="популяється" lemma="популятися" tags="verb:rev:imperf:pres:s:3" q="-0.6" />
</sentence>
<paragraph/>
"""

        assertEquals expected, tagged.tagged
    }

    
    @Test
    public void testUnknown() {

        def uknowns = getClass().getClassLoader().getResource("tag/unknown.txt").readLines()
            .findAll { it -> ! it.startsWith('#') }

        uknowns.each { word ->
        
            TagResult tagged = tagText.tagText(word)
        
            assertTrue tagged.tagged.contains('tags=\"unknown\"'), tagged.tagged
        }
    }
}
