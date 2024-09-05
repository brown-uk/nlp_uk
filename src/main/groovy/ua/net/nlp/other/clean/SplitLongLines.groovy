package ua.net.nlp.other.clean

import groovy.transform.CompileStatic

@CompileStatic
class SplitLongLines {
    int maxLength = 1024
    int minLength = 64
    
    String split(String text) {
        def lines = text.readLines()
        def lineCnt = lines.size()
        if( lineCnt > 2 )
            return null
        
        if( ! lines.find{ l -> l.length() > maxLength} )
            return null

        if( lineCnt > 1 ) {
            def p = ~/^([А-ЯІЇЄҐA-Z ,'0-9«»".:!?()-]+)\h+\n?([А-ЯІЇЄҐ]'?[а-яіїєґ])/
            def m = p.matcher(text)
            if( m.find() ) {
                text = m.replaceFirst('$1\n\n$2')
            }
            else if( lines[0] =~ /[а-яіїєґ]\h*$/ ) {
                text = text.replaceFirst(/\n/, '\n\n')
            }
        }
    
        text = text.replaceAll(/(.{/+minLength+/}[а-яіїєґА-ЯІЇЄҐ0-9]{3}\.) ([А-ЯІЇЄҐ])/, '$1\n$2')

//        println "-------------------------------"        
//        println text.readLines().take(4).join("\n")
        
        text
    }
    
    static void main(String[] args) {
        def splitLongLines = new SplitLongLines()
        
        new File(args[0]).eachFileRecurse { f ->
            if( ! f.name.toLowerCase().endsWith(".txt") )
                return 
            
            def txt = f.getText('utf-8')
            txt = splitLongLines.split(txt)
            
            if( txt != null ) {
                f.setText(txt, 'utf-8')
                println "Updated: ${f.path}"
            }
        }
        
    }
}
