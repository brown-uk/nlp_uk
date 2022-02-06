#!/bin/env groovy

package org.nlp_uk.tools

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.nlp_uk.tools.TagText.OutputFormat.json

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.nlp_uk.tools.TagText.OutputFormat
import org.nlp_uk.tools.TagText.TagOptions
import org.nlp_uk.tools.TagText.TagOptions.DisambigModule
import org.nlp_uk.tools.TagText.TagResult


class TagTextDisambigTest {
	def options = new TagOptions()

	static TagText tagText = new TagText()
	
	@BeforeAll
	static void before() {
        tagText.disambigStats.dbg = true
	}


    @Test
    public void testStats() {
        tagText.setOptions(new TagOptions(xmlOutput: true, disambiguate: [DisambigModule.frequency]))

        TagResult tagged = tagText.tagText("а")

        def expected =
"""<sentence>
  <tokenReading>
    <token value="а" lemma="а" tags="conj:coord" />
    <token value="а" lemma="а" tags="part" />
    <token value="а" lemma="а" tags="intj" />
  </tokenReading>
</sentence>
"""
        assertEquals expected, tagged.tagged

        TagResult tagged2 = tagText.tagText("Тому")
        
                def expected2 =
"""<sentence>
  <tokenReading>
    <token value="Тому" lemma="тому" tags="adv" />
    <token value="Тому" lemma="тому" tags="conj:subord" />
    <token value="Тому" lemma="той" tags="adj:m:v_dav:&amp;pron:dem" />
    <token value="Тому" lemma="Тома" tags="noun:anim:m:v_zna:prop:fname" />
    <token value="Тому" lemma="Том" tags="noun:anim:m:v_dav:prop:fname" />
    <token value="Тому" lemma="те" tags="noun:inanim:n:v_dav:&amp;pron:dem" />
    <token value="Тому" lemma="той" tags="adj:n:v_dav:&amp;pron:dem" />
  </tokenReading>
</sentence>
"""
        assertEquals expected2, tagged2.tagged
        
    }

    
    @Test
    public void testSingleTokenFormat() {
        tagText.setOptions(new TagOptions(xmlOutput: true, disambiguate: [DisambigModule.frequency], tokenFormat: true, showDisambigRate: true))

        TagResult tagged = tagText.tagText("а")

        def expected =
"""<sentence>
  <token value="а" lemma="а" tags="conj:coord" q="0.994">
    <alts>
      <token value="а" lemma="а" tags="part" q="0.006" />
      <token value="а" lemma="а" tags="intj" q="0.000" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged

        TagResult tagged2 = tagText.tagText("А")

        def expected2 =
"""<sentence>
  <token value="А" lemma="а" tags="conj:coord" q="0.783">
    <alts>
      <token value="А" lemma="а" tags="part" q="0.215" />
      <token value="А" lemma="а" tags="intj" q="0.002" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
        assertEquals expected2, tagged2.tagged
    }

    
    @Test
    public void testSingleTokenFormat2() {
        tagText.setOptions(new TagOptions(xmlOutput: true, disambiguate: [DisambigModule.frequency], tokenFormat: true, showDisambigRate: true))
        TagResult tagged4 = tagText.tagText("шаблоні")
        
        def expected4 =
"""<sentence>
  <token value="шаблоні" lemma="шаблон" tags="noun:inanim:m:v_mis:xp2" q="0.501">
    <alts>
      <token value="шаблоні" lemma="шаблон" tags="noun:inanim:m:v_mis:xp1" q="0.499" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
            assertEquals expected4, tagged4.tagged
            assertEquals 1, tagged4.stats.disambigMap['noWord']
     }

    
    @Test
    public void testSingleTokenFormatWithCtx1() {
        tagText.setOptions(new TagOptions(xmlOutput: true, tokenFormat: true, disambiguate: [DisambigModule.context], showDisambigRate: true))

        TagResult tagged = tagText.tagText("на нього")

        def expected =
"""<sentence>
  <token value="на" lemma="на" tags="prep" />
  <token value="нього" lemma="він" tags="noun:unanim:m:v_zna:&amp;pron:pers:3" q="0.815">
    <alts>
      <token value="нього" lemma="воно" tags="noun:unanim:n:v_zna:&amp;pron:pers:3" q="0.141" />
      <token value="нього" lemma="він" tags="noun:unanim:m:v_rod:&amp;pron:pers:3" q="0.036" />
      <token value="нього" lemma="воно" tags="noun:unanim:n:v_rod:&amp;pron:pers:3" q="0.008" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
        assertEquals 1, tagged.stats.disambigMap['word']
    }
    
    @Test
    public void testSingleTokenFormatWithCtx21() {
        tagText.setOptions(new TagOptions(xmlOutput: true, tokenFormat: true, disambiguate: [DisambigModule.context], showDisambigRate: true))

        // by tag stats
        TagResult tagged0 = tagText.tagText("стан досліджуваного")
        
        def expected0 =
"""<sentence>
  <token value="стан" lemma="стан" tags="noun:inanim:m:v_zna:xp2" q="0.554">
    <alts>
      <token value="стан" lemma="стан" tags="noun:inanim:m:v_naz:xp2" q="0.304" />
      <token value="стан" lemma="стан" tags="noun:inanim:m:v_naz:xp1" q="0.127" />
      <token value="стан" lemma="стан" tags="noun:inanim:m:v_zna:xp1" q="0.015" />
    </alts>
  </token>
  <token value="досліджуваного" lemma="досліджуваний" tags="adj:m:v_rod:&amp;adjp:pasv:imperf" q="0.522">
    <alts>
      <token value="досліджуваного" lemma="досліджуваний" tags="adj:n:v_rod:&amp;adjp:pasv:imperf" q="0.435" />
      <token value="досліджуваного" lemma="досліджуваний" tags="adj:m:v_zna:ranim:&amp;adjp:pasv:imperf" q="0.043" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
            assertEquals expected0, tagged0.tagged
            assertEquals 1, tagged0.stats.disambigMap['noWord']
    }

    @Test
    public void testSingleTokenFormatWithCtx22() {
        tagText.setOptions(new TagOptions(xmlOutput: true, tokenFormat: true, disambiguate: [DisambigModule.context], showDisambigRate: true))
        // by tag stats
        TagResult tagged2 = tagText.tagText("в книгомережі")
        
        def expected2 =
"""<sentence>
  <token value="в" lemma="в" tags="prep" />
  <token value="книгомережі" lemma="книгомережа" tags="noun:inanim:f:v_mis" q="0.647">
    <alts>
      <token value="книгомережі" lemma="книгомережа" tags="noun:inanim:p:v_zna" q="0.175" />
      <token value="книгомережі" lemma="книгомережа" tags="noun:inanim:f:v_rod" q="0.119" />
      <token value="книгомережі" lemma="книгомережа" tags="noun:inanim:p:v_naz" q="0.056" />
      <token value="книгомережі" lemma="книгомережа" tags="noun:inanim:f:v_dav" q="0.004" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
            assertEquals expected2, tagged2.tagged
            assertEquals 1, tagged2.stats.disambigMap['noWord']
    }

    @Test
    public void testSingleTokenFormatWithCtx23() {
        tagText.setOptions(new TagOptions(xmlOutput: true, tokenFormat: true, disambiguate: [DisambigModule.context], showDisambigRate: true))
        TagResult tagged3 = tagText.tagText("в окремім")
        
        def expected3 =
"""<sentence>
  <token value="в" lemma="в" tags="prep" />
  <token value="окремім" lemma="окремий" tags="adj:m:v_mis" q="0.544">
    <alts>
      <token value="окремім" lemma="окремий" tags="adj:n:v_mis" q="0.254" />
      <token value="окремім" lemma="окреме" tags="noun:inanim:n:v_mis" q="0.202" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
            assertEquals expected3, tagged3.tagged
            assertEquals 1, tagged3.stats.disambigMap['noWord']
    }
    
    @Test
    public void testSingleTokenFormatWithCtx3() {
        tagText.setOptions(new TagOptions(xmlOutput: true, tokenFormat: true, disambiguate: [DisambigModule.context], showDisambigRate: true))

        TagResult tagged = tagText.tagText("засобів і методів")

        def expected =
"""<sentence>
  <token value="засобів" lemma="засіб" tags="noun:inanim:p:v_rod" />
  <token value="і" lemma="і" tags="conj:coord" q="0.916">
    <alts>
      <token value="і" lemma="і" tags="part" q="0.084" />
    </alts>
  </token>
  <token value="методів" lemma="метод" tags="noun:inanim:p:v_rod" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
        assertEquals 1, tagged.stats.disambigMap['word']
    }
    
    @Test
    public void testSingleTokenFormatWithCtx4() {
        tagText.setOptions(new TagOptions(xmlOutput: true, tokenFormat: true, disambiguate: [DisambigModule.context], showDisambigRate: true))

        // no left context word
        TagResult tagged = tagText.tagText("ліфтів і методів")

        def expected =
"""<sentence>
  <token value="ліфтів" lemma="ліфт" tags="noun:inanim:p:v_rod" />
  <token value="і" lemma="і" tags="conj:coord" q="0.914">
    <alts>
      <token value="і" lemma="і" tags="part" q="0.086" />
    </alts>
  </token>
  <token value="методів" lemma="метод" tags="noun:inanim:p:v_rod" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
        assertEquals 1, tagged.stats.disambigMap['word']
    }


    @Test
    public void testFirstToken() {
        tagText.setOptions(new TagOptions(xmlOutput: true, tokenFormat: true))

        TagResult tagged = tagText.tagText("а")

        def expected =
"""<sentence>
  <token value="а" lemma="а" tags="conj:coord">
    <alts>
      <token value="а" lemma="а" tags="intj" />
      <token value="а" lemma="а" tags="part" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }

    @Test
    public void testFirstTokenOnly() {
        tagText.setOptions(new TagOptions(xmlOutput: true, singleTokenOnly: true, disambiguate: [DisambigModule.frequency]))

        TagResult tagged = tagText.tagText("відлетіла")

        def expected =
"""<sentence>
  <token value="відлетіла" lemma="відлетіти" tags="verb:perf:past:f" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
        
        def expected2 =
"""<sentence>
  <token value="маркітні" lemma="маркітний" tags="adj:p:v_naz" />
</sentence>
<paragraph/>
"""
        tagged = tagText.tagText("маркітні")

        assertEquals expected2, tagged.tagged

        def expected3 =
"""<sentence>
  <token value="Заняття" lemma="заняття" tags="noun:inanim:p:v_zna" />
</sentence>
<paragraph/>
"""
        tagged = tagText.tagText("Заняття")

        assertEquals expected3, tagged.tagged

        def expected4 =
"""<sentence>
  <token value="Чорні" lemma="чорний" tags="adj:p:v_naz:compb" />
</sentence>
<paragraph/>
"""
        tagged = tagText.tagText("Чорні")

        assertEquals expected4, tagged.tagged
    }

    @Test
    public void testFirstTokenOnlyByTagWE() {
        tagText.setOptions(new TagOptions(xmlOutput: true, singleTokenOnly: true, disambiguate: [DisambigModule.frequency, DisambigModule.wordEnding], showDisambigRate: true))

        TagResult tagged = tagText.tagText("стильні")

        def expected =
"""<sentence>
  <token value="стильні" lemma="стильний" tags="adj:p:v_zna:rinanim:compb" q="0.522" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }

    @Test
    public void testFirstTokenOnlyByTagCtx() {
        tagText.setOptions(new TagOptions(xmlOutput: true, tokenFormat: true, disambiguate: [DisambigModule.frequency, DisambigModule.context], showDisambigRate: true))

        TagResult tagged = tagText.tagText("в чорно-біле")

        def expected =
"""<sentence>
  <token value="в" lemma="в" tags="prep" />
  <token value="чорно-біле" lemma="чорно-білий" tags="adj:n:v_naz" q="0.850">
    <alts>
      <token value="чорно-біле" lemma="чорно-білий" tags="adj:n:v_zna" q="0.150" />
      <token value="чорно-біле" lemma="чорно-білий" tags="adj:n:v_kly" q="0.000" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }
}
