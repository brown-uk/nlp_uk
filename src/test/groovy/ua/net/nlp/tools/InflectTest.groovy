package ua.net.nlp.tools

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import ua.net.nlp.other.Inflect


class InflectTest {

    @Test
    void test() {
        Inflect inflectText = new Inflect()
        def res = inflectText.inflectWord("місто", "noun:inanim:n:v_rod.*", true)
        def expected = ["міста"]
        assertEquals expected, Arrays.asList(res)
    }

}
