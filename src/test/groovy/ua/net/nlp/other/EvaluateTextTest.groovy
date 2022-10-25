#!/usr/bin/env groovy

package ua.net.nlp.other

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test


class EvaluateTextTest {

    EvaluateText evalText = new EvaluateText( )
	
    @Disabled
	@Test
	public void test1() {
        def errLines = []
        def ruleMatches = evalText.check("десь я тии", true, errLines)
        println ruleMatches
		assertEquals 2, ruleMatches.size()
        assertEquals 7, ruleMatches[1].getFromPos()
        assertEquals 10, ruleMatches[1].getToPos()
        
        evalText.setSentLimit(1)
        ruleMatches = evalText.check("десь я тии. Десь тии я", true, errLines)
        println ruleMatches
        assertEquals 1, ruleMatches.size()
        assertEquals 5, ruleMatches[0].getFromPos()
        assertEquals 8, ruleMatches[0].getToPos()
    }

    @Disabled
    @Test
    public void test2() {
        def text =
"""черевичками, лаками й закордонними поїзд"""

        text += (char)0xAD
        
        def errLines = []
        def ruleMatch = evalText.check(text, true, errLines)
        println ruleMatch
        assertEquals 1, ruleMatch.size()
    }

}
