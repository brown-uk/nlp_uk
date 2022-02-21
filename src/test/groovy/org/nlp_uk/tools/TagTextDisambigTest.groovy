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
    <token value="Тому" lemma="Том" tags="noun:anim:m:v_dav:prop:fname" />
    <token value="Тому" lemma="Тома" tags="noun:anim:m:v_zna:prop:fname" />
    <token value="Тому" lemma="той" tags="adj:n:v_dav:&amp;pron:dem" />
    <token value="Тому" lemma="те" tags="noun:inanim:n:v_dav:&amp;pron:dem" />
  </tokenReading>
</sentence>
"""
        assertEquals expected2, tagged2.tagged
        
    }

    
    @Test
    public void testTokenFormat() {
        tagText.setOptions(new TagOptions(xmlOutput: true, disambiguate: [DisambigModule.frequency], tokenFormat: true, showDisambigRate: false))

        TagResult tagged = tagText.tagText("а")

        def expected =
"""<sentence>
  <token value="а" lemma="а" tags="conj:coord">
    <alts>
      <token value="а" lemma="а" tags="part" />
      <token value="а" lemma="а" tags="intj" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged

        TagResult tagged2 = tagText.tagText("А")

        def expected2 =
"""<sentence>
  <token value="А" lemma="а" tags="conj:coord">
    <alts>
      <token value="А" lemma="а" tags="part" />
      <token value="А" lemma="а" tags="intj" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
        assertEquals expected2, tagged2.tagged
    }

    
    @Test
    public void testTokenFormat2() {
        tagText.setOptions(new TagOptions(xmlOutput: true, disambiguate: [DisambigModule.frequency], tokenFormat: true, showDisambigRate: false))
        TagResult tagged4 = tagText.tagText("шаблоні")
        
        def expected4 =
"""<sentence>
  <token value="шаблоні" lemma="шаблон" tags="noun:inanim:m:v_mis:xp2">
    <alts>
      <token value="шаблоні" lemma="шаблон" tags="noun:inanim:m:v_mis:xp1" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
            assertEquals expected4, tagged4.tagged
            assertEquals 1, tagged4.stats.disambigMap['noWord']
     }

    
    @Test
    public void testTokenFormatWithCtx1() {
        tagText.setOptions(new TagOptions(xmlOutput: true, tokenFormat: true, disambiguate: [DisambigModule.context], showDisambigRate: false))

        TagResult tagged = tagText.tagText("на нього")

        def expected =
"""<sentence>
  <token value="на" lemma="на" tags="prep" />
  <token value="нього" lemma="він" tags="noun:unanim:m:v_zna:&amp;pron:pers:3">
    <alts>
      <token value="нього" lemma="воно" tags="noun:unanim:n:v_zna:&amp;pron:pers:3" />
      <token value="нього" lemma="він" tags="noun:unanim:m:v_rod:&amp;pron:pers:3" />
      <token value="нього" lemma="воно" tags="noun:unanim:n:v_rod:&amp;pron:pers:3" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
        assertEquals 1, tagged.stats.disambigMap['word']
    }
    
    @Test
    public void testTokenFormatWithCtxAndXp() {
        tagText.setOptions(new TagOptions(xmlOutput: true, tokenFormat: true, disambiguate: [DisambigModule.context], showDisambigRate: false))

        // by tag stats
        TagResult tagged0 = tagText.tagText("стан досліджуваного")
        
        def expected0 =
"""<sentence>
  <token value="стан" lemma="стан" tags="noun:inanim:m:v_naz:xp2">
    <alts>
      <token value="стан" lemma="стан" tags="noun:inanim:m:v_naz:xp1" />
      <token value="стан" lemma="стан" tags="noun:inanim:m:v_zna:xp2" />
      <token value="стан" lemma="стан" tags="noun:inanim:m:v_zna:xp1" />
    </alts>
  </token>
  <token value="досліджуваного" lemma="досліджуваний" tags="adj:m:v_rod:&amp;adjp:pasv:imperf">
    <alts>
      <token value="досліджуваного" lemma="досліджуваний" tags="adj:n:v_rod:&amp;adjp:pasv:imperf" />
      <token value="досліджуваного" lemma="досліджуваний" tags="adj:m:v_zna:ranim:&amp;adjp:pasv:imperf" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
            assertEquals expected0, tagged0.tagged
            assertEquals 1, tagged0.stats.disambigMap['noWord']
    }

    @Test
    public void testTokenFormatWithCtx22() {
        tagText.setOptions(new TagOptions(xmlOutput: true, tokenFormat: true, disambiguate: [DisambigModule.context], showDisambigRate: false))
        // by tag stats
        TagResult tagged2 = tagText.tagText("в книгомережі")
        
        def expected2 =
"""<sentence>
  <token value="в" lemma="в" tags="prep" />
  <token value="книгомережі" lemma="книгомережа" tags="noun:inanim:f:v_mis">
    <alts>
      <token value="книгомережі" lemma="книгомережа" tags="noun:inanim:p:v_zna" />
      <token value="книгомережі" lemma="книгомережа" tags="noun:inanim:f:v_rod" />
      <token value="книгомережі" lemma="книгомережа" tags="noun:inanim:p:v_naz" />
      <token value="книгомережі" lemma="книгомережа" tags="noun:inanim:f:v_dav" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
            assertEquals expected2, tagged2.tagged
            assertEquals 1, tagged2.stats.disambigMap['noWord']
    }

    @Test
    public void testTokenFormatWithCtx23() {
        tagText.setOptions(new TagOptions(xmlOutput: true, tokenFormat: true, disambiguate: [DisambigModule.context], showDisambigRate: false))
        TagResult tagged3 = tagText.tagText("в окремім")
        
        def expected3 =
"""<sentence>
  <token value="в" lemma="в" tags="prep" />
  <token value="окремім" lemma="окремий" tags="adj:m:v_mis">
    <alts>
      <token value="окремім" lemma="окремий" tags="adj:n:v_mis" />
      <token value="окремім" lemma="окреме" tags="noun:inanim:n:v_mis" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
            assertEquals expected3, tagged3.tagged
            assertEquals 1, tagged3.stats.disambigMap['noWord']
    }
    
    @Test
    public void testTokenFormatWithCtx2() {
        tagText.setOptions(new TagOptions(xmlOutput: true, tokenFormat: true, disambiguate: [DisambigModule.context], showDisambigRate: false))

        TagResult tagged = tagText.tagText(", цього тижня")

        def expected =
"""<sentence>
  <token value="," lemma="," tags="punct" />
  <token value="цього" lemma="цей" tags="adj:m:v_rod:&amp;pron:dem">
    <alts>
      <token value="цього" lemma="це" tags="noun:inanim:n:v_rod:&amp;pron:dem" />
      <token value="цього" lemma="цей" tags="adj:n:v_rod:&amp;pron:dem" />
      <token value="цього" lemma="цей" tags="adj:m:v_zna:ranim:&amp;pron:dem" />
    </alts>
  </token>
  <token value="тижня" lemma="тиждень" tags="noun:inanim:m:v_rod">
    <alts>
      <token value="тижня" lemma="тиждень" tags="noun:inanim:m:v_zna:var" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }

    @Test
    public void testTokenFormatWithCtx3() {
        tagText.setOptions(new TagOptions(xmlOutput: true, tokenFormat: true, disambiguate: [DisambigModule.context], showDisambigRate: false))

        TagResult tagged = tagText.tagText("засобів і методів")

        def expected =
"""<sentence>
  <token value="засобів" lemma="засіб" tags="noun:inanim:p:v_rod" />
  <token value="і" lemma="і" tags="conj:coord">
    <alts>
      <token value="і" lemma="і" tags="part" />
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
    public void testTokenFormatWithCtx4() {
        tagText.setOptions(new TagOptions(xmlOutput: true, tokenFormat: true, disambiguate: [DisambigModule.context], showDisambigRate: false))

        // no left context word
        TagResult tagged = tagText.tagText("ліфтів і методів")

        def expected =
"""<sentence>
  <token value="ліфтів" lemma="ліфт" tags="noun:inanim:p:v_rod" />
  <token value="і" lemma="і" tags="conj:coord">
    <alts>
      <token value="і" lemma="і" tags="part" />
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
    public void testTokenFormatWithCtx5() {
        tagText.setOptions(new TagOptions(xmlOutput: true, tokenFormat: true, disambiguate: [DisambigModule.context], showDisambigRate: false))

        TagResult tagged = tagText.tagText("під час переслідування")

        def expected =
"""<sentence>
  <token value="під" lemma="під" tags="prep" />
  <token value="час" lemma="час" tags="noun:inanim:m:v_zna">
    <alts>
      <token value="час" lemma="час" tags="noun:inanim:m:v_naz:&amp;predic" />
    </alts>
  </token>
  <token value="переслідування" lemma="переслідування" tags="noun:inanim:n:v_rod">
    <alts>
      <token value="переслідування" lemma="переслідування" tags="noun:inanim:n:v_naz" />
      <token value="переслідування" lemma="переслідування" tags="noun:inanim:n:v_zna" />
      <token value="переслідування" lemma="переслідування" tags="noun:inanim:p:v_naz" />
      <token value="переслідування" lemma="переслідування" tags="noun:inanim:p:v_zna" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
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
        tagText.setOptions(new TagOptions(xmlOutput: true, singleTokenOnly: true, disambiguate: [DisambigModule.frequency, DisambigModule.wordEnding], showDisambigRate: false))

        TagResult tagged = tagText.tagText("стильні дерева")

        def expected =
"""<sentence>
  <token value="стильні" lemma="стильний" tags="adj:p:v_naz:compb" />
  <token value="дерева" lemma="дерево" tags="noun:inanim:p:v_naz" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }

    @Test
    public void testFirstTokenOnlyByTagCtx() {
        tagText.setOptions(new TagOptions(xmlOutput: true, tokenFormat: true, disambiguate: [DisambigModule.frequency, DisambigModule.context], showDisambigRate: false))

        TagResult tagged = tagText.tagText("в чорно-біле")

        def expected =
"""<sentence>
  <token value="в" lemma="в" tags="prep" />
  <token value="чорно-біле" lemma="чорно-білий" tags="adj:n:v_zna">
    <alts>
      <token value="чорно-біле" lemma="чорно-білий" tags="adj:n:v_naz" />
      <token value="чорно-біле" lemma="чорно-білий" tags="adj:n:v_kly" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }

    @Test
    public void testFirstTokenOnlyByTagCtx2() {
        tagText.setOptions(new TagOptions(xmlOutput: true, tokenFormat: true, disambiguate: [DisambigModule.frequency, DisambigModule.context], showDisambigRate: false))

        TagResult tagged = tagText.tagText("у пасічництво")

        def expected =
"""<sentence>
  <token value="у" lemma="у" tags="prep" />
  <token value="пасічництво" lemma="пасічництво" tags="noun:inanim:n:v_zna">
    <alts>
      <token value="пасічництво" lemma="пасічництво" tags="noun:inanim:n:v_naz" />
    </alts>
  </token>
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }
}
