#!/bin/env groovy

package org.nlp_uk.tools

import static org.junit.jupiter.api.Assertions.assertEquals

import org.apache.groovy.io.StringBuilderWriter
import org.apache.tools.ant.filters.StringInputStream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.nlp_uk.tools.TagText.Stats
import org.nlp_uk.tools.TagText.TagOptions
import org.nlp_uk.tools.TagText.TagResult


class TextUtilTest {
	TagOptions options = new TagOptions( 
		input: "-", 
		unknownStats: true 
		)

	TextUtils tagUtils = new TextUtils()
	TagText tagText = new TagText()
	
	@BeforeEach
	void before() {
		tagText.setOptions(options)
	}


	@Test
	public void testParallel() {
		def ret = []
		
		int cores = Runtime.getRuntime().availableProcessors()
		System.err.println("$cores cores detected")
		if( cores < 2 ) {
			System.err.println("this test won't test parallel tagging")
		}		
		
		def byteOS = new ByteArrayOutputStream(10000)
		def out = new PrintStream(byteOS)
		def count = 16
		def input = new BufferedInputStream(new StringInputStream("борода кікука.\n\n".repeat(count)))
		
		tagUtils.processFileParallel(input, out, 
			{ buffer ->  
            	return tagText.tagText(buffer)
			}, 
			cores, 
			{ TagResult result ->
				tagText.stats.add(result.stats) 
			})
		
		assertEquals("борода[борода/noun:inanim:f:v_naz] кікука[кікука/null].<P/> \n".repeat(count), byteOS.toString("UTF-8"))
		assertEquals(new HashMap<>(["кікука": count]), new HashMap<>(tagText.stats.unknownMap))
	}

	@Test
	public void testSingleThread() {
		def ret = []
		
		def byteOS = new ByteArrayOutputStream(10000)
		def out = new PrintStream(byteOS)
		def count = 16
		def input = new BufferedInputStream(new StringInputStream("борода кікука.\n\n".repeat(count)))
		
		tagUtils.processFile(input, out, 
			{ buffer ->  
            	return tagText.tagText(buffer)
			}, 
			{ TagResult result ->
				tagText.stats.add(result.stats) 
			})
		
		assertEquals("борода[борода/noun:inanim:f:v_naz] кікука[кікука/null].<P/> \n".repeat(count), byteOS.toString("UTF-8"))
		assertEquals(new HashMap<>(["кікука": count]), new HashMap<>(tagText.stats.unknownMap))
	}
}
