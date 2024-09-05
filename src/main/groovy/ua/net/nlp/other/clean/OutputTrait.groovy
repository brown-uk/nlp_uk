package ua.net.nlp.other.clean

import java.nio.charset.StandardCharsets

import org.slf4j.Logger

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@CompileStatic
@PackageScope
class OutputTrait {
    Logger logger
    CleanOptions options
    ByteArrayOutputStream byteStream
    PrintStream out

    OutputTrait() {
        init()
    }
    
    void init() {
        byteStream = new ByteArrayOutputStream(1024)
        out = new PrintStream(byteStream)
    }
    
    public void setLogger(Logger logger) {
        this.logger = logger
    }            

    synchronized void flushAndPrint() {
        out.flush()
        System.out.println(byteStream.toString(StandardCharsets.UTF_8))
        init()
    }
    
    void debug(str) {
        if( logger ) {
            logger.debug str.toString()
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
            out.println(str)
        }
    }
    
    void append(OutputTrait localOut) {
        localOut.out.flush()
        out.append(localOut.byteStream.toString(StandardCharsets.UTF_8))
    }
}
