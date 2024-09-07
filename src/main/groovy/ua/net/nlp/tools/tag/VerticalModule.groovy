package ua.net.nlp.tools.tag

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import ua.net.nlp.tools.tag.TagTextCore.TTR
import ua.net.nlp.tools.tag.TagTextCore.TaggedSentence

@CompileStatic
@PackageScope
class VerticalModule {
    
    TagOptions options
    
    void printSentence(TaggedSentence taggedSent, StringBuilder sb, int sentId) {
        if( sb.length() > 0 ) {
            sb.append("\n")
        }
        sb.append("<s>\n")
        
        taggedSent.tokens.each { TTR token ->
            def tkn = token.tokens[0]
            if( tkn.tags == 'punct' && ! tkn.whitespaceBefore ) {
                sb.append('<g/>\n')
            }
            sb.append("${tkn.value}\t${tkn.tags}\t${tkn.lemma}")
            if( options.semanticTags ) {
                sb.append("\t${tkn.semtags?:''}")
            }
            sb.append('\n')
        }

        sb.append("</s>\n")
    }
}
