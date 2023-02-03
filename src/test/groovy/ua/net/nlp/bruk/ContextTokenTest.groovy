package ua.net.nlp.bruk

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test


public class ContextTokenTest {

	@Test
	public void testNumbers() {
        def r = ContextToken.normalizeContextString("89", "89", "number")
        assertEquals "0", r

        r = ContextToken.normalizeContextString("4", "4", "number")
        assertEquals "2", r

        r = ContextToken.normalizeContextString("15", "15", "number")
        assertEquals "0", r
        
        r = ContextToken.normalizeContextString("13", "13", "number")
        assertEquals "0", r
        
        r = ContextToken.normalizeContextString("23", "23", "number")
        assertEquals "2", r
        
        r = ContextToken.normalizeContextString("21", "21", "number")
        assertEquals "1", r

        r = ContextToken.normalizeContextString("0,2-0,3", "0,0", "number")
        assertEquals "0,0", r

        r = ContextToken.normalizeContextString("1999", "1999", "number")
        assertEquals "YY99", r

        r = ContextToken.normalizeContextString("1999-2020", "1999-2020", "number")
        assertEquals "YY20", r

        r = ContextToken.normalizeContextString("2999", "3999", "number")
        assertEquals "0", r
	}

    @Test
    public void testSymbols() {
        def r = ContextToken.normalizeContextString("один\u2013два", "один-два", "numr")
        assertEquals "один-два", r

        r = ContextToken.normalizeContextString("\u2014", "\u2014", "punct")
        assertEquals "-", r

        r = ContextToken.normalizeContextString("?..", "?..", "punct")
        assertEquals "?", r
    }
}
