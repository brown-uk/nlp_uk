package ua.net.nlp.other.clean

import groovy.transform.CompileStatic

@CompileStatic
abstract class CleanUtils {

    static String escapeNl(String text) {
        text.replace("\r", "\\r").replace("\n", "\\n")
    } 
    
    static String getContext(String text, String str) {
        int idx = text.indexOf(str)
        if( idx == -1 )
            return ""

        int from = Math.max(idx - 10, 0)
        int to = Math.min(idx + 10, text.length()-1)
        return escapeNl(text.substring(from, to))
    }

}
