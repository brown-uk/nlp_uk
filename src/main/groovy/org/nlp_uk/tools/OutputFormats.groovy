package org.nlp_uk.tools

import groovy.transform.CompileStatic
import org.nlp_uk.tools.TagText.TagOptions

class OutputFormats {
    
    TagOptions options
    
    OutputFormats(TagOptions options) {
        this.options = options
    }
    
    
    StringBuilder outputSentenceXml(taggedObjects) {
//        builder.'sentence'() {
//            tokenReadings.each { tr -> tr
//                'tokenReading'() {
//                    tr.tokens.each { t ->
//                       'token'(t)
//                    }
//                }
//            }
//        }
        
        // XmlBuilder is nice but using strings gives almost 20% speedup on large files
        StringBuilder sb = new StringBuilder(1024)
        sb.append("<sentence>\n");
        taggedObjects.each { tr -> tr
            if( ! options.tokenFormat ) {
                sb.append("  <tokenReading>\n")
            }
            tr.tokens.each { t ->
                appendToken(t, sb)
            }
            if( ! options.tokenFormat ) {
                sb.append("  </tokenReading>\n")
            }
        }
        sb.append("</sentence>");
        return sb
    }

    private void appendToken(t, StringBuilder sb) {
        String indent = options.tokenFormat ? "  " : "    "
        sb.append(indent).append("<token value=\"").append(quoteXml(t.value, false)).append("\"")
        if( t.lemma != null ) {
            sb.append(" lemma=\"").append(quoteXml(t.lemma, false)).append("\"")
        }
        if( t.tags ) {
            sb.append(" tags=\"").append(quoteXml(t.tags, false)).append("\"")

            if( ! options.tokenFormat ) {
                if( t.tags == "punct" ) {
                    sb.append(" whitespaceBefore=\"").append(t.whitespaceBefore).append("\"")
                }
            }
        }
        if( t.semtags ) {
            sb.append(" semtags=\"").append(quoteXml(t.semtags, false)).append("\"")
        }
        if( t.q != null ) {
            sb.append(" q=\"").append(t.q).append("\"")
        }

        if( t.alts ) {
            sb.append(">\n    <alts>\n")
            t.alts.each { ti ->
                sb.append("    ")
                appendToken(ti, sb)
            }
            sb.append("    </alts>\n").append(indent).append("</token>\n")
        }
        else {
            sb.append(" />\n")
        }
    }
    
    StringBuilder outputSentenceJson(tokenReadingsList) {
//        builder {
//            tokenReadings tokenReadingsList.collect { tr ->
//                [
//                    tokens: tr.tokens.collect { t ->
//                        t
//                    }
//                ]
//            }
//        }
//        String jsonOut = builder.toString()
//        jsonOut = JsonOutput.prettyPrint(jsonOut)
//        jsonOut = StringEscapeUtils.unescapeJavaScript(jsonOut)
//        jsonOut = jsonOut.replaceAll(/(?m)^(.)/, '        $1')
//        return jsonOut

        // JsonBuilder is nice but using strings gives almost 40% speedup on large files
        StringBuilder sb = new StringBuilder(1024)
        sb.append("    {\n");
        sb.append("      \"tokenReadings\": [\n");
        tokenReadingsList.eachWithIndex { tr, trIdx -> tr
            sb.append("        {\n");
            sb.append("          \"tokens\": [\n");
            
            tr.tokens.eachWithIndex { t, tIdx ->
                sb.append("            { ")
                sb.append("\"value\": \"").append(quoteJson(t.value)).append("\"")
                if( t.lemma != null ) {
                    sb.append(", \"lemma\": \"").append(quoteJson(t.lemma)).append("\"")
                }
                if( t.tags != null ) {
                    sb.append(", \"tags\": \"").append(t.tags).append("\"")
                    if( t.tags == "punct" ) {
                        sb.append(", \"whitespaceBefore\": ").append(t.whitespaceBefore) //.append("")
                    }
                }
                if( t.semtags ) {
                    sb.append(", \"semtags\": \"").append(t.semtags).append("\"")
                }
                sb.append(" }");
                if( tIdx < tr.tokens.size() - 1 ) {
                    sb.append(",")
                }
                sb.append("\n")
            }

            sb.append("          ]");
            sb.append("\n        }");
            if( trIdx < tokenReadingsList.size() - 1 ) {
                sb.append(",")
            }
            sb.append("\n")
        }
        sb.append("      ]\n");
        sb.append("    }");
        return sb
    }
    
    @CompileStatic
    static String quoteJson(String s) {
        s.replace('"', '\\"')
    }
    @CompileStatic
    static String quoteXml(String s, boolean withApostrophe) {
//        XmlUtil.escapeXml(s)
        // again - much faster on our own
        s = s.replace('&', "&amp;").replace('<', "&lt;").replace('>', "&gt;").replace('"', "&quot;")
        if( withApostrophe ) {
            s = s.replace('\'', "&apos;")
        }
        s
    }

}
