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
Десь\tadv:&pron:ind\tдесь
<g/>
,\tpunct\t,
там\tadv:&pron:dem\tтам
за\tprep\tза
горою\tadv\tгорою
ходила\tverb:imperf:past:f\tходити
Галя\tnoun:anim:f:v_naz:prop:fname\tГаля
<g/>
.\tpunct\t.
</s>

<s>
А\tconj:coord\tа
далі\tadv:compc:&predic\tдалі
-\tpunct\t-
озеро\tnoun:inanim:n:v_naz\tозеро
<g/>
...\tpunct\t...
</s>
"""

		assertEquals expected, tagged.tagged
	}

    
    @Test
    public void testTxtFormatWithSemantic() {
        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.vertical, semanticTags: true))
        
        String text = "А далі - озеро..."
        TagResult tagged = tagText.tagText(text)

        def expected =
"""<s>
А\tconj:coord\tа\t
далі\tadv:compc:&predic\tдалі\t1:dist:2:time
-\tpunct\t-\t
озеро\tnoun:inanim:n:v_naz\tозеро\t
<g/>
...\tpunct\t...\t
</s>
"""

        assertEquals expected, tagged.tagged
    }
    

    @Test
    public void testTxtFormatWithUD() {
        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.conllu, semanticTags: true))
        
        def text = "А треба далі воно - озеро..."
        TagResult tagged = tagText.tagText(text)

        def expected =
"""# sent_id = 1
# text = $text
1\tА\tа\tCCONJ\tconj:coord\t_\t_\t_\t_
2\tтреба\tтреба\tADV\tnoninfl:&predic\tUninflect=Yes\t_\t_\t_
3\tдалі\tдалі\tADV\tadv:compc:&predic\tDegree=Cmp\t_\t_\t1:dist:2:time
4\tвоно\tвоно\tNOUN\tnoun:unanim:n:v_naz:&pron:pers:3\tAnimacy=Anim,Inan|Case=Nom|Gender=Neut|Number=Sing|Person=3|PronType=Prs|VerbForm=Fin\t_\t_\t1:conc:deictic
5\t-\t-\tPUNCT\tpunct\t_\t_\t_\t_
6\tозеро\tозеро\tNOUN\tnoun:inanim:n:v_naz\tAnimacy=Inan|Case=Nom|Gender=Neut|Number=Sing\t_\t_\tSpaceAfter=No
7\t...\t...\tPUNCT\tpunct\t_\t_\t_\t_
""".toString()

        assertEquals expected, tagged.tagged

        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.conllu, semanticTags: false))
        
        text = "Шановні колеги, прошу вставте картки, зараз проведемо реєстрацію натисканням зеленої кнопки приладів."

        tagged = tagText.tagText(text)
        
        expected =
"""# sent_id = 1
# text = $text
1\tШановні\tшановний\tADJ\tadj:p:v_kly:compb\tCase=Voc|Degree=Pos|Number=Plur\t_\t_\t_
2\tколеги\tколега\tNOUN\tnoun:anim:f:v_rod\tAnimacy=Anim|Case=Gen|Gender=Fem|Number=Sing\t_\t_\tSpaceAfter=No
3\t,\t,\tPUNCT\tpunct\t_\t_\t_\t_
4\tпрошу\tпросити\tVERB\tverb:imperf:pres:s:1\tAspect=Imp|Mood=Ind|Number=Sing|Person=1|Tense=Pres|VerbForm=Fin\t_\t_\t_
5\tвставте\tвставити\tVERB\tverb:perf:impr:p:2\tAspect=Perf|Mood=Imp|Number=Plur|Person=2|VerbForm=Fin\t_\t_\t_
6\tкартки\tкартка\tNOUN\tnoun:inanim:f:v_rod\tAnimacy=Inan|Case=Gen|Gender=Fem|Number=Sing\t_\t_\tSpaceAfter=No
7\t,\t,\tPUNCT\tpunct\t_\t_\t_\t_
8\tзараз\tзараз\tADV\tadv:&pron:dem\tPronType=Dem\t_\t_\t_
9\tпроведемо\tпровести\tVERB\tverb:perf:futr:p:1\tAspect=Perf|Mood=Ind|Number=Plur|Person=1|Tense=Fut|VerbForm=Fin\t_\t_\t_
10\tреєстрацію\tреєстрація\tNOUN\tnoun:inanim:f:v_zna\tAnimacy=Inan|Case=Acc|Gender=Fem|Number=Sing\t_\t_\t_
11\tнатисканням\tнатискання\tNOUN\tnoun:inanim:n:v_oru\tAnimacy=Inan|Case=Ins|Gender=Neut|Number=Sing\t_\t_\t_
12\tзеленої\tзелений\tADJ\tadj:f:v_rod:compb\tCase=Gen|Degree=Pos|Gender=Fem|Number=Sing\t_\t_\t_
13\tкнопки\tкнопка\tNOUN\tnoun:inanim:f:v_rod\tAnimacy=Inan|Case=Gen|Gender=Fem|Number=Sing\t_\t_\t_
14\tприладів\tприлад\tNOUN\tnoun:inanim:p:v_rod\tAnimacy=Inan|Case=Gen|Gender=Masc|Number=Plur\t_\t_\tSpaceAfter=No
15\t.\t.\tPUNCT\tpunct\t_\t_\t_\t_
""".toString()
    
//        println tagged.tagged.replace("\t", '\\t')
       
        assertEquals expected, tagged.tagged
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
}
