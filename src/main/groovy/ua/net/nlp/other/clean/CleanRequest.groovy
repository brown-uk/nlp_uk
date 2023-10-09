package ua.net.nlp.other.clean

import groovy.transform.CompileStatic

@CompileStatic
class CleanRequest {
    public String text
    public File file
    public File outFile
    public boolean dosNl
    
    String getLineBreak() { dosNl ? ".\r\n" : ".\n" }
    CleanRequest forText(String text) {
        new CleanRequest(text: text, file: file, outFile: outFile, dosNl: dosNl)
    }
}
