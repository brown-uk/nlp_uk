#!/bin/env groovy

package ua.net.nlp.tools.tag

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNotEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.junit.jupiter.api.Assumptions.assumeTrue

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import ua.net.nlp.bruk.WordReading
import ua.net.nlp.tools.tag.TagOptions
import ua.net.nlp.tools.tag.TagTextCore
import ua.net.nlp.tools.tag.TagTextCore.TagResult


class TagTextDisambigTest {
    final NEW_TESTS = Boolean.getBoolean("ua.net.nlp.tests.new")

    // static so we don't have to reload stats
    static TagTextCore tagText = new TagTextCore()
    TagOptions options = new TagOptions()
    
    @BeforeAll
    static void before() {
        tagText.disambigStats.writeDerivedStats = true
    }


    @BeforeEach
    void beforeEach() {
        options.disambiguate = true
        options.tokenFormat = true
        options.singleTokenOnly = true
        options.disambiguationDebug = true
        
        tagText.setOptions(options)
    }

    @CompileStatic
    @Test
    public void testTokenReading() {
        def wr1 = new WordReading('а', 'part')
        def wr2 = new WordReading('а', 'conj')
        assertNotEquals(wr1, wr2) 
    }
    
    @Test
    public void testTokenFormat() {
        options.singleTokenOnly = false
        
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
    public void testFirstTokenOnly() {
        TagResult tagged = tagText.tagText("відлетіла")

        def expected =
"""<sentence>
  <token value="відлетіла" lemma="відлетіти" tags="verb:perf:past:f" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
        
        def expected4 =
"""<sentence>
  <token value="Чорні" lemma="чорний" tags="adj:p:v_naz:compb" />
</sentence>
<paragraph/>
"""
        tagged = tagText.tagText("Чорні")

        assertEquals expected4, tagged.tagged

        assumeTrue(NEW_TESTS)
        
        tagged = tagText.tagText("минула думка")
        
        expected =
"""<sentence>
  <token value="минула" lemma="минути" tags="verb:perf:past:f" />
  <token value="думка" lemma="думка" tags="noun:inanim:f:v_naz" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }
    
    
    @Test
    public void testXp22() {
        options.singleTokenOnly = false
        
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
    public void testXp2() {
        assumeTrue(NEW_TESTS)

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
//            assertEquals 1, tagged0.stats.disambigMap['noWord']
    }

    @Test
    public void testPrepWithPron() {
        options.singleTokenOnly = false

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
    public void testPrepNounMis() {
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
    public void testPrepAdjMis() {
        TagResult tagged3 = tagText.tagText("в окремім")
        
        def expected3 =
"""<sentence>
  <token value="в" lemma="в" tags="prep" />
  <token value="окремім" lemma="окремий" tags="adj:m:v_mis" />
</sentence>
<paragraph/>
"""
            assertEquals expected3, tagged3.tagged
            assertEquals 1, tagged3.stats.disambigMap['noWord']
    }
    
    @Test
    public void testPrepNounChas() {
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
    public void testPrepAdjUnknown() {
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
    public void testPrepNoun() {
        TagResult tagged = tagText.tagText("у пасічництво")

        def expected =
"""<sentence>
  <token value="у" lemma="у" tags="prep" />
  <token value="пасічництво" lemma="пасічництво" tags="noun:inanim:n:v_zna" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }


    @Test
    public void testCtxAdjNoun() {
        TagResult tagged = tagText.tagText(", цього тижня")

        def expected =
"""<sentence>
  <token value="," lemma="," tags="punct" />
  <token value="цього" lemma="цей" tags="adj:m:v_rod:&amp;pron:dem" />
  <token value="тижня" lemma="тиждень" tags="noun:inanim:m:v_rod" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }

    @Test
    public void testNounAndNoun() {
        TagResult tagged = tagText.tagText("засобів і методів")

        def expected =
"""<sentence>
  <token value="засобів" lemma="засіб" tags="noun:inanim:p:v_rod" />
  <token value="і" lemma="і" tags="conj:coord" />
  <token value="методів" lemma="метод" tags="noun:inanim:p:v_rod" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
        assertEquals 1, tagged.stats.disambigMap['word']
    }
    
    @Test
    public void testNounAndNoun2() {
        // no left context word
        TagResult tagged = tagText.tagText("ліфтів і методів")

        def expected =
"""<sentence>
  <token value="ліфтів" lemma="ліфт" tags="noun:inanim:p:v_rod" />
  <token value="і" lemma="і" tags="conj:coord" />
  <token value="методів" lemma="метод" tags="noun:inanim:p:v_rod" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
        assertEquals 1, tagged.stats.disambigMap['word']
    }
    
    
    @Test
    public void testAdvOverAdj() {
        assumeTrue(NEW_TESTS)

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
    public void testAdvOverNoun() {
        assumeTrue(NEW_TESTS)
        
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

    @Test
    public void testAnimOverInanim() {
        assumeTrue(NEW_TESTS)

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
    public void testSeloCapitalProp() {
        assumeTrue(NEW_TESTS)

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
    public void testVerbNounZna() {
        TagResult tagged = tagText.tagText("вивчає репродуктивне")
        
        def expected =
"""<sentence>
  <token value="вивчає" lemma="вивчати" tags="verb:imperf:pres:s:3" />
  <token value="репродуктивне" lemma="репродуктивний" tags="adj:n:v_zna" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged

        tagged = tagText.tagText("будували спеціальні зимівники")
        
        expected =
"""<sentence>
  <token value="будували" lemma="будувати" tags="verb:imperf:past:p" />
  <token value="спеціальні" lemma="спеціальний" tags="adj:p:v_zna:rinanim" />
  <token value="зимівники" lemma="зимівник" tags="noun:inanim:p:v_zna" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }
    
    
    @Test
    public void testVerbNoun() {
        def tagged = tagText.tagText("з'являється коріння")
        
        def expected =
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
    }
    
    @Test
    public void testAdjNounIv() {
        def tagged = tagText.tagText("кабінет міністрів")
        
        def expected =
"""<sentence>
  <token value="кабінет" lemma="кабінет" tags="noun:inanim:m:v_naz" />
  <token value="міністрів" lemma="міністр" tags="noun:anim:p:v_rod" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }
    

    @Test
    public void testPropNoun() {
        assumeTrue(NEW_TESTS)

        def tagged = tagText.tagText("сміху Котляревського")
        
        def expected =
"""<sentence>
  <token value="сміху" lemma="сміх" tags="noun:inanim:m:v_rod" />
  <token value="Котляревського" lemma="Котляревський" tags="noun:anim:m:v_rod:prop:lname" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }
    
    
    @Test
    public void testPropCountryNoun1() {
        assumeTrue(NEW_TESTS)

        def tagged = tagText.tagText("і Чилі, Перу")
        
        def expected =
"""<sentence>
  <token value="і" lemma="і" tags="part" />
  <token value="Чилі" lemma="Чилі" tags="noun:inanim:f:v_rod:nv:prop:geo" />
  <token value="," lemma="," tags="punct" />
  <token value="Перу" lemma="Перу" tags="noun:inanim:f:v_rod:nv:prop:geo" />
</sentence>
<paragraph/>
"""
        assertEquals expected, tagged.tagged
    }
    
    @Test
    public void testPropCountryNoun2() {
        def tagged = tagText.tagText("і Аргентина")
        
        def expected =
"""<sentence>
  <token value="і" lemma="і" tags="part" />
  <token value="Аргентина" lemma="Аргентина" tags="noun:inanim:f:v_naz:prop:geo" />
</sentence>
<paragraph/>
"""
            assertEquals expected, tagged.tagged

        assumeTrue(NEW_TESTS)
            
        tagged = tagText.tagText(", Панами")
        
        expected =
"""<sentence>
  <token value="," lemma="," tags="punct" />
  <token value="Панами" lemma="Панама" tags="noun:inanim:f:v_rod:prop:geo" />
</sentence>
<paragraph/>
"""
            assertEquals expected, tagged.tagged
        }
    
    @Test
    public void testAdjNounLinkBoost() {
        assumeTrue(NEW_TESTS)

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

    
    @Test
    public void testAdjAdjNoun() {
        
        def tagged = tagText.tagText("Американські збройні сили")
        
        def expected = """<sentence>
  <token value="Американські" lemma="американський" tags="adj:p:v_naz" />
  <token value="збройні" lemma="збройний" tags="adj:p:v_zna:rinanim" />
  <token value="сили" lemma="сила" tags="noun:inanim:p:v_zna" />
</sentence>
<paragraph/>
"""

        assertEquals expected, tagged.tagged


        assumeTrue(NEW_TESTS)
        
        tagged = tagText.tagText("на такий великий кавун")
        
        expected = """<sentence>
  <token value="на" lemma="на" tags="prep" />
  <token value="такий" lemma="такий" tags="adj:m:v_zna:rinanim:&amp;pron:dem" />
  <token value="великий" lemma="великий" tags="adj:m:v_zna:rinanim:compb" />
  <token value="кавун" lemma="кавун" tags="noun:inanim:m:v_zna" />
</sentence>
<paragraph/>
"""

        assertEquals expected, tagged.tagged
    }

    
    @Test
    public void testIgnoreParts() {
        assumeTrue(NEW_TESTS)
        
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
    public void testPani() {
        def tagged = tagText.tagText("й пані Людмили")
        
        assertTrue tagged.tagged.contains("lemma=\"пані\"")
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
