#!/bin/env groovy

package ua.net.nlp.tools

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import ua.net.nlp.tools.tag.TagOptions
import ua.net.nlp.tools.tag.TagOptions.OutputFormat
import ua.net.nlp.tools.tag.TagTextCore
import ua.net.nlp.tools.tag.TagTextCore.TagResult


class TextUtilTest {
	TagOptions options = new TagOptions( 
		input: "-", 
		unknownStats: true 
		)

	TextUtils tagUtils = new TextUtils()
	TagTextCore tagText = new TagTextCore()
	
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
		def input = new ByteArrayInputStream(("борода кікука.\n\n".repeat(count) + "А").getBytes("UTF-8"))
		
        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.txt, unknownStats: true))
        
		tagUtils.processFileParallel(input, out, options,
			{ buffer ->  
            	return tagText.tagText(buffer)
			}, 
			cores, 
			{ TagResult result ->
				tagText.stats.add(result.stats) 
			})
		
		def expected = "борода[борода/noun:inanim:f:v_naz] кікука[кікука/unknown].[./punct]\n".repeat(count) + "А[а/conj:coord,а/intj,а/part]\n\n"
		assertEquals(expected, byteOS.toString("UTF-8") + "\n")
		assertEquals(new HashMap<>(["кікука": count]), new HashMap<>(tagText.stats.unknownMap))
	}

	@Test
	public void testSingleThread() {
		def ret = []
		
		def byteOS = new ByteArrayOutputStream(10000)
		def out = new PrintStream(byteOS)
		def count = 8
        def input = new ByteArrayInputStream(("борода кікука.\n\n".repeat(count) + "А").getBytes("UTF-8"))
		
        tagText.setOptions(new TagOptions(outputFormat: OutputFormat.txt, unknownStats: true))
        
		tagUtils.processFile(input, out, options,
			{ buffer ->  
            	return tagText.tagText(buffer)
			}, 
			{ TagResult result ->
				tagText.stats.add(result.stats) 
			})
		
        String expected = "борода[борода/noun:inanim:f:v_naz] кікука[кікука/unknown].[./punct]\n".repeat(count) + "А[а/conj:coord,а/intj,а/part]\n\n"
		assertEquals(expected, byteOS.toString("UTF-8") +"\n")
		assertEquals(new HashMap<>(["кікука": count]), new HashMap<>(tagText.stats.unknownMap))
	}
}
