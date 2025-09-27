#!/bin/env groovy

package ua.net.nlp.tools.tag

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import ua.net.nlp.tools.tag.TagOptions
import ua.net.nlp.tools.tag.TagTextCore
import ua.net.nlp.tools.tag.TagTextCore.TagResult



class TagTextSemTest {
	def options = new TagOptions()

	static TagTextCore tagText = new TagTextCore()
	
	@BeforeAll
	static void before() {
        tagText.disambigStats.writeDerivedStats = true
	}


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
    <token value="усе" lemma="усе" tags="noun:inanim:n:v_naz:pron:gen" semtags="1:quantif" />
    <token value="усе" lemma="усе" tags="noun:inanim:n:v_zna:pron:gen" semtags="1:quantif" />
    <token value="усе" lemma="увесь" tags="adj:n:v_naz:pron:gen" semtags="1:quantif" />
    <token value="усе" lemma="увесь" tags="adj:n:v_zna:pron:gen" semtags="1:quantif" />
  </tokenReading>
  <tokenReading>
    <token value="голова" lemma="голова" tags="noun:anim:f:v_naz" semtags="1:conc:hum&amp;hierar" />
    <token value="голова" lemma="голова" tags="noun:anim:m:v_naz" semtags="1:conc:hum&amp;hierar" />
    <token value="голова" lemma="голова" tags="noun:inanim:f:v_naz" semtags="1:conc:body:part:2:abst:ment:3:abst:unit" />
  </tokenReading>
  <tokenReading>
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
    <token value="один-другий" lemma="один-другий" tags="adj:m:v_naz:numr" semtags="1:abst:quantity" />
    <token value="один-другий" lemma="один-другий" tags="adj:m:v_zna:rinanim:numr" semtags="1:abst:quantity" />
  </tokenReading>
  <tokenReading>
    <token value="." lemma="." tags="punct" />
  </tokenReading>
</sentence>
"""

        tagText.setOptions(new TagOptions(semanticTags: true))
        TagResult tagged = tagText.tagText("Слово усе голова аахенська Вашингтон акту один-другий.")
        assertEquals expected, tagged.tagged
    }

    
    @Test
    public void testSemanticOrg() {
        def expected=
"""<sentence>
  <tokenReading>
    <token value="хімвиробник" lemma="хімвиробник" tags="noun:anim:m:v_naz" semtags="1:conc:org" />
  </tokenReading>
  <tokenReading>
    <token value="півча" lemma="півча" tags="noun:inanim:f:v_naz" semtags="1:conc:hum:group" />
    <token value="півча" lemma="півчий" tags="adj:f:v_naz:bad" />
  </tokenReading>
</sentence>
"""

        tagText.setOptions(new TagOptions(semanticTags: true))
        TagResult tagged = tagText.tagText("хімвиробник півча")
        assertEquals expected, tagged.tagged
    }

    
    @Test
    public void testSemanticAlt() {
        def expected=
"""<sentence>
  <tokenReading>
    <token value="колеґа" lemma="колеґа" tags="noun:anim:f:v_naz:alt" semtags="1:conc:hum" />
    <token value="колеґа" lemma="колеґа" tags="noun:anim:m:v_naz:alt" semtags="1:conc:hum" />
  </tokenReading>
  <tokenReading>
    <token value="по-турецьки" lemma="по-турецьки" tags="adv" semtags="1:manner" />
  </tokenReading>
</sentence>
"""

        tagText.setOptions(new TagOptions(semanticTags: true))
        TagResult tagged = tagText.tagText("колеґа по\u2013турецьки")
        assertEquals expected, tagged.tagged
    }

    
    @Test
    public void testSemanticDerivat() {
        def expected= 
"""<sentence>
  <token value="стверджуючи" lemma="стверджуючи" tags="advp:imperf" semtags="1:speech:2:effect" />
</sentence>
<paragraph/>
"""

        tagText.setOptions(new TagOptions(semanticTags: true, tokenFormat: true))
        TagResult tagged = tagText.tagText("стверджуючи")
        assertEquals expected, tagged.tagged
    }

}
