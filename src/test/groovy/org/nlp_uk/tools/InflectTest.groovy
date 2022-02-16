package org.nlp_uk.tools

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import groovy.json.JsonSlurper


class InflectTest {

    @Test
    void test() {
        Inflect inflectText = new Inflect()
        def res = inflectText.inflectWord("місто", "noun:inanim:n:v_rod.*", true)
        def expected = ["міста"]
        assertEquals expected, Arrays.asList(res)
    }

}
