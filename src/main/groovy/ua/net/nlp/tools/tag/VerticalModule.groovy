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
        if( ! taggedSent.tokens ) // <p>
            return
        
        if( sb.length() > 0 ) {
            sb.append("\n")
        }
        sb.append("<s>\n")
        
        taggedSent.tokens.eachWithIndex { TTR token, int i ->
            def tkn = token.tokens[0]
            if( i > 0 && tkn.tags == 'punct' && ! tkn.whitespaceBefore ) {
                sb.append('<g/>\n')
            }
            sb.append("${tkn.value}\t${tkn.tags}\t${tkn.lemma}")
            if( options.semanticTags ) {
                if( tkn.semtags ) {
                    sb.append("\tsemTags=${tkn.semtags}")
                }
                else {
                    sb.append("\t_")
                }
            }
            sb.append('\n')
        }

        sb.append("</s>\n")
    }
}
