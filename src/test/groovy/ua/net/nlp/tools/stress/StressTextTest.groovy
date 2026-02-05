#!/bin/env groovy

package ua.net.nlp.tools.stress

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import groovy.transform.CompileStatic
import ua.net.nlp.tools.stress.StressTextCore
import ua.net.nlp.tools.stress.StressTextCore.StressOptions
import ua.net.nlp.tools.stress.StressTextCore.StressResult


@CompileStatic
class StressTextTest {
	StressOptions options = new StressOptions()

	static StressTextCore stressText = new StressTextCore()
	StressResult result
	
	@BeforeEach
	void before() {
        options.disambiguate = false
		stressText.setOptions(options)
	}

	@AfterEach
	void after() {
		println "Unknown: " + result.stats.unknown.size()
		println "Homonym: " + result.stats.homonyms.size()
	}


	@Test
	public void testStress() {
		def expected = "Аби́ те про́сте́ сло́во загуло́ розмі́щеним якнайщирі́шим мабобом."
		def text = "Аби те просте слово загуло розміщеним якнайщирішим мабобом."
//		def text = TagText.stripAccent(expected + ambig).trim()

		result = stressText.stressText(text)
		assertEquals expected.trim(), result.tagged
		assertEquals 1, result.stats.unknown.size()
		assertEquals 0, result.stats.homonyms.size()
	}

	@Test
	public void testStressDualTags() {
		def expected = "аналізу́є/аналізує абонува́ти ага́кало докла́дніше"
		def text = "аналізує абонувати агакало докладніше"

		result = stressText.stressText(text)
		assertEquals expected.trim(), result.tagged
		assertEquals [:], result.stats.unknown
		assertEquals 0, result.stats.homonyms.size()
	}
	
	@Test
	public void testStressProp() {
		def text = "Байден з Берліна до Києва"
        def expected = "Ба́йден з Берлі́на до Ки́єва"
        
		result = stressText.stressText(text)
		assertEquals expected.trim(), result.tagged
		assertEquals [:], result.stats.unknown
		assertEquals 0, result.stats.homonyms.size()
	}

	@Test
	public void testStress2() {
		def expected = "Готу́є ре́чення мене́ біржовика́ архівника"
		def text = "Готує речення мене біржовика архівника"

		result = stressText.stressText(text)
		assertEquals expected.trim(), result.tagged
	}

	@Test
	public void testStressHomonym() {
		def expected = "Я пасу́/па́су по́ки ове́ць, ні́чим/нічи́м не гі́рше за будь-кого́."
		def text = "Я пасу поки овець, нічим не гірше за будь-кого."
//		def text = TagText.stripAccent(expected + ambig).trim()

		result = stressText.stressText(text)
		assertEquals expected.trim(), result.tagged
	}
    

    @Test
    public void testStressUnknown() {
        options.disambiguate = true
        stressText.setOptions(options)

        def expected = "Ковтаню́к."

        result = stressText.stressText(strip(expected))
        assertEquals expected.trim(), result.tagged
    }


    @Test
    public void testStressHomonymDismabig() {
        options.disambiguate = true
        stressText.setOptions(options)
        
        def expected = "Я ніко́му не пасу́ по́ки ове́ць, нічи́м не гі́рше за будь-кого́."

        result = stressText.stressText(strip(expected))
        assertEquals expected.trim(), result.tagged
        
        expected = "Її́ перекла́ли, досягне́ чого́сь поді́бного, 750 чоловікі́в необе́ртні собі́ чо́рні ді́ри анігілюва́лися; христо́вого"

        result = stressText.stressText(strip(expected))
        assertEquals expected.trim(), result.tagged

        expected = "Оле́но, Олекса́ндрович, Есто́ніє, Ґаліле́я І́ндові Протви́. ки́єво-могиля́нського. Про́тво!"

        result = stressText.stressText(strip(expected))
        assertEquals expected.trim(), result.tagged

        // logic with tags
        expected = "найекстрема́льніший елемента́рніший ува́жніше найцікаві́ше; реда́ктор ціє́ї кни́жки Шекспі́ра, зму́шені у ви́ді моде́лей дозво́лений проє́кт авторі́в"

        result = stressText.stressText(strip(expected))
        assertEquals expected.trim(), result.tagged

        // context prep // noun:anim:p:v_zna:rare // noun:inanim:m:v_zna:var
        expected = "по Кри́му, в Криму́; пої́хав у го́сті; написа́в листа́"
        
        result = stressText.stressText(strip(expected))
        assertEquals expected.trim(), result.tagged
    
        // dual gender + verb:imperf/perf
        expected = "еспера́нто дружи́на аблакту́юсь"
        
        result = stressText.stressText(strip(expected))
        assertEquals expected.trim(), result.tagged

        expected = "Про́стору-ча́су вче́них-я́дерників до́вго-до́вго триво́жно-депреси́вних п'я́тому-шо́стому 30-кілометро́вої 30-рі́ччям 11-кла́сники ip-адре́са"
        
        result = stressText.stressText(strip(expected))
        assertEquals expected.trim(), result.tagged

        expected = "лиши́ти само́го себе́ на ме́не й на се́бе"
        
        result = stressText.stressText(strip(expected))
        assertEquals expected.trim(), result.tagged
        
        // xp1/xp2
        expected = "о́рган правлі́ння"
        
        result = stressText.stressText(strip(expected))
        assertEquals expected.trim(), result.tagged

        // TODO:
        // згі́дно з його зако́ном
        // прикла́д/при́клад
        // виміря́вши/ви́мірявши - perf/imperf
    }

    
    @Test
    public void testPreserveWhiteSpace() {
        def expected = "Подя́ки. Всім.\nБага́то люде́й: допомага́ли\n    мені́ написа́ти цю \"кни́жку\"."

        result = stressText.stressText(strip(expected))
        assertEquals expected.trim(), result.tagged
    }
        
    private static String strip(String text) {
        return text.replace("\u0301", "")
    }
}
