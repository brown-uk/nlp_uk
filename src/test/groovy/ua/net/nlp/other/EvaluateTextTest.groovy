#!/usr/bin/env groovy

package ua.net.nlp.other

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test


class EvaluateTextTest {

    EvaluateText evalText = new EvaluateText( )
	
	@Test
	public void test() {
        def errLines = []
        def ruleMatch = evalText.check("десь я тии", true, errLines)
        println ruleMatch
		assertEquals 2, ruleMatch.size()
    }
}
