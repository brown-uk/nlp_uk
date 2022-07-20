#!/usr/bin/env groovy

package ua.net.nlp.tools

// A wrapper to load TagText.groovy with all related classes without complicating CLI

class TagTextW {

    @groovy.transform.SourceURI
    static SOURCE_URI
    static SCRIPT_DIR=new File(SOURCE_URI).parent

    static void main(String[] args) {
        long tm1 = System.currentTimeMillis()
        
        def cl = new GroovyClassLoader()
        cl.addClasspath(SCRIPT_DIR + "/../../../../")
        cl.addClasspath(SCRIPT_DIR + "/../../../../../resources")
        
        def basePkg = TagTextW.class.getPackageName()
        def tagTextClass = cl.loadClass("${basePkg}.TagText")
        def m = tagTextClass.getMethod("main", String[].class)
        def mArgs = [args].toArray() // new Object[]{args} - Eclips chokes on this

        long tm2 = System.currentTimeMillis()

        if( "--timing" in args ) {        
            System.err.println("Loaded classes in ${tm2-tm1} ms")
        }
        m.invoke(null, mArgs)
    }

}
