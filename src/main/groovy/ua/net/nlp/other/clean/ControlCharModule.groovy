package ua.net.nlp.other.clean

import java.util.function.Function
import java.util.regex.MatchResult
import java.util.regex.Matcher
import java.util.regex.Pattern

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@PackageScope
@CompileStatic
class ControlCharModule {
    
    private final Pattern CONTROL_CHAR_PATTERN_R = Pattern.compile(/[\u0000-\u0008\u000B-\u0012\u0014-\u001F\u0A0D]/, Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE)
    private final Pattern CONTROL_CHAR_PATTERN_W = Pattern.compile(/([а-яіїєґ'\u2019\u02BC\u0301-]+)[\u0000-\u0008\u000B-\u0012\u0014-\u001F\u0A0D]\n?([а-яіїєґ'\u2019\u02BC\u0301-]+)/, Pattern.MULTILINE|Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE)
    
    OutputTrait out
    LtModule ltModule
    
    @CompileStatic
    String removeControlChars(String text) {
        
        def m = text =~ CONTROL_CHAR_PATTERN_W
        
        if( m ) {
            out.println "\treplacing control characters inside words"
            text = m.replaceAll{ mr ->
                def fix = "${mr.group(1)}-${mr.group(2)}"
                if( ltModule.knownWord(fix) ) return fix
                fix = "${mr.group(1)}${mr.group(2)}"
                if( ltModule.knownWord(fix) ) return fix
                return mr.group(0)
            }
        }

        def m2 = text =~ CONTROL_CHAR_PATTERN_R
        
        if( m2 ) {
            out.println "\tremoving standalone control characters"
            text = m2.replaceAll('')
        }
        return text
    }
}
