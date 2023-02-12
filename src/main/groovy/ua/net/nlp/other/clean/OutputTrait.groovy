package ua.net.nlp.other.clean

import java.nio.charset.StandardCharsets

import org.slf4j.Logger

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@CompileStatic
public class OutputTrait {
    Logger logger
    ThreadLocal<PrintStream> out = new ThreadLocal<>()
    ThreadLocal<ByteArrayOutputStream> outSw = new ThreadLocal<>()
    CleanOptions options

    public void setLogger(Logger logger) {
        this.logger = logger
    }            

    void init() {
        if( options.parallel ) { // each file gets new output
            def byteStream = new ByteArrayOutputStream()
            outSw.set(byteStream)
            out.set(new PrintStream(byteStream))
        }
        else {
            out.set(System.out)
        }
    }
    
    void flush() {
        if( options.parallel ) {
            out.get().flush()
            System.out.println(outSw.get().toString(StandardCharsets.UTF_8))
        }
    }
    
    void debug(str) {
        if( logger ) {
            logger.info str.toString()
        }
        else {
            if( options.debug ) {
                out.println "\tDEBUG: $str"
            }
        }
    }

    // making println thread-safe
    void println(Object str) {
        if( logger ) {
            logger.info str.toString()
        }
        else {
            out.get().println(str)
        }
    }
}
