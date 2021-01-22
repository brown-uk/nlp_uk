#!/bin/env groovy

package org.nlp_uk.tools

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.nlp_uk.tools.StressText.StressResult


class StressTextTest {
	def options = [ : ]

	static StressText stressText = new StressText()
	StressResult result
	
	@BeforeEach
	void before() {
		stressText.setOptions(options)
	}

	@AfterEach
	void after() {
		println "Unknown: " + result.stats.unknownCnt
		println "Homonym: " + result.stats.homonymCnt
	}


	@Test
	public void testStress() {
		def expected = "Аби́ те про́сте́ сло́во загуло́ розмі́щеним мабобом."
		def text = "Аби те просте слово загуло розміщеним мабобом."
//		def text = TagText.stripAccent(expected + ambig).trim()

		result = stressText.stressText(text)
		assertEquals expected.trim(), result.tagged
		assertEquals 1, result.stats.unknownCnt
		assertEquals 0, result.stats.homonymCnt
	}

	@Test
	public void testStressDualTags() {
		def expected = "аналізу́є/аналізує абонува́ти ага́кало/агакало докладніше/докла́дніше"
		def text = "аналізує абонувати агакало докладніше"

		result = stressText.stressText(text)
		assertEquals expected.trim(), result.tagged
		assertEquals 3, result.stats.unknownCnt
		assertEquals 0, result.stats.homonymCnt
	}
	
	@Test
	public void testStressProp() {
		def expected = "Байден з Берлі́на до Ки́єва"
		def text = "Байден з Берліна до Києва"

		result = stressText.stressText(text)
		assertEquals expected.trim(), result.tagged
		assertEquals 1, result.stats.unknownCnt
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
}



