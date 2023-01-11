#!/bin/env groovy

package ua.net.nlp.tools

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assumptions.assumeTrue

import org.checkerframework.framework.qual.IgnoreInWholeProgramInference
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

import ua.net.nlp.tools.tag.TagOptions
import ua.net.nlp.tools.tag.TagTextCore
import ua.net.nlp.tools.tag.TagTextCore.TagResult


class TagTextDisambigTest {
    final NEW_TESTS = Boolean.getBoolean("ua.net.nlp.tests.new")

    def options = new TagOptions()

	static TagTextCore tagText = new TagTextCore()
	
	@BeforeAll
	static void before() {
        tagText.disambigStats.writeDerivedStats = true
	}


    public void testStats() {
        tagText.setOptions(new TagOptions(disambiguate: true)) // [DisambigModule.frequency]))

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
    <token value="Тому" lemma="те" tags="noun:inanim:n:v_dav:&amp;pron:dem" />
    <token value="Тому" lemma="той" tags="adj:n:v_dav:&amp;pron:dem" />
  </tokenReading>
</sentence>
"""
        assertEquals expected2, tagged2.tagged
        
    }

    
    @Test
    public void testTokenFormat() {
        tagText.setOptions(new TagOptions(disambiguate: true, tokenFormat: true)) // [DisambigModule.frequency]

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
        tagText.setOptions(new TagOptions(disambiguate: true, tokenFormat: true, showDisambigRate: false))
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
    public void testWithCtx1() {
        tagText.setOptions(new TagOptions(tokenFormat: true, disambiguate: true, showDisambigRate: false))

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
    public void testWithCtxAndXp() {
        assumeTrue(NEW_TESTS)

        tagText.setOptions(new TagOptions(tokenFormat: true, singleTokenOnly: true, disambiguate: true, showDisambigRate: false)) // [DisambigModule.context]

        // by tag stats
        TagResult tagged0 = tagText.tagText("стан досліджуваного")
        
        def expected0 =
"""<sentence>
  <token value="стан" lemma="стан" tags="noun:inanim:m:v_naz:xp2" />
  <token value="досліджуваного" lemma="досліджуваний" tags="adj:m:v_rod:&amp;adjp:pasv:imperf" />
</sentence>
<paragraph/>
"""
            assertEquals expected0, tagged0.tagged
            assertEquals 1, tagged0.stats.disambigMap['noWord']
    }

    @Test
    public void testWithCtx22() {
        tagText.setOptions(new TagOptions(tokenFormat: true, singleTokenOnly: true, disambiguate: true, showDisambigRate: false)) // [DisambigModule.context]
        // by tag stats
        TagResult tagged2 = tagText.tagText("в книгомережі")
        
        def expected2 =
"""<sentence>
  <token value="в" lemma="в" tags="prep" />
  <token value="книгомережі" lemma="книгомережа" tags="noun:inanim:f:v_mis" />
</sentence>
<paragraph/>
"""
            assertEquals expected2, tagged2.tagged
            assertEquals 1, tagged2.stats.disambigMap['noWord']
    }

    @Test
    public void testWithCtx23() {
        tagText.setOptions(new TagOptions(tokenFormat: true, disambiguate: true, showDisambigRate: false)) // [DisambigModule.context]
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
    public void testWithCtx2() {
        tagText.setOptions(new TagOptions(tokenFormat: true, disambiguate: true, showDisambigRate: false)) // [DisambigModule.context]

        TagResult tagged = tagText.tagText(", цього тижня")

        def expected =
"""<sentence>
  <token value="," lemma="," tags="punct" />
  <token value="цього" lemma="цей" tags="adj:m:v_rod:&amp;pron:dem">
    <alts>
      <token value="цього" lemma="це" tags="noun:inanim:n:v_rod:&amp;pron:dem" />
      <token value="цього" lemma="цей" tags="adj:m:v_zna:ranim:&amp;pron:dem" />
      <token value="цього" lemma="цей" tags="adj:n:v_rod:&amp;pron:dem" />
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
    public void testWithCtx3() {
        tagText.setOptions(new TagOptions(tokenFormat: true, disambiguate: true, showDisambigRate: false)) // [DisambigModule.context]

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
    public void testWithCtx4() {
        tagText.setOptions(new TagOptions(tokenFormat: true, disambiguate: true, showDisambigRate: false)) // [DisambigModule.context]

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
    public void testWithCtx5() {
        tagText.setOptions(new TagOptions(tokenFormat: true, singleTokenOnly: true, disambiguate: true))

        TagResult tagged = tagText.tagText("під час переслідування")

        def expected =
"""<sentence>
  <token value="під" lemma="під" tags="prep" />
  <token value="час" lemma="час" tags="noun:inanim:m:v_zna" />
  <token value="переслідування" lemma="переслідування" tags="noun:inanim:n:v_rod" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }
    
    @Test
    public void testWithCtx6() {
        tagText.setOptions(new TagOptions(singleTokenOnly: true, disambiguate: true))

        TagResult tagged = tagText.tagText("Щеплення")

        def expected =
"""<sentence>
  <token value="Щеплення" lemma="щеплення" tags="noun:inanim:n:v_naz" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }
    
    @Test
    public void testWithCtx7() {
        assumeTrue(NEW_TESTS)

        tagText.setOptions(new TagOptions(tokenFormat: true, singleTokenOnly: true, disambiguate: true, disambiguationDebug:true))

        TagResult tagged = tagText.tagText("блискуче")

        def expected =
"""<sentence>
  <token value="блискуче" lemma="блискуче" tags="adv:compb" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }
    
    @Test
    public void testWithCtx8() {
        assumeTrue(NEW_TESTS)

        tagText.setOptions(new TagOptions(tokenFormat: true, singleTokenOnly: true, disambiguate: true, disambiguationDebug:true))

        TagResult tagged = tagText.tagText("Спала")

        def expected =
"""<sentence>
  <token value="Спала" lemma="спати" tags="verb:imperf:past:f" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }
    
    @Test
    public void testWithCtx9() {
        assumeTrue(NEW_TESTS)

        tagText.setOptions(new TagOptions(tokenFormat: true, singleTokenOnly: true, disambiguate: true, disambiguationDebug:true))

        TagResult tagged = tagText.tagText("селищному голові")

        def expected =
"""<sentence>
  <token value="селищному" lemma="селищний" tags="adj:m:v_dav" />
  <token value="голові" lemma="голова" tags="noun:anim:m:v_dav" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }

    
    @Test
    public void testWithCapitalProp() {
        assumeTrue(NEW_TESTS)

        tagText.setOptions(new TagOptions(tokenFormat: true, singleTokenOnly: true, disambiguate: true, disambiguationDebug:true))

        TagResult tagged = tagText.tagText("село Сосни")

        def expected =
"""<sentence>
  <token value="село" lemma="село" tags="noun:inanim:n:v_naz" />
  <token value="Сосни" lemma="Сосни" tags="noun:inanim:p:v_naz:ns:prop:geo" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }

    @Test
    public void testFirstToken() {
        tagText.setOptions(new TagOptions(tokenFormat: true))

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
        tagText.setOptions(new TagOptions(singleTokenOnly: true, disambiguate: true))

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
  <token value="Заняття" lemma="заняття" tags="noun:inanim:p:v_naz" />
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

    @Disabled
    @Test
    public void testFirstTokenOnly2() {
        tagText.setOptions(new TagOptions(singleTokenOnly: true, disambiguate: true, showDisambigRate: false))

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
        tagText.setOptions(new TagOptions(tokenFormat: true, singleTokenOnly: true, disambiguate: true, showDisambigRate: false))

        TagResult tagged = tagText.tagText("в чорно-біле")

        def expected =
"""<sentence>
  <token value="в" lemma="в" tags="prep" />
  <token value="чорно-біле" lemma="чорно-білий" tags="adj:n:v_zna" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }

    @Test
    public void testFirstTokenOnlyByTagCtx2() {
        tagText.setOptions(new TagOptions(tokenFormat: true, disambiguate: true, showDisambigRate: false))

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

    
    @Test
    public void testFirstTokenOnlyByTagCtxVerbNoun1() {
        tagText.setOptions(new TagOptions(tokenFormat: true, singleTokenOnly: true, disambiguate: true, showDisambigRate: false, disambiguationDebug:true))

        TagResult tagged = tagText.tagText("вивчає репродуктивне")
        
        def expected =
"""<sentence>
  <token value="вивчає" lemma="вивчати" tags="verb:imperf:pres:s:3" />
  <token value="репродуктивне" lemma="репродуктивний" tags="adj:n:v_zna" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }

    @Test
    public void testFirstTokenOnlyByTagCtxVerbNoun2() {
        tagText.setOptions(new TagOptions(tokenFormat: true, singleTokenOnly: true, disambiguate: true, showDisambigRate: false, disambiguationDebug:true))

        TagResult tagged = tagText.tagText("будували спеціальні зимівники")
        
        def expected =
"""<sentence>
  <token value="будували" lemma="будувати" tags="verb:imperf:past:p" />
  <token value="спеціальні" lemma="спеціальний" tags="adj:p:v_zna:rinanim" />
  <token value="зимівники" lemma="зимівник" tags="noun:inanim:p:v_zna" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged

        tagged = tagText.tagText("з'являється коріння")
        
        expected =
"""<sentence>
  <token value="з'являється" lemma="з'являтися" tags="verb:rev:imperf:pres:s:3" />
  <token value="коріння" lemma="коріння" tags="noun:inanim:n:v_naz" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }
    
    @Test
    public void testAdjNounLink() {
        tagText.setOptions(new TagOptions(tokenFormat: true, singleTokenOnly: true, disambiguate: true, disambiguationDebug:true))

        TagResult tagged = tagText.tagText("зеленого відродження")
        
        def expected =
"""<sentence>
  <token value="зеленого" lemma="зелений" tags="adj:n:v_rod:compb" />
  <token value="відродження" lemma="відродження" tags="noun:inanim:n:v_rod" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged

        // стрільців і командирів
    
        tagged = tagText.tagText("кабінет міністрів")
        
        expected =
"""<sentence>
  <token value="кабінет" lemma="кабінет" tags="noun:inanim:m:v_naz" />
  <token value="міністрів" lemma="міністр" tags="noun:anim:p:v_rod" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }

    @Disabled
    @Test
    public void testAdjNounLinkBoots() {
        tagText.setOptions(new TagOptions(tokenFormat: true, singleTokenOnly: true, disambiguate: true, disambiguationDebug:true))
        def tagged = tagText.tagText("тристоронні договори")
        
        def expected =
"""<sentence>
  <token value="тристоронні" lemma="тристоронній" tags="adj:p:v_naz" />
  <token value="договори" lemma="договір" tags="noun:inanim:p:v_naz" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
        
        tagged = tagText.tagText("вегетативне розмноження")
        
        expected =
        """<sentence>
  <token value="вегетативне" lemma="вегетативний" tags="adj:n:v_naz" />
  <token value="розмноження" lemma="розмноження" tags="noun:inanim:n:v_naz" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }

    @Disabled
    @Test
    public void testIgnoreParts() {
        tagText.setOptions(new TagOptions(tokenFormat: true, singleTokenOnly: true, disambiguate: true, disambiguationDebug:true))
        def tagged = tagText.tagText("перешкоджали б вільному")
        
        def expected =
"""<sentence>
    <token value="перешкоджали" lemma="перешкоджати" tags="verb:imperf:past:p" />
    <token value="б" lemma="б" tags="part" />
    <token value="вільному" lemma="вільний" tags="adj:m:v_dav:compb" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }
    

    @Test
    public void testDoty() {
        assumeTrue(NEW_TESTS)
        
        tagText.setOptions(new TagOptions(tokenFormat: true, singleTokenOnly: true, disambiguate: true, disambiguationDebug:true))

        TagResult tagged = tagText.tagText("ослаблюються доти")
        
        def expected =
"""<sentence>
  <token value="ослаблюються" lemma="ослаблюватися" tags="verb:rev:imperf:pres:p:3" />
  <token value="доти" lemma="доти" tags="adv:&amp;pron:dem" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }

    // що нижче по схилу
    // Дорога забрала
    // , як карі
    // 32-бітний
    // повів їх назад
    // 50-тих   50-той
    // корені різні
    // у розвиткові суспільства
    // зрілого Франка


    
    // відповідають певним «стандартам»
    // в цих супровідних
    // ці лічильні засоби
    // облікові жетони
    // усвідомлювали значення та роль
  
}
