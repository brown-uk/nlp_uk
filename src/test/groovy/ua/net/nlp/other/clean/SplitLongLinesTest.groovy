package ua.net.nlp.other.clean;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test;

class SplitLongLinesTest {
    SplitLongLines splitLongLines = new SplitLongLines();

    @BeforeEach
    void init() {
        splitLongLines.maxLength = 30
        splitLongLines.minLength = 20
    }
    
    @Test
    void test() {
        def txt = "ЛЮБИЙ ДРУЖЕ! Героїв цієї книжки ти часто бачив у мультиках. Так-так. Усі вони постійно потрапляють у скрутне становище\nі от"
        def result = splitLongLines.split(txt)
        
        def expected =
"""ЛЮБИЙ ДРУЖЕ!

Героїв цієї книжки ти часто бачив у мультиках.
Так-так. Усі вони постійно потрапляють у скрутне становище
і от"""
        
        assertEquals expected, result
        
        txt = """ГЕНЕЗИС ШПИГУНСЬКОГО РОМАНУ: ВІД Р. КІПЛІНГА ДО Я. ФЛЕМІНГА 
Постановка проблеми. У цей час спостерігається стійкий інтерес до вивчення проблеми. Ідеологічного впливу на взаємні"""
        result = splitLongLines.split(txt)
        
        expected =
"""ГЕНЕЗИС ШПИГУНСЬКОГО РОМАНУ: ВІД Р. КІПЛІНГА ДО Я. ФЛЕМІНГА

Постановка проблеми. У цей час спостерігається стійкий інтерес до вивчення проблеми.
Ідеологічного впливу на взаємні"""
        
        assertEquals expected, result
    }

    
    @Test
    void testNoTouch() {
        def txt = "ЛЮБИЙ ДРУЖЕ!\nГероїв цієї книжки ти часто бачив у мультиках.\nУсі вони постійно потрапляють у скрутне становище"
        def result = splitLongLines.split(txt)

        assertEquals null, result
    }
}
