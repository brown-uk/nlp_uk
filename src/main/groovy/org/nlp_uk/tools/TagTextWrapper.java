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
 */
public class TagTextWrapper {
    // only "." is supported for now
    private static final String SCRIPT_DIR = ".";

    public static void main(String[] args) throws Exception {

        Binding binding = new Binding();
        GroovyScriptEngine engine = new GroovyScriptEngine(SCRIPT_DIR);

        Class<?> clazz = engine.loadScriptByName("TagText.groovy");

        for(int i=0; i<100; i++) {
            clazz.getDeclaredMethod("main", String[].class).invoke(null, (Object)args);
            System.out.println("Done: " + i);
        }
    }
}
