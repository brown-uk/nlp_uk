package ua.net.nlp.bruk

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class WordContext {
    ContextToken contextToken
    int offset
    
    @CompileStatic
    WordContext(ContextToken contexToken, int offset) {
        this.contextToken = contexToken
        this.offset = offset
    }
    
    @CompileStatic
    String toString() {
        def offs = offset > 0 ? "+$offset" : "$offset"
        "$offs\t$contextToken"
    }
}
