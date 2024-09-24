#!/bin/env groovy

package ua.net.nlp.tools.tag

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse

import org.apache.commons.text.StringEscapeUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

import groovy.json.JsonSlurper
import ua.net.nlp.tools.TextUtils.OutputFormat
import ua.net.nlp.tools.tag.TagOptions
import ua.net.nlp.tools.tag.TagStats
import ua.net.nlp.tools.tag.TagTextCore
import ua.net.nlp.tools.tag.TagTextCore.TTR
import ua.net.nlp.tools.tag.TagTextCore.TagResult
import ua.net.nlp.tools.tag.TagTextCore.TaggedToken


class TagTextVerticalOutputTest {
	def options = new TagOptions()

	static TagTextCore tagText = new TagTextCore()
	
	@BeforeEach
	void before() {
//		tagText.setOptions(options)
	}

	def file() { return new File("/dev/null") }


	@Test
	public void testTxtFormat() {
        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.vertical))
		TagResult tagged = tagText.tagText("Десь, там за горою ходила Галя. А далі - озеро...")

        def expected = 
"""<s>
Десь adv:&pron:ind десь
<g/>
, punct ,
там adv:&pron:dem там
за prep за
горою noun:inanim:f:v_oru гора
ходила verb:imperf:past:f ходити
Галя noun:anim:f:v_naz:prop:fname Галя
<g/>
. punct .
</s>

<s>
А conj:coord а
далі adv:compc:&predic далі
- punct -
озеро noun:inanim:n:v_naz озеро
<g/>
... punct ...
</s>
"""

		assertEquals expected, adjustResult(tagged.tagged)
	}

    
    @Test
    public void testTxtFormatWithSemantic() {
        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.vertical, semanticTags: true))
        
        String text = "А далі - озеро..."
        TagResult tagged = tagText.tagText(text)

        def expected =
"""<s>
А conj:coord а 
далі adv:compc:&predic далі 1:dist:2:time
- punct - 
озеро noun:inanim:n:v_naz озеро 
<g/>
... punct ... 
</s>
"""

        assertEquals expected, adjustResult(tagged.tagged)
    }
    

    @Test
    public void testTxtFormatWithUD() {
        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.conllu, semanticTags: true))
        
        def text = "А треба далі воно - озеро Світязь яке..."
        TagResult tagged = tagText.tagText(text)

        def expected =
"""# sent_id = 1
# text = $text
1 А а CCONJ conj:coord _ _ _ _
2 треба треба ADV noninfl:&predic _ _ _ Uninflect=Yes
3 далі далі ADV adv:compc:&predic Degree=Cmp _ _ SemTags=1:dist:2:time
4 воно воно NOUN noun:unanim:n:v_naz:&pron:pers:3 Animacy=Anim,Inan|Case=Nom|Gender=Neut|Number=Sing|Person=3|PronType=Prs _ _ SemTags=1:conc:deictic
5 - - PUNCT punct _ _ _ _
6 озеро озеро NOUN noun:inanim:n:v_naz Animacy=Inan|Case=Nom|Gender=Neut|Number=Sing _ _ _
7 Світязь Світязь PROPN noun:inanim:m:v_naz:prop:geo:xp1 Animacy=Inan|Case=Nom|Gender=Masc|NameType=Geo|Number=Sing _ _ SemTags=1:conc:loc
8 яке який ADJ adj:n:v_naz:&pron:int:rel:def Case=Nom|Gender=Neut|Number=Sing|PronType=Int|PronType=Rel _ _ SpaceAfter=No
9 ... ... PUNCT punct _ _ _ _
""".toString()

        assertEquals expected, adjustResult(tagged.tagged)

        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.conllu, semanticTags: false))
        
        def sent1 = "Шановні колеги, прошу вставте картки, зараз проведемо реєстрацію натисканням зеленої кнопки приладів."
        def sent2 = "Рада України."
        text = "$sent1 $sent2"
        
        tagged = tagText.tagText(text)
        
        expected =
"""# sent_id = 1
# text = $sent1
1 Шановні шановний ADJ adj:p:v_naz:compb Case=Nom|Degree=Pos|Number=Plur _ _ _
2 колеги колега NOUN noun:anim:p:v_naz Animacy=Anim|Case=Nom|Gender=Fem|Number=Plur _ _ SpaceAfter=No
3 , , PUNCT punct _ _ _ _
4 прошу просити VERB verb:imperf:pres:s:1 Aspect=Imp|Mood=Ind|Number=Sing|Person=1|Tense=Pres|VerbForm=Fin _ _ _
5 вставте вставити VERB verb:perf:impr:p:2 Aspect=Perf|Mood=Imp|Number=Plur|Person=2|VerbForm=Fin _ _ _
6 картки картка NOUN noun:inanim:p:v_zna Animacy=Inan|Case=Acc|Gender=Fem|Number=Plur _ _ SpaceAfter=No
7 , , PUNCT punct _ _ _ _
8 зараз зараз ADV adv:&pron:dem PronType=Dem _ _ _
9 проведемо провести VERB verb:perf:futr:p:1 Aspect=Perf|Mood=Ind|Number=Plur|Person=1|Tense=Fut|VerbForm=Fin _ _ _
10 реєстрацію реєстрація NOUN noun:inanim:f:v_zna Animacy=Inan|Case=Acc|Gender=Fem|Number=Sing _ _ _
11 натисканням натискання NOUN noun:inanim:p:v_dav Animacy=Inan|Case=Dat|Gender=Neut|Number=Plur _ _ _
12 зеленої зелений ADJ adj:f:v_rod:compb Case=Gen|Degree=Pos|Gender=Fem|Number=Sing _ _ _
13 кнопки кнопка NOUN noun:inanim:f:v_rod Animacy=Inan|Case=Gen|Gender=Fem|Number=Sing _ _ _
14 приладів прилад NOUN noun:inanim:p:v_rod Animacy=Inan|Case=Gen|Gender=Masc|Number=Plur _ _ SpaceAfter=No
15 . . PUNCT punct _ _ _ _

# sent_id = 2
# text = $sent2
1 Рада рада NOUN noun:inanim:f:v_naz Animacy=Inan|Case=Nom|Gender=Fem|Number=Sing _ _ _
2 України Україна PROPN noun:inanim:f:v_rod:prop:geo Animacy=Inan|Case=Gen|Gender=Fem|NameType=Geo|Number=Sing _ _ SpaceAfter=No
3 . . PUNCT punct _ _ _ _
""".toString()
    
//        println tagged.tagged.replace(" ", '\ ')
       
        assertEquals expected, adjustResult(tagged.tagged)
    }


    @Test
    public void testTxtFormatWithUDPlural() {

        def list = []
        tagText.udModule.language = tagText.language
        tagText.udModule.addPluralGender(new TaggedToken(value: 'труб', tags: 'noun:inanim:p:v_rod', lemma: 'труба'), list)
        
        assertEquals(['Gender=Fem'], list)
        
        list = []
        tagText.udModule.addPluralGender(new TaggedToken(value: 'статтей', tags: 'noun:inanim:p:v_rod:subst', lemma: 'стаття'), list)
        assertEquals(['Gender=Fem'], list)
    }
    
    private static String adjustResult(String txt) {
        txt.replace('\t', ' ')
            .replaceAll(/\|TagConfidence=[0-9.]+/, '')
            .replaceAll(/TagConfidence=[0-9.]+/, '_')
    }
}
