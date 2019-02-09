import java.io.File;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

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
    // only "." is supported for now
    private static final String SCRIPT_DIR = ".";

    public static void main(String[] args) throws Exception {

        Binding binding = new Binding();
        GroovyScriptEngine engine = new GroovyScriptEngine(SCRIPT_DIR);

        Class<?> clazz = engine.loadScriptByName("TagText.groovy");


// Example 1: for simple 1-time invocation
//        clazz.getDeclaredMethod("main", String[].class).invoke(null, (Object)args);

// Example 2: for multiple files
// Note: for multi-threaded approach create separate tagText instance for each thread
        java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor();
        Object tagText = constructor.newInstance();

        for(int i=0; i<10; i++) {
            Object options = clazz.getDeclaredMethod("parseOptions", String[].class).invoke(null, (Object)new String[]{"-i", "text.txt"});
            clazz.getDeclaredMethod("setOptions", Object.class).invoke(tagText, options);
            clazz.getDeclaredMethod("process").invoke(tagText);
            System.out.println("Done tagging: " + i);
        }
    }

}
