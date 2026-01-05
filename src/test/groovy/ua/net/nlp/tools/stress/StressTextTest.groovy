#!/bin/env groovy

package ua.net.nlp.tools.stress

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import ua.net.nlp.tools.stress.StressTextCore
import ua.net.nlp.tools.stress.StressTextCore.StressResult


class StressTextTest {
	def options = [ : ]

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
		println "Homonym: " + result.stats.homonymCnt
	}


	@Test
	public void testStress() {
		def expected = "Аби́ те про́сте́ сло́во загуло́ розмі́щеним якнайщирі́шим мабобом."
		def text = "Аби те просте слово загуло розміщеним якнайщирішим мабобом."
//		def text = TagText.stripAccent(expected + ambig).trim()

		result = stressText.stressText(text)
		assertEquals expected.trim(), result.tagged
		assertEquals 1, result.stats.unknown.size()
		assertEquals 0, result.stats.homonymCnt
	}

	@Test
	public void testStressDualTags() {
		def expected = "аналізу́є/аналізує абонува́ти ага́кало/агакало докла́дніше"
		def text = "аналізує абонувати агакало докладніше"

		result = stressText.stressText(text)
		assertEquals expected.trim(), result.tagged
		assertEquals [:], result.stats.unknown
		assertEquals 0, result.stats.homonymCnt
	}
	
	@Test
	public void testStressProp() {
		def text = "Байден з Берліна до Києва"
        def expected = "Ба́йден з Берлі́на до Ки́єва"
        
		result = stressText.stressText(text)
		assertEquals expected.trim(), result.tagged
		assertEquals [:], result.stats.unknown
		assertEquals 0, result.stats.homonymCnt
	}

	@Test
	public void testStress2() {
		def expected = "Готу́є ре́чення мене́/ме́не біржовика́ архівника"
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
    public void testStressHomonymDismabig() {
        options.disambiguate = true
        stressText.setOptions(options)
        
        def expected = "Я пасу́ по́ки ове́ць, нічи́м не гі́рше за будь-кого́."

        result = stressText.stressText(strip(expected))
        assertEquals expected.trim(), result.tagged
        
        expected = "Її́ перекла́ли, досягне́ чого́сь поді́бного, 750 чоловікі́в"

        result = stressText.stressText(strip(expected))
        assertEquals expected.trim(), result.tagged

        expected = "Оле́но, Олекса́ндрович, Есто́ніє"

        result = stressText.stressText(strip(expected))
        assertEquals expected.trim(), result.tagged

        // logic with tags
        expected = "реда́ктор ціє́ї кни́жки Шекспі́ра, зму́шені"

        result = stressText.stressText(strip(expected))
        assertEquals expected.trim(), result.tagged
        
       // TODO: ніко́ли/ні́коли
       // згі́дно з його зако́ном
       // прикла́д/при́клад
    }
    
    private static String strip(String text) {
        return text.replace("\u0301", "")
    }
}
