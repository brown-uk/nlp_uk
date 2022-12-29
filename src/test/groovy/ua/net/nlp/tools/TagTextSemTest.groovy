#!/bin/env groovy

package ua.net.nlp.tools

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
    <token value="усе" lemma="усе" tags="noun:inanim:n:v_naz:&amp;pron:gen" semtags="1:quantif" />
    <token value="усе" lemma="усе" tags="noun:inanim:n:v_zna:&amp;pron:gen" semtags="1:quantif" />
    <token value="усе" lemma="усе" tags="part" />
    <token value="усе" lemma="увесь" tags="adj:n:v_naz:&amp;pron:gen" semtags="1:quantif" />
    <token value="усе" lemma="увесь" tags="adj:n:v_zna:&amp;pron:gen" semtags="1:quantif" />
  </tokenReading>
  <tokenReading>
    <token value="голова" lemma="голова" tags="noun:anim:f:v_naz" semtags="1:conc:hum&amp;hierar" />
    <token value="голова" lemma="голова" tags="noun:anim:m:v_naz" semtags="1:conc:hum&amp;hierar" />
    <token value="голова" lemma="голова" tags="noun:inanim:f:v_naz" semtags="1:conc:body:part:2:abst:ment:3:abst:unit" />
  </tokenReading>
  <tokenReading>
    <token value="аахенська" lemma="аахенський" tags="adj:f:v_kly" semtags="1:abst" />
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
    <token value="один-другий" lemma="один-другий" tags="adj:m:v_naz:&amp;numr" semtags="1:abst:quantity" />
    <token value="один-другий" lemma="один-другий" tags="adj:m:v_zna:rinanim:&amp;numr" semtags="1:abst:quantity" />
  </tokenReading>
  <tokenReading>
    <token value="." lemma="." tags="punct" />
  </tokenReading>
</sentence>
"""

        tagText.setOptions(new TagOptions(semanticTags: true, xmlOutput: true))
        TagResult tagged = tagText.tagText("Слово усе голова аахенська Вашингтон акту один-другий.")
        assertEquals expected, tagged.tagged
    }

}
