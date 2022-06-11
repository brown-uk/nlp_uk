#!/usr/bin/env groovy

package ua.net.nlp.tools

// A wrapper to load TagText.groovy with all related classes without complicating CLI

class TagTextW {

    @groovy.transform.SourceURI
    static SOURCE_URI
    static SCRIPT_DIR=new File(SOURCE_URI).parent

    static void main(String[] args) {
        def cl = new GroovyClassLoader()
        cl.addClasspath(SCRIPT_DIR + "/../../../../")
        cl.addClasspath(SCRIPT_DIR + "/../../../../../resources")
        
        def basePkg = TagTextW.class.getPackageName()
        def tagTextClass = cl.loadClass("${basePkg}.TagText")
        def m = tagTextClass.getMethod("main", String[].class)
        def mArgs = [args].toArray() // new Object[]{args} - Eclips chokes on this
        
        System.err.println("---")
        m.invoke(null, mArgs)
    }

}
