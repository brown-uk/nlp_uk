package ua.net.nlp.bruk

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class WordReading {
    public String lemma
    public String postag
    
    @CompileStatic
    WordReading(String lemma, String postag) {
        this.lemma = lemma
        this.postag = postag
    }
    
    @CompileStatic
    String toString() {
        "$lemma\t$postag"
    }
}
