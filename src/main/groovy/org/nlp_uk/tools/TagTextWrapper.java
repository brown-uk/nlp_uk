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
 */
public class TagTextWrapper {

    public static void main(String[] args) throws Exception {

        Binding binding = new Binding();
        GroovyScriptEngine engine = new GroovyScriptEngine(".");

        Class<?> clazz = engine.loadScriptByName("TagText.groovy");

        clazz.getDeclaredMethod("main", String[].class).invoke(null, (Object)args);
    }
}
