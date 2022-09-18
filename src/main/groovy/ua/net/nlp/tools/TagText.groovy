#!/usr/bin/env groovy

package ua.net.nlp.tools

import java.nio.charset.StandardCharsets

// A wrapper to load tag/TagTextCore.groovy with all related classes and resources without complicating CLI

class TagText {

    @groovy.transform.SourceURI
    static SOURCE_URI
    static SCRIPT_DIR=new File(SOURCE_URI).parent

    static void main(String[] args) {
        warnForEncoding()
        
        long tm1 = System.currentTimeMillis()
        
        def cl = new GroovyClassLoader()
        cl.addClasspath(SCRIPT_DIR + "/../../../../")
        def resourceDir = SCRIPT_DIR + "/../../../../../resources"
        if( ! new File(resourceDir).isDirectory() ) {
//            println "making missing dir: $resourceDir"
            new File(resourceDir).mkdirs()
        }
        cl.addClasspath(resourceDir)
        
        def basePkg = TagText.class.getPackageName()
        def tagTextClass = cl.loadClass("${basePkg}.tag.TagTextCore")
        def m = tagTextClass.getMethod("main", String[].class)
        def mArgs = [args].toArray() // new Object[]{args} - Eclips chokes on this

        long tm2 = System.currentTimeMillis()

        if( "--timing" in args ) {        
            System.err.println("Loaded classes in ${tm2-tm1} ms")
        }
        m.invoke(null, mArgs)
    }

    private static void warnForEncoding() {
        String osName = System.getProperty("os.name").toLowerCase();
        if ( osName.contains("windows")) {
            if( ! "UTF-8".equals(System.getProperty("file.encoding"))
                    || ! StandardCharsets.UTF_8.equals(java.nio.charset.Charset.defaultCharset()) ) {
                System.setOut(new PrintStream(System.out,true,"UTF-8"))
        
                println "file.encoding: " + System.getProperty("file.encoding")
                println "defaultCharset: " + java.nio.charset.Charset.defaultCharset()
        
                println "On Windows to get unicode handled correctly you need to set environment variable before running expand:"
                println "\tbash:"
                println "\t\texport JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8"
                println "\tPowerShell:"
                println "\t\t\$env:JAVA_TOOL_OPTIONS=\"-Dfile.encoding=UTF-8\""
                println "\tcmd:"
                println "\t\t(change Font to 'Lucida Console' in cmd window properties)"
                println "\t\tchcp 65001"
                println "\t\tset JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8"
            }
        }
    }
    
}
