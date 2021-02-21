package org.nlp_uk.tools;

import java.lang.reflect.Method;

import groovy.util.GroovyScriptEngine;

/**
 * This class shows how to call TagText.groovy
 * If you have multiple text you process in java
 * this way you can avoid starting new jdk for each TagText.groovy invocation
 *
 * compile: javac -cp ".:$GROOVY_PATH/lib/*" TagTextWrapper.java
 * run: java -cp ".:$GROOVY_PATH/lib/*" TagTextWrapper -i <filename>
 *
 * Note: both TagText.groovy and TextUtils.groovy must be in current directory
 * Note: you may want to comment out @Grab...logback-classic in TagText.groovy if you have your own logging framework
 */
public class TagTextWrapper {
    private static final String SCRIPT_DIR = "src/main/groovy/org/nlp_uk/tools";

    public void tag(String filename, String outFilename) throws Exception {

//        Binding binding = new Binding();
        GroovyScriptEngine engine = new GroovyScriptEngine(SCRIPT_DIR);

        engine.loadScriptByName("TextUtils.groovy");
        Class<?> clazz = engine.loadScriptByName("TagText.groovy");


// Example 1: for simple 1-time invocation
//        clazz.getDeclaredMethod("main", String[].class).invoke(null, (Object)args);

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

}
