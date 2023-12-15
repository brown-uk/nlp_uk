package ua.net.nlp.other.clean

import java.util.function.Function
import java.util.regex.MatchResult
import java.util.regex.Matcher
import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@PackageScope
@CompileStatic
class ControlCharModule {
    
    private final Pattern CONTROL_CHAR_PATTERN_R = Pattern.compile(/[\u0000-\u0008\u000B-\u0012\u0014-\u001F\u0A0D]/, Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE)
    private final Pattern CONTROL_CHAR_PATTERN_W = Pattern.compile(/([а-яіїєґ'\u2019\u02BC\u0301-]+)[\u0000-\u0008\u000B-\u0012\u0014-\u001F\u0A0D]\n?([а-яіїєґ'\u2019\u02BC\u0301-]+)/, Pattern.MULTILINE|Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE)
    private final Pattern PRIVATE_BLOCK_CHARS = Pattern.compile(/[\uE000-\uF8FF]/)
    
    OutputTrait out
    LtModule ltModule
    
    String removeControlChars(String text) {
        text = text.replace("\u200E", "")
        
        text = remove001DHyphens(text)
        
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
        
        def ctrlM = CONTROL_CHAR_PATTERN_R.matcher(text)
        if( ctrlM ) {
            def ctx = CleanUtils.getContext(text, ctrlM.group(0))
            out.println "\tWARNING: still control characters present: $ctx"
        }
        
        def privM = PRIVATE_BLOCK_CHARS.matcher(text)
        if( privM ) {
            def ctx = CleanUtils.getContext(text, privM.group(0))
            out.println "\tWARNING: private area characters - needs manual analysis: $ctx"
        }
        
        return text
    }
    
    private String remove001DHyphens(String text) {
        if( text.contains("\u001D") ) {
            out.println "\tremoving U+001D"
//            out.println "\treplacing U+001D with U+00AC"
//            text = text.replace("\u001D", "\u00AC")
            text = text.replaceAll(/(?iu)([а-яіїєґ])\u001D\n(\h*)([а-яіїєґ'\u2019\u20BC-]+)/, '$1$3\n$2')
            text = text.replaceAll(/(?iu)([а-яіїєґ])\u001D([а-яіїєґ])/, '$1-$2')
        }
        return text
    }

}
