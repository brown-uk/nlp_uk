#!/bin/env groovy

class CleanTextTest extends GroovyTestCase {
    def options = [ "wordCount": 20, "debug": true ]

    CleanText ct = new CleanText( options )

    def file() { return new File("/dev/null") }

    String clean(String str) {
        println "---------"
        str = str.replace('_', '')
        ct.cleanUp(str, file(), options)
    }

    public void test() {


        assertEquals "брат", clean("б_p_ат")

        assertEquals "труба", clean("тр_y_ба")

        assertEquals "on throughпортал в", clean("on throughпортал в")

        assertEquals "урахування\n", clean("ураху-\nвання")

        assertEquals "екс-«депутат»\n", clean("екс-\n«депутат»")

    }
}
