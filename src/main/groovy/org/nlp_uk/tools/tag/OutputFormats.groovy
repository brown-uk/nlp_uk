package org.nlp_uk.tools.tag

import org.apache.groovy.json.internal.CharBuf
import org.nlp_uk.tools.TagText.OutputFormat
import org.nlp_uk.tools.TagText.TTR
import org.nlp_uk.tools.TagText.TagOptions
import org.nlp_uk.tools.TagText.TaggedToken

import groovy.json.DefaultJsonGenerator
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import groovy.transform.CompileStatic
import groovy.xml.MarkupBuilder

//@CompileStatic
class OutputFormats {
    static final boolean useXmlBuilder = true
    
    final TagOptions options

    StringWriter writer
    MarkupBuilder xmlBuilder
    JsonBuilder jsonBuilder
    JsonGenerator jsonGenerator
    
    
    OutputFormats(TagOptions options) {
        this.options = options
    }
    
    void init() {

        if( options.outputFormat == OutputFormat.xml ) {
//            initXmlBuilder()
        }
        else if( options.outputFormat == OutputFormat.json ) {
            def options = new JsonGenerator.Options() \
                .disableUnicodeEscaping()
                .excludeNulls()

//            jsonGenerator = options.build()
            
            jsonGenerator = new DefaultJsonGenerator(options) {
                @Override
                public String toJson(Object object) {
                    CharBuf buffer = CharBuf.create(4*1024) // bigger buffer - huge speedup
                    buffer.append("  ")
                    writeObject(object, buffer)
                    return buffer.toString()
                    // TODO: pretify with indent without losing performance
                    // jsonOut = JsonOutput.prettyPrint(jsonOut)
                    // jsonOut = StringEscapeUtils.unescapeJavaScript(jsonOut)
                    // jsonOut = jsonOut.replaceAll(/(?m)^(.)/, '        $1')
                }
            }
                    
            jsonBuilder = new JsonBuilder(jsonGenerator)
        }
    }

    private initXmlBuilder() {
        writer = new StringWriter(4*1024)
        xmlBuilder = new MarkupBuilder(writer)
        xmlBuilder.setDoubleQuotes(true)
        xmlBuilder.setOmitNullAttributes(true)
    }
    
    @CompileStatic
    CharSequence outputSentenceXml(List<TTR> taggedObjects) {
        if( useXmlBuilder ) {
            return xmlWithBuilder(taggedObjects)
//            return xmlWithDom(taggedObjects)
        }
        
        // XmlBuilder is nice but using strings gives almost 20% speedup on large files
        return xmlDirect(taggedObjects)
    }
    
    @CompileStatic
    CharSequence outputSentenceJson(List<TTR> tokenReadingsList) {
        return jsonWithGenerator(tokenReadingsList)
//        return jsonDirect(tokenReadingsList)
    }

    CharSequence xmlWithBuilder(List<TTR> taggedObjects) {
        initXmlBuilder()
        
        xmlBuilder.'sentence'() {
            if( options.tokenFormat ) {
                taggedObjects.each { tr -> tr
//                    'tokens'() {
                        tr.tokens[0].each { t ->
                           'token'(value: t.value, lemma: t.lemma, tags: t.tags, semtags: t.semtags, q: t.q) {
                               if( t.alts ) {
                                   'alts'() {
                                       t.alts.each { a ->
                                           'token'(value: a.value, lemma: a.lemma, tags: a.tags, semtags: a.semtags, q: a.q)
                                       }                               
                                   }
                               }
                           }
//                        }
                    }
                }
            }
            else {
                taggedObjects.each { tr -> tr
                    'tokenReading'() {
                        tr.tokens.each { t ->
                           'token'(value: t.value, lemma: t.lemma, tags: t.tags, whitespaceBefore: t.whitespaceBefore, semtags: t.semtags)
                        }
                    }
                }
            }
        }
        
        CharSequence str = writer.getBuffer()
        return str
    }
    
    
//    CharSequence xmlWithDom(List<TTR> taggedObjects) {
//        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
//        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
//        Document doc = docBuilder.newDocument();
//
//        Element rootElement = doc.createElement("sentence");
//        doc.appendChild(rootElement);
//        Element parentElement = rootElement
//        taggedObjects.each { TTR tr ->
//            if( ! options.tokenFormat ) {
//                Element child = doc.createElement("tokenReading")
//                parentElement.appendChild(child)
//            }
//            tr.tokens.each { TaggedToken t ->
//                Element child = doc.createElement("token")
//                child.setAttribute("value", t.value)
//                parentElement.appendChild(child)
//            }
//        }
//        TransformerFactory transformerFactory = TransformerFactory.newInstance();
//        Transformer transformer = transformerFactory.newTransformer();
//        DOMSource source = new DOMSource(doc);
//        StreamResult result = new StreamResult(new StringWriter(4*1024));
//        transformer.transform(source, result);
//        
//        return result.getWriter().toString()
//    }

    CharSequence jsonWithBuilder(List<TTR> taggedObjects) {
        if( options.tokenFormat ) {
            jsonBuilder(taggedObjects[0])
        }
        else {
            jsonBuilder {
                taggedObjects.each { tr -> tr
                    'tokenReading'() {
                        tr.tokens.each { t ->
                            'token'(value: t.value, lemma: t.lemma, tags: t.tags, whitespaceBefore: t.whitespaceBefore, semtags: t.semtags)
                        }
                    }
                }
            }
        }

        return jsonBuilder.toString()
    }

    @CompileStatic
    CharSequence jsonWithGenerator(List<TTR> taggedObjects) {
        String jsonOut = options.tokenFormat
            ? jsonGenerator.toJson(taggedObjects.collect{ token: it })
            : jsonGenerator.toJson([tokenReadings: taggedObjects])
        return jsonOut
    }

    @CompileStatic
    CharSequence xmlDirect(List<TTR> taggedObjects) {
        StringBuilder sb = new StringBuilder(1024)
        sb.append("<sentence>\n");
        taggedObjects.each { TTR tr ->
            if( ! options.tokenFormat ) {
                sb.append("  <tokenReading>\n")
            }
            tr.tokens.each { TaggedToken t ->
                appendToken(t, sb)
            }
            if( ! options.tokenFormat ) {
                sb.append("  </tokenReading>\n")
            }
        }
        sb.append("</sentence>");
        return sb
    }

    @CompileStatic
    private void appendToken(TaggedToken t, StringBuilder sb) {
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
        
//    @CompileStatic
//    StringBuilder jsonDirect(List<TTR> tokenReadingsList) {
//        // JsonBuilder is nice but using strings gives almost 40% speedup on large files
//        StringBuilder sb = new StringBuilder(1024)
//        sb.append("    {\n");
//        sb.append("      \"tokenReadings\": [\n");
//        tokenReadingsList.eachWithIndex { TTR tr, trIdx ->
//            sb.append("        {\n");
//            sb.append("          \"tokens\": [\n");
//            
//            tr.tokens.eachWithIndex { TaggedToken t, tIdx ->
//                sb.append("            { ")
//                sb.append("\"value\": \"").append(quoteJson(t.value)).append("\"")
//                if( t.lemma != null ) {
//                    sb.append(", \"lemma\": \"").append(quoteJson(t.lemma)).append("\"")
//                }
//                if( t.tags != null ) {
//                    sb.append(", \"tags\": \"").append(t.tags).append("\"")
//                    if( t.tags == "punct" ) {
//                        sb.append(", \"whitespaceBefore\": ").append(t.whitespaceBefore) //.append("")
//                    }
//                }
//                if( t.semtags ) {
//                    sb.append(", \"semtags\": \"").append(t.semtags).append("\"")
//                }
//                sb.append(" }");
//                if( tIdx < tr.tokens.size() - 1 ) {
//                    sb.append(",")
//                }
//                sb.append("\n")
//            }
//
//            sb.append("          ]");
//            sb.append("\n        }");
//            if( trIdx < tokenReadingsList.size() - 1 ) {
//                sb.append(",")
//            }
//            sb.append("\n")
//        }
//        sb.append("      ]\n");
//        sb.append("    }");
//        return sb
//    }
//    
//    @CompileStatic
//    static String quoteJson(String s) {
//        s.replace('"', '\\"')
//    }

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
