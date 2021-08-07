package org.nlp_uk.tools;

import java.lang.reflect.Method;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;

/**
 * This class shows how to call TagText.groovy
 * If you have multiple text you process in java
 * this way you can avoid starting new jdk for each TagText.groovy invocation
 */
public class TagTextWrapperRun {

    public void tag(String filename, String outFilename) throws Exception {

        GroovyShell shell = new GroovyShell(); //this.class.classLoader, new Binding(), config)  
        Object script = shell.evaluate("org.nlp_uk.tools.TagText.class");

        Class<?> clazz = (Class<?>) script;

// Example 2: for multiple files
// Note: for multi-threaded approach create separate tagText instance for each thread
        java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor();
        Object tagText = constructor.newInstance();
        Method parseOptionsMethod = clazz.getDeclaredMethod("parseOptions", String[].class);
        Method processMethod = clazz.getDeclaredMethod("process");

        for(int i=0; i<4; i++) {
            Object options = parseOptionsMethod.invoke(null, (Object)new String[]{"-i", filename, "-o", outFilename, "-e", "-x"});
            Method setOptionsMethod = clazz.getDeclaredMethod("setOptions", options.getClass());
            setOptionsMethod.invoke(tagText, options);
            processMethod.invoke(tagText);
            System.out.println("Done tagging: " + i);
        }
    }

    public static void main(String[] args) throws Exception {
        new TagTextWrapper().tag(args[0], args[1]);
    }

}
