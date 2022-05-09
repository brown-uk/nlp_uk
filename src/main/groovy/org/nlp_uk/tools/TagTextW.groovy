#!/bin/env groovy

package org.nlp_uk.tools

// A wrapper to load TagText.groovy with all related classes without complicating CLI

class TagTextW {

    @groovy.transform.SourceURI
    static SOURCE_URI
    static SCRIPT_DIR=new File(SOURCE_URI).parent

    static void main(String[] args) {
        def cl = new GroovyClassLoader()
//        println SCRIPT_DIR
        cl.addClasspath(SCRIPT_DIR + "/../../../")
        
        def tagTextClass = cl.loadClass("org.nlp_uk.tools.TagText")
        def m = tagTextClass.getMethod("main", String[].class)
//        println m.parameterTypes
        m.invoke(null, new Object[]{args})
    }

}
