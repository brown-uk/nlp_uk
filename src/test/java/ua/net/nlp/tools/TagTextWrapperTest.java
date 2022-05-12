package ua.net.nlp.tools;

import java.io.File;

import org.junit.jupiter.api.Test;

public class TagTextWrapperTest {

	@Test
	public void testTagTextWrapper() throws Exception {
		TagTextWrapper wrapper = new TagTextWrapper();
		new File("build/tmp").mkdirs();
		wrapper.tag("src/test/resources/tag/simple.txt", "build/tmp/simple.tagged.txt");
	}

}