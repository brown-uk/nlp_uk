#!/usr/bin/env groovy

package org.nlp_uk.tools

@GrabConfig(systemClassLoader=true)
//@Grab(group='org.languagetool', module='language-uk', version='5.6')
@Grab(group='org.languagetool', module='language-uk', version='5.6-SNAPSHOT')
@Grab(group='ch.qos.logback', module='logback-classic', version='1.2.3')
@Grab(group='info.picocli', module='picocli', version='4.6.+')

import java.util.regex.Pattern
import java.util.stream.Collectors
import org.languagetool.*
import org.languagetool.language.*
import org.nlp_uk.bruk.ContextToken
import org.nlp_uk.bruk.WordContext
import org.nlp_uk.bruk.WordReading
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.util.Eval
import picocli.CommandLine

import picocli.CommandLine.Option
import picocli.CommandLine.ParameterException
import picocli.CommandLine.Parameters


class TagText {
    enum OutputFormat { txt, xml, json }
    
    @groovy.transform.SourceURI
    static SOURCE_URI
    // if this script is called from GroovyScriptEngine SourceURI is data: and does not work for File()
    static SCRIPT_DIR = SOURCE_URI.scheme == "data" 
		? new File("src/main/groovy/org/nlp_uk/tools")
		: new File(SOURCE_URI).parent

    // easy way to include a class without forcing classpath to be set
    def textUtils = Eval.me(new File("$SCRIPT_DIR/TextUtils.groovy").text + "\n new TextUtils()")
    
    static final Pattern PUNCT_PATTERN = Pattern.compile(/[,.:;!?\/()\[\]{}«»„“"'…\u2013\u2014\u201D\u201C•■♦-]+/)
    static final Pattern SYMBOL_PATTERN = Pattern.compile(/[%&@$*+=<>\u00A0-\u00BF\u2000-\u20CF\u2100-\u218F\u2200-\u22FF]+/)
    static final Pattern UNKNOWN_PATTERN = Pattern.compile(/(.*-)?[а-яіїєґА-ЯІЇЄҐ]+(-.*)?/)
    static final Pattern NON_UK_PATTERN = Pattern.compile(/[ыэъёЫЭЪЁ]|[иИ]{2}/)
    static final Pattern UNCLASS_PATTERN = Pattern.compile(/\p{IsLatin}[\p{IsLatin}\p{IsDigit}-]*|[0-9]+-?[а-яіїєґА-ЯІЇЄҐ]+|[а-яіїєґА-ЯІЇЄҐ]+-?[0-9]+/)
    static final Pattern XML_TAG_PATTERN = Pattern.compile(/<\/?[a-zA-Z_0-9]+>/)
    static final Pattern UPPERCASED_PATTERN = Pattern.compile(/[А-ЯІЇЄҐ][а-яіїєґ'-]+/)
    
    def language = new Ukrainian() {
        @Override
        protected synchronized List<?> getPatternRules() { return [] }
    }

    JLanguageTool langTool = new MultiThreadedJLanguageTool(language)

    TagOptions options
	Map<String, Map<String,List<String>>> semanticTags = new HashMap<>()
    Map<String, Map<WordReading, Integer>> disambigStatsF = new HashMap<>()
    Map<String, Map<WordReading, Map<WordContext, Integer>>> disambigStatsC = new HashMap<>()
    Map<String, Integer> disambigStatsT = new HashMap<>().withDefault { 0 }
    //    Map<String, Map<WordContext, Integer>> disambigStats = new HashMap<>()
    
	@Canonical
	static class TagResult {
		String tagged
		Stats stats
	}
	
	Stats stats = new Stats()



    @CompileStatic
    def tagText(String text) {
        List<AnalyzedSentence> analyzedSentences = langTool.analyzeText(text);
		
//		StringWriter writer
//		GroovyObjectSupport builder
	
		if( options.outputFormat == OutputFormat.xml ) {
//			writer = new StringWriter()
//			builder = new MarkupBuilder(writer)
		}
        else if( options.outputFormat == OutputFormat.json ) {
//            def gen = new JsonGenerator.Options() \
//                .disableUnicodeEscaping()
//                .build()
//            builder = new JsonBuilder(gen)
        }

        def sb = new StringBuilder()
        for (AnalyzedSentence analyzedSentence : analyzedSentences) {
            
            analyzedSentence.getTokens().each { AnalyzedTokenReadings t ->
                def multiWordReadings = t.getReadings().findAll { r ->
                    r.getPOSTag() != null && r.getPOSTag().startsWith("<")
                }
                multiWordReadings.each { r ->
                    t.removeReading(r, "remove_multiword")
                }
            }

            if ( ! options.noTag ) {

                if( options.outputFormat == OutputFormat.xml ) {
                    def field = AnalyzedSentence.getDeclaredField("nonBlankTokens")
                    field.setAccessible(true)
                    AnalyzedTokenReadings[] tokens = analyzedSentence.getTokensWithoutWhitespace()

                    def taggedObjects = tagAsObject(tokens)

                    StringBuilder x = outputSentenceXml(taggedObjects)
                    sb.append(x).append("\n")
                    
                    if( options.singleTokenFormat ) {
                        if( tokens[-1].hasPosTag(JLanguageTool.PARAGRAPH_END_TAGNAME) ) {
                            sb.append("<paragraph/>\n")
                        }
                    }

                    //				sb.append(writer.toString()).append("\n");
                    //                writer.getBuffer().setLength(0)
                }
                else if( options.outputFormat == OutputFormat.json ) {
                    AnalyzedTokenReadings[] tokens = analyzedSentence.getTokensWithoutWhitespace()

                    def tokenReadingObj = tagAsObject(tokens)

                    def s = outputSentenceJson(tokenReadingObj)
                    if( sb.length() > 0 ) sb.append(",\n");
                    sb.append(s)
                    //                sb.append("\n");
                }
                else {
                    String sentenceLine
                    sentenceLine = analyzedSentence.toString()
                    sentenceLine.replaceAll(/,[^\/].*?\/<.*?>/, '')
                    if( options.tokenPerLine ) {
                        sentenceLine = sentenceLine.replaceAll(/(<S>|\]) */, '$0\n')
                    }
                    else {
                        sentenceLine = sentenceLine.replaceAll(/ *(<S>|\[<\/S>\]) */, '')
                    }

                    sb.append(sentenceLine) //.append("\n");
                }

                if( options.showDisambig ) {
                    analyzedSentence.tokens.each { AnalyzedTokenReadings it ->
                        def historicalAnnotations = it.getHistoricalAnnotations()
                        if( historicalAnnotations && ! historicalAnnotations.contains("add_paragaph_end") ) {
                            println "* " + historicalAnnotations.trim()
                        }
                    }
                }
            }
        }
        
		def stats = new Stats()
        if( options.homonymStats ) {
            stats.collectHomonyms(analyzedSentences)
        }
        if( options.unknownStats ) {
            stats.collectUnknown(analyzedSentences)
        }
        if( options.frequencyStats ) {
            stats.collectFrequency(analyzedSentences)
        }
        if( options.lemmaStats ) {
            stats.collectLemmaFrequency(analyzedSentences)
        }

        return new TagResult(sb.toString(), stats)
    }

    private static class TTR {
        List<?> tokens
    }

    @CompileStatic
    private static boolean hasPosTag(AnalyzedTokenReadings tokenReadings) {
        tokenReadings.getReadings().stream()
            .anyMatch{ t -> ! isTagEmpty(t.getPOSTag()) }
    }   
    
    @CompileStatic
    private List<TTR> tagAsObject(AnalyzedTokenReadings[] tokens) {
        tokens = tokens[1..-1] // remove SENT_START
        List<TTR> tokenReadingsT = []

        tokens.eachWithIndex { AnalyzedTokenReadings tokenReadings, int idx ->
            String theToken = tokenReadings.getToken()
            String cleanToken = tokenReadings.getCleanToken()
            
            boolean hasTag = hasPosTag(tokenReadings) 

            // TODO: ugly workaround for disambiguator problem
            if( ! hasTag || "\u2014".equals(theToken) ) {
                if( tokenReadings.isLinebreak() )
                    return
    
                if( PUNCT_PATTERN.matcher(theToken).matches() ) {
                    tokenReadingsT << new TTR(tokens: [[value: theToken, lemma: cleanToken, tags: 'punct', 'whitespaceBefore': tokenReadings.isWhitespaceBefore()]])
                    return
                }
                else if( SYMBOL_PATTERN.matcher(theToken).matches() ) {
                    tokenReadingsT << new TTR(tokens: [['value': theToken, lemma: cleanToken, tags: 'symb']])
                    return
                }
                else if( XML_TAG_PATTERN.matcher(theToken).matches() ) {
                    tokenReadingsT << new TTR(tokens: [[value: theToken, lemma: cleanToken, tags: 'xmltag']])
                    return
                }
                else if( UNKNOWN_PATTERN.matcher(theToken).matches() && ! NON_UK_PATTERN.matcher(theToken).find() ) {
                    if( isZheleh(options) ) {
                        tokenReadings = adjustTokensWithZheleh(tokenReadings, tokens, idx)
                        hasTag = hasPosTag(tokenReadings)
                    }
                    
                    if( ! hasTag ) {
                        def lemma = options.setLemmaForUnknown ? cleanToken : ''
                        tokenReadingsT << new TTR(tokens: [['value': theToken, lemma: lemma, tags: 'unknown']])
                        return
                    }
                }
                else { // if( UNCLASS_PATTERN.matcher(theToken).matches() ) {
                    tokenReadingsT << new TTR(tokens: [['value': theToken, lemma: cleanToken, tags: 'unclass']])
                    return
                }

            }

            
            TTR item = new TTR(tokens: [])
            
            List<AnalyzedToken> readings = new ArrayList<>(tokenReadings.getReadings())
            readings.removeIf{ it -> it.getPOSTag() != null && it.getPOSTag().endsWith("_END") }
            
            readings = readings.findAll { AnalyzedToken tkn ->
                String posTag = tkn.getPOSTag()
                posTag != null && ! posTag.startsWith("<")
            }

            List<Integer> rates = orderByStats(readings, cleanToken, tokens, idx)

            if( options.singleTokenFormat ) {
                def tagTokens = readings.collect { AnalyzedToken tkn ->
                    getTagTokens(tkn)
                }
                def firstToken = tagTokens[0]
                if( ! options.singleTokenOnly && tagTokens.size() > 1 ) {
                    firstToken['alts'] = tagTokens[1..-1]
                    
                    if( rates ) {
                        Integer sum = (Integer) rates.sum()
//                        if( ! sum ) println ":: sum==0 for $cleanToken"
                        tagTokens.eachWithIndex { t, idx2 ->
                            t['q'] = sum ? ((rates[idx2]*1000).intdiv(sum)).div(1000) : 0
                        }
                    }
                }
                item.tokens = [ firstToken ]
            }
            else {
                item.tokens = readings.collect { AnalyzedToken tkn ->
                    getTagTokens(tkn)
                }
            }
            
            tokenReadingsT << item
        }
            
        tokenReadingsT
    }

//    private rate(List tkns) {
//        if( disambigStatsF.containsKey(tkns[0].value) ) {
//          tkns.each { tkn ->
//                Map<WordReading, Integer> stats = disambigStatsF[tkn.value]
//                tkn['q'] = getRate(r1, stats)
//            }
//        }
//        else {
//          tkns.each { tkn ->
//                tkn['q'] = disambigStatsT[tkn.postag]
//            }
//        }
//    }
    
    @CompileStatic
    private getTagTokens(AnalyzedToken tkn) {
        String posTag = tkn.getPOSTag()
        String semTags = getSemTags(tkn, posTag)

        String lemma = tkn.getLemma() ?: ''
        def token = semTags \
                        ? ['value': tkn.getToken(), 'lemma': lemma, 'tags': posTag, 'semtags': semTags]
                : ['value': tkn.getToken(), 'lemma': lemma, 'tags': posTag]
    }

    private static final boolean dbg = false
    
    @CompileStatic
    private List<Integer> orderByStats(List<AnalyzedToken> readings, String cleanToken, AnalyzedTokenReadings[] tokens, int idx) {
        if( options.disambiguateByStats && readings.size() > 1 ) {

            if( ! disambigStatsF.containsKey(cleanToken) ) {
                // if no stats and there's no prop readings try lowercase
                if( UPPERCASED_PATTERN.matcher(cleanToken).matches() 
                        /* && ! readings.find{ r -> r.getPOSTag().contains(":prop") } */ ) {
                    cleanToken = cleanToken.toLowerCase()
                }
            }
            
            if( disambigStatsF.containsKey(cleanToken) ) {
                stats.inStats ++
                
                Map<WordReading, Integer> statsF = disambigStatsF[cleanToken]
                Map<WordReading, Map<WordContext, Integer>> statsC = disambigStatsC[cleanToken]

                readings.sort { r1, r2 -> 
                    int cmp = getRate(r2, statsF, statsC, tokens, idx, false).compareTo(getRate(r1, statsF, statsC, tokens, idx, false))
                    cmp ?: getPostagRate(r2.getPOSTag()).compareTo(getPostagRate(r1.getPOSTag()))  
                }
                readings.collect { r -> getRate(r, statsF, statsC, tokens, idx, dbg) }
            }
            else { 
                stats.offStats ++
                
                readings.sort { r1, r2 -> getPostagRate(r2.getPOSTag()).compareTo(getPostagRate(r1.getPOSTag())) }
                readings.collect { r -> getPostagRate(r.getPOSTag()) }
            }
        }
    }

    @CompileStatic
    private static WordContext createWordContext(AnalyzedTokenReadings[] tokens, int idx, int pos) {
        ContextToken contextToken = idx + pos < 0 
            ? new ContextToken('', '', 'BEG')
            : idx + pos >= tokens.length 
                ? new ContextToken('', '', 'END')
                : new ContextToken(tokens[idx+pos].getCleanToken(), '', '')  
        new WordContext(contextToken: contextToken, offset: pos)
    }
    
    @CompileStatic
    private int getRate(AnalyzedToken at, Map<WordReading, Integer> statsF, Map<WordReading, Map<WordContext, Integer>> statsC, AnalyzedTokenReadings[] tokens, int idx, boolean dbg) {
        if( dbg )
            println ":: $at, idx: $idx"

        int total = 0
        
        WordReading wordReading = new WordReading(lemma: at.getLemma(), postag: at.getPOSTag())
        Integer vF = statsF[wordReading]
        
        if( vF ) { 

            if( false ) {
            Map<WordContext, Integer> ctx = statsC[wordReading]
            WordContext wordContext = createWordContext(tokens, idx, -1)
            
            if( dbg )
                println "  wr: $wordReading, v: $vF"

            def fitCtx = ctx.findAll { WordContext wc, Integer v2 ->
                wc.offset == wordContext.offset \
                    && wc.contextToken.word == wordContext.contextToken.word
            }
            if( fitCtx ) {
                if( dbg )
                    println "  $fitCtx"
                vF = vF * 2
            }
            }

            total = vF
        }

//        statsF.each { WordReading wr, Integer v ->
//
//            Map<WordContext, Integer> ctx = statsC[wr]
//            WordContext wordContext = createWordContext(tokens, idx, -1)
//
//            if( dbg )
//                println "  wr: $wr, v: $v"
//
//            boolean found = false
//            def fitCtx = ctx.findAll { WordContext wc, Integer v2 -> 
//                wc.offset == wordContext.offset \
//                    && wc.contextToken.word == wordContext.contextToken.word
//            }
//            if( fitCtx ) {
//                if( dbg )
//                    println "  $fitCtx"
//                v = v * 2
//            }
//             
//            if( wr.lemma == at.getLemma() && wr.postag == at.getPOSTag() ) {
//                total += v
//                if (dbg) println " =x= "
//            }
//        }

        if( dbg )        
        println ":: $total"
        return total
    }

    @CompileStatic
    private static String normalizePostagForRate(String postag) {
        postag.replaceAll(/:xp[1-9]/, '')
    }
    
    @CompileStatic
    private int getPostagRate(String postag) {
        postag = normalizePostagForRate(postag)
//        println ":: $postag: ${disambigStatsT[postag]}"
        return disambigStatsT[postag]
    }

    @CompileStatic
    private String getSemTags(AnalyzedToken tkn, String posTag) {
        if( options.semanticTags && tkn.getLemma() != null && posTag != null ) {
            def lemma = tkn.getLemma()
            String posTagKey = posTag.replaceFirst(/:.*/, '')
            String key = "$lemma $posTagKey"

            def potentialSemTags = semanticTags.get(key)
            if( potentialSemTags ) {
                Map<String, List<String>> potentialSemTags2 = semanticTags.get(key)
                List<String> potentialSemTags3 = null
                if( potentialSemTags2 ) {
                    potentialSemTags2 = potentialSemTags2.findAll { k,v -> !k || posTag.contains(k) }
                    List<String> values = (java.util.List<java.lang.String>) potentialSemTags2.values().flatten()
                    potentialSemTags3 = values.findAll { filterSemtag(lemma, posTag, it) }
                    return potentialSemTags3 ? potentialSemTags3.join(';') : null
                }
            }
        }
        return null
    }

    @CompileStatic
    private static boolean filterSemtag(String lemma, String posTag, String semtag) {
        if( posTag.contains("pron") )
            return semtag =~ ":deictic|:quantif"

        if( posTag.startsWith("noun") ) {
            
            if( Character.isUpperCase(lemma.charAt(0)) && posTag.contains(":geo") ) {
                return semtag.contains(":loc")
            }

            if( posTag.contains(":anim") ) {
                if( posTag.contains("name") )
                    return semtag =~ ":hum|:supernat"
                else
                    return semtag =~ ":hum|:supernat|:animal"
            }

            if( posTag.contains(":unanim") )
                return semtag.contains(":animal")

            return ! (semtag =~ /:hum|:supernat|:animal/)
        }
        true
    }


    private StringBuilder outputSentenceXml(taggedObjects) {
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
            if( ! options.singleTokenFormat ) {
                sb.append("  <tokenReading>\n")
            }
            tr.tokens.each { t -> 
                appendToken(t, sb)
            }
            if( ! options.singleTokenFormat ) {
                sb.append("  </tokenReading>\n")
            }
        }
        sb.append("</sentence>");
        return sb
    }

    private void appendToken(t, StringBuilder sb) {
        String indent = options.singleTokenFormat ? "  " : "    "
        sb.append(indent).append("<token value=\"").append(quoteXml(t.value, false)).append("\"")
        if( t.lemma != null ) {
            sb.append(" lemma=\"").append(quoteXml(t.lemma, false)).append("\"")
        }
        if( t.tags ) {
            sb.append(" tags=\"").append(quoteXml(t.tags, false)).append("\"")

            if( ! options.singleTokenFormat ) {
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
    
    private StringBuilder outputSentenceJson(tokenReadingsList) {
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

    @CompileStatic
    private static String adjustZheleh(String text) {
        // найпаскуднїшою
        text = text.replaceAll(/(?ui)([бвгґджзклмнпрстфхцчшщ])ї/, '$1і')
        // і у сімї
        text = text.replaceAll(/(?ui)([бвпмфр])([юяє])/, '$1\'$2')
        text = text.replaceAll(/(?ui)([сцз])ь([бвпмф])([ія])/, '$1$2$3')
        // next are tagged as ":bad" in tagger
//        text = text.replaceAll(/(?ui)ь([сц])(к)/, '$1ь$2')
//        text = text.replaceAll(/(?ui)-(же|ж|би|б)/, ' $1')
    }

    @CompileStatic
	AnalyzedTokenReadings adjustTokensWithZheleh(AnalyzedTokenReadings tokenReadings, AnalyzedTokenReadings[] tokens, int idx) {
		AnalyzedToken origAnalyzedToken = tokenReadings.getReadings().get(0)
		boolean syaIsNext = idx < tokens.size()-1 && tokens[idx+1].getToken() == 'ся'

		if( ( tokenReadings.isPosTagUnknown() || syaIsNext )
			&& origAnalyzedToken.token =~ /[а-яіїєґА-ЯІЇЄҐ]/ ) {

			String adjustedToken = adjustZheleh(origAnalyzedToken.token)

			if( syaIsNext ) {
				tokenReadings = langTool.getLanguage().getTagger().tag([adjustedToken + 'ся']).get(0)
				// println "trying verb:rev $adjustedToken " + tokenReadings.getReadings()
			}

			if( tokenReadings.isPosTagUnknown() ) {
				tokenReadings = langTool.getLanguage().getTagger().tag([adjustedToken]).get(0)
			}

			// put back original word
			for(int i=0; i<tokenReadings.getReadings().size(); i++) {
				AnalyzedToken token = tokenReadings.getReadings().get(i);

				String posTag = token.getPOSTag()

				tokenReadings.getReadings().set(i, new AnalyzedToken(origAnalyzedToken.token, posTag, token.lemma))
			}
		}

		return tokenReadings
	}
	
	

    private static boolean isZheleh(options) {
        return options.modules && 'zheleh' in options.modules
    }


    def process() {
        stats = new Stats()
        
        def outputFile = textUtils.processByParagraph(options, { buffer ->
            return tagText(buffer)
        },
		{ TagResult result ->
			stats.add(result.stats) 
		});
    }

    def postProcess() {

        if( options.homonymStats ) {
            stats.printHomonymStats()
        }
        if( options.unknownStats ) {
            stats.printUnknownStats()
        }
        if( options.frequencyStats ) {
            stats.printFrequencyStats()
        }
        if( options.lemmaStats ) {
            stats.printLemmaFrequencyStats()
        }
        
        if( options.disambiguateByStats ) {
            println "inStats: ${stats.inStats}, offStats: ${stats.offStats}"
        }
    }

    @CompileStatic
    def loadSemTags() {
        if( semanticTags.size() > 0 )
            return

		// def base = System.getProperty("user.home") + "/work/ukr/spelling/dict_uk/data/sem"
		String base = "https://raw.githubusercontent.com/brown-uk/dict_uk/master/data/sem"
		def semDir = new File("sem")
		if( semDir.isDirectory() ) {
			base = "${semDir.path}"
			System.err.println("Loading semantic tags from ./sem")
		}
		else {
			System.err.println("Loading semantic tags from $base")
		}

        long tm1 = System.currentTimeMillis()
        int semtagCount = 0
		["noun", "adj", "adv", "verb", "numr"].each { cat ->
			String text = base.startsWith("http")
				? "$base/${cat}.csv".toURL().getText("UTF-8")
				: new File(semDir, "${cat}.csv").getText("UTF-8")

            if( text.startsWith("\uFEFF") ) {
                text = text.substring(1)
            }

			text.eachLine { line ->
                line = line.trim().replaceFirst(/\s*#.*/, '')
                if( ! line )
                    return

                def parts = line.split(',')
				if( parts.length < 2 ) {
					System.err.println("skipping invalid semantic tag for: \"$line\"")
					return
				}

				def add = ""
				if( parts.length >= 3 && parts[2].trim().startsWith(':') ) {
				   add = parts[2].trim()
				}
                def semtags = parts[1]
				def key = parts[0] + " " + cat
                
                if( ! (key in semanticTags) ) {
                    semanticTags[key] = [:]
                }
                if( ! (add in semanticTags[key]) ) {
                    semanticTags[key][add] = []
                }

                // semtags sometimes have duplicate lines
                if( ! (semtags in semanticTags[key][add]) ) {
                    semanticTags[key][add] << semtags
                    semtagCount += 1
                }
			}
		}

        if( ! options.quiet ) {
            long tm2 = System.currentTimeMillis()
            System.err.println("Loaded $semtagCount semantic tags for ${semanticTags.size()} lemmas in ${tm2-tm1} ms")
        }
	}

    @CompileStatic
    def loadDisambigStats() {
        if( disambigStatsF.size() > 0 )
            return

        long tm1 = System.currentTimeMillis()
        
        def statDir = new File(new File((String)SCRIPT_DIR + "/../../../../../.."), "stats")
        assert statDir.isDirectory(), "Disambiguation stats not found in ${statDir.name}"
        
        String word
        WordReading wordReading

        new File(statDir, "lemma_freqs_hom.txt").eachLine { String line ->
            def p = line.split(/\s*,\s*/)

            if( line.startsWith(' ') ) {
                int pos = p[1] as int
                String ctxWord=p[2], ctxLemma=p[3], ctxPostag=p[4]
                int ctxCnt=p[5] as int

                if( ctxWord.indexOf('^') >= 0 ) ctxWord = ctxWord.replace('^', ',')
                if( ctxLemma.indexOf('^') >= 0 ) ctxLemma = ctxLemma.replace('^', ',')
                
                ContextToken contextToken = new ContextToken(ctxWord, ctxLemma, ctxPostag)
                WordContext wordContext = new WordContext(contextToken: contextToken, offset: pos)
                
                disambigStatsC.computeIfAbsent(word, {s -> new HashMap<>()})
                    .computeIfAbsent(wordReading, {x -> new HashMap<>()})
                    .put(wordContext, ctxCnt as int)
    
                return
            }

            word=p[0]
            String lemma=p[1], postag=p[2], cnt=p[3]

            wordReading = new WordReading(lemma:lemma, postag:postag)

            disambigStatsF.computeIfAbsent(word, {s -> new HashMap<>()})
                .put(wordReading, cnt as int)
            
            postag = normalizePostagForRate(postag)
            disambigStatsT[postag] += cnt as int
        }

        long tm2 = System.currentTimeMillis()
        System.err.println("Loaded ${disambigStatsF.size()} disambiguation stats, ${disambigStatsT.size()} tags in ${tm2-tm1} ms")
    }

    
    @CompileStatic
    static boolean isTagEmpty(String posTag) {
        posTag == null || posTag.endsWith("_END")
    }
    

    	
    static class TagOptions {
        @Option(names = ["-i", "--input"], arity="1", description = "Input file. Default: stdin")
        String input
        @Parameters(index = "0", description = "Input files. Default: stdin", arity="0..1000")
        List<String> inputFiles
        @Option(names = ["-o", "--output"], arity="1", description = "Output file (default: <input file base name> + .tagged.txt/.xml/.json) or stdout if input is stdin")
        String output
        @Option(names = ["-l", "--tokenPerLine"], description = "One token per line (for .txt output only)")
        boolean tokenPerLine
        @Option(names = ["-x", "--xmlOutput"], description = "Output in xml format")
        boolean xmlOutput
        @Option(names = ["-n", "--outputFormat"], arity="1", description = "Output format: {xml (default), json, txt}", defaultValue = "xml")
        OutputFormat outputFormat
        @Option(names = ["-s", "--homonymStats"], description = "Collect homohym statistics")
        boolean homonymStats
        @Option(names = ["-u", "--unknownStats"], description = "Collect unknown words statistics")
        boolean unknownStats
        @Option(names = ["-b", "--filterUnknown"], description = "Filter out unknown words with non-Ukrainian character combinations")
        boolean filterUnknown
        @Option(names = ["-w", "--frequencyStats"], description = "Collect word frequency")
        boolean frequencyStats
        @Option(names = ["-z", "--lemmaStats"], description = "Collect lemma frequency")
        boolean lemmaStats
        @Option(names = ["-e", "--semanticTags"], description = "Add semantic tags")
        boolean semanticTags
        @Option(names = ["-k", "--noTag"], description = "Do not write tagged text (only perform stats)")
        boolean noTag
        @Option(names = ["-m", "--modules"], arity="1", description = "Comma-separated list of modules, supported modules: [zheleh]")
        List<String> modules
        @Option(names = ["--singleThread"], description = "Always use single thread (default is to use multithreading if > 2 cpus are found)")
        boolean singleThread
        @Option(names = ["-q", "--quiet"], description = "Less output")
        boolean quiet
        @Option(names= ["-h", "--help"], usageHelp= true, description= "Show this help message and exit.")
        boolean helpRequested
//        @Option(names = ["--disableDisambigRules"], arity="1", required = false, description = "Comma-separated list of ids of disambigation rules to disable")
//        boolean disabledRules
        @Option(names = ["-d", "--showDisambig"], description = "Show disambiguation rules applied")
        boolean showDisambig
        @Option(names = ["--setLemmaForUnknown"], description = "Fill lemma for unknown words (default: empty lemma)")
        boolean setLemmaForUnknown
        @Option(names = ["-g", "--disambiguate-by-stats"], description = "Use statistics for disambiguation")
        boolean disambiguateByStats
        @Option(names = ["-t", "--tokenFormat"], description = "Use <token> format (instead of <tokenReading>)")
        boolean singleTokenFormat
        @Option(names = ["-t1", "--singleTokenOnly"], description = "Use single token format")
        boolean singleTokenOnly

        void adjust() {
            if( ! outputFormat ) {
                if( xmlOutput ) {
                    outputFormat = OutputFormat.xml
                }
                else {
                    outputFormat = OutputFormat.xml
                }
            }
            if( singleTokenOnly ) {
                singleTokenFormat = true
            }
            if( ! quiet ) {
                println "Output format: " + outputFormat
            }
        }
    }
    

    @CompileStatic
    static TagOptions parseOptions(String[] argv) {
        TagOptions options = new TagOptions()
        CommandLine commandLine = new CommandLine(options)
        try {
            commandLine.parseArgs(argv)
            if (options.helpRequested) {
                commandLine.usage(System.out)
                System.exit 0
            } 
        } catch (ParameterException ex) {
            println ex.message
            commandLine.usage(System.out)
            System.exit 1
        }

        return options
    }

    
    void setOptions(TagOptions options) {
        options.adjust()

        setInputOutput(options)
                
        this.options = options
        
        if( ! options.quiet ) {
            if( isZheleh(options) ) {
                System.err.println ("Using adjustments for Zhelekhivka")
                if( options.frequencyStats || options.unknownStats || options.lemmaStats ) {
                    System.err.println ("NOTE: Zhelekhivka adjustments currently do not apply to statistics!")
                }
            }
        }

        if( options.semanticTags ) {
            if( options.outputFormat == OutputFormat.txt ) {
                System.err.println ("Semantic tagging only available in xml/json output")
                System.exit 1
            }

            loadSemTags()
        }

        if( options.disambiguateByStats ) {
            if( options.outputFormat == OutputFormat.txt ) {
                System.err.println ("Semantic tagging only available in xml/json output")
                System.exit 1
            }

            loadDisambigStats()
        }
    }

    void setInputOutput(TagOptions options) {
        
        if( ! options.input ) {
            options.input = "-"
        }
        if( ! options.output ) {
            def fileExt = "." + options.outputFormat // ? ".xml" : ".txt"
            def outfile = options.input == '-' ? '-' : options.input.replaceFirst(/\.txt$/, ".tagged${fileExt}")
            options.output = outfile
        }

    }
    
    
    static final Pattern CYR_LETTER = Pattern.compile(/[а-яіїєґА-ЯІЇЄҐ]/)    
    static final Pattern NON_UK_LETTER = Pattern.compile(/[ыэъёЫЭЪЁ]|ие|ИЕ|ннн|оі$|[а-яіїєґА-ЯІЇЄҐ]'?[a-zA-Z]|[a-zA-Z][а-яіїєґА-ЯІЇЄҐ]/)    
    
    @CompileStatic
	class Stats {
		Map<String, Integer> homonymFreqMap = [:].withDefault { 0 }
		Map<String, Set<String>> homonymTokenMap = [:].withDefault{ new LinkedHashSet<>() }
		Map<String, Integer> unknownMap = [:].withDefault { 0 }
		Map<String, Integer> frequencyMap = [:].withDefault { 0 }
		Map<String, Integer> lemmaFrequencyMap = [:].withDefault { 0 }
        Map<String, Set<String>> lemmaFrequencyPostagsMap = [:].withDefault { [] as Set }
        Set lemmaAmbigs = new HashSet<>()
		Map<String, Integer> knownMap = [:].withDefault { 0 }
		int knownCnt = 0

        int inStats = 0
        int offStats = 0
        
		synchronized void add(Stats stats) {
			stats.homonymFreqMap.each { k,v -> homonymFreqMap[k] += v }
			stats.homonymTokenMap.each { k,v -> homonymTokenMap[k].addAll(v) }
			stats.unknownMap.each { k,v -> unknownMap[k] += v }
			stats.knownMap.each { k,v -> knownMap[k] += v }
			stats.frequencyMap.each { k,v -> frequencyMap[k] += v }
			stats.lemmaFrequencyMap.each { k,v -> lemmaFrequencyMap[k] += v }
            stats.lemmaFrequencyPostagsMap.each { k,v -> lemmaFrequencyPostagsMap[k] += v }
            lemmaAmbigs.addAll(stats.lemmaAmbigs)
		    knownCnt += stats.knownCnt
            
            inStats += stats.inStats
            offStats += stats.offStats
		}

        @CompileStatic
		def collectUnknown(List<AnalyzedSentence> analyzedSentences) {
			for (AnalyzedSentence analyzedSentence : analyzedSentences) {
				// if any words contain Russian sequence filter out the whole sentence - this removes tons of Russian words from our unknown list
				// we could also test each word against Russian dictionary but that would filter out some valid Ukrainian words too
				
                def tokensNoSpace = analyzedSentence.getTokensWithoutWhitespace()[1..-1]
                if( options.filterUnknown ) {
					def unknownBad = tokensNoSpace.any { AnalyzedTokenReadings tokenReadings ->
						tokenReadings.getCleanToken().indexOf('еи') > 0
					}
					if( unknownBad )
						continue
				}

				tokensNoSpace.each { AnalyzedTokenReadings tokenReadings ->
                        String posTag = tokenReadings.getAnalyzedToken(0).getPOSTag()
        				if( isTagEmpty(posTag) ) {
                            String token = tokenReadings.getCleanToken()
                            if( CYR_LETTER.matcher(token).find()
                                && ! NON_UK_LETTER.matcher(token).find() ) {
        					unknownMap[token] += 1
        				}
					}
				}

				tokensNoSpace.each { AnalyzedTokenReadings tokenReadings ->
				    if( ! (tokenReadings.getToken() =~ /[0-9]|^[a-zA-Z-]+$/) 
                            && tokenReadings.getReadings().any { AnalyzedToken at -> ! isTagEmpty(at.getPOSTag())} ) {
				      knownCnt++
				      knownMap[tokenReadings.getToken()] += 1
				    }
				}
			}
		}


        @CompileStatic
		def collectHomonyms(List<AnalyzedSentence> analyzedSentences) {

			for (AnalyzedSentence analyzedSentence : analyzedSentences) {
				for(AnalyzedTokenReadings readings: analyzedSentence.getTokens()) {
                    if( readings.getReadingsLength() < 2 )
                        continue

                    List<AnalyzedToken> tokenReadings = new ArrayList<>(readings.getReadings())
                    tokenReadings.removeIf{ AnalyzedToken it -> 
                        it.getPOSTag() == null \
                            || it.getPOSTag() in [JLanguageTool.SENTENCE_END_TAGNAME, JLanguageTool.PARAGRAPH_END_TAGNAME] \
                            || it.getPOSTag().startsWith('<') 
                    }

					if( tokenReadings.size() < 2 )
						continue

					def key = tokenReadings.join("|")
					homonymFreqMap[key] += 1
					homonymTokenMap[key] << readings.getCleanToken()
				}
			}
		}


        @CompileStatic
		def collectFrequency(List<AnalyzedSentence> analyzedSentences) {
			for (AnalyzedSentence analyzedSentence : analyzedSentences) {
				analyzedSentence.getTokensWithoutWhitespace()[1..-1].each { AnalyzedTokenReadings tokenReadings ->
					if( isTagEmpty(tokenReadings.getAnalyzedToken(0).getPOSTag())
                            && tokenReadings.getToken() =~ /[а-яіїєґА-ЯІЇЄҐ]/ ) {
//                            && ! (tokenReadings.getToken() =~ /[ыэъё]|[а-яіїєґА-ЯІЇЄҐ]'?[a-zA-Z]|[a-zA-Z][а-яіїєґА-ЯІЇЄҐ]/) ) {
						frequencyMap[tokenReadings.getCleanToken()] += 1
					}
				}
			}
		}

        @CompileStatic
		def collectLemmaFrequency(List<AnalyzedSentence> analyzedSentences) { 
			for (AnalyzedSentence analyzedSentence : analyzedSentences) {
				analyzedSentence.getTokensWithoutWhitespace()[1..-1].each { AnalyzedTokenReadings tokenReadings ->
					 Map<String, List<AnalyzedToken>> lemmas = tokenReadings.getReadings()
						.findAll { it.getLemma() \
                            && tokenReadings.getCleanToken() ==~ /[а-яіїєґА-ЯІЇЄҐ'-]+/ \
                            && ! (it.getPOSTag() =~ /arch|alt|short|long|bad|</) 
						}
                        .groupBy{ it.getLemma() }
                    
                    lemmas.each { String k, List<AnalyzedToken> v ->
						lemmaFrequencyMap[ k ] += 1
                        if( lemmas.size() > 1 ) {
                            lemmaAmbigs << k
                        }
                        def tags = v.findAll{ it.getPOSTag() && ! it.getPOSTag().startsWith("SENT") }.collect { it.getPOSTag().replaceFirst(/:.*/, '') }
                        lemmaFrequencyPostagsMap[k].addAll( tags ) 
					}
				}
			}
		}
	

	    def printHomonymStats() {
	
	        def printStream
	        if( options.output == "-" ) {
	            printStream = System.out
	            printStream.println "\n\n"
	        }
	        else {
	            def outputFile = new File(options.output.replaceFirst(/\.(txt|xml|json)$/, '') + '.homonym.txt')
	            printStream = new PrintStream(outputFile, "UTF-8")
	        }
	
	        printStream.println("Час-та\tОм.\tЛем\tСлово\tОмоніми")
	
	        homonymFreqMap
    	        .sort { -it.value }
    	        .each{ k, v ->
    	            def items = k.split(/\|/)
    	            def homonimCount = items.size()
    	            def posHomonimCount = items.collect { it.split(":", 2)[0] }.unique().size()
    	
    	            def lemmasHaveCaps = items.any { Character.isUpperCase(it.charAt(0)) }
    	            if( ! lemmasHaveCaps ) {
    	                homonymTokenMap[k] = homonymTokenMap[k].collect { it.toLowerCase() } as Set
    	            }
    	
    	            def str = String.format("%6d\t%d\t%d\t%s\t\t%s", v, homonimCount, posHomonimCount, homonymTokenMap[k].join(","), k)
    	            printStream.println(str)
    	        }
	    }
	
	    def printUnknownStats() {
	
	        def printStream
	        if( options.output == "-" ) {
	            printStream = System.out
	            printStream.println "\n\n"
	        }
	        else {
	            def outputFile = new File(options.output.replaceFirst(/\.(txt|xml|json)$/, '') + '.unknown.txt')
	            printStream = new PrintStream(outputFile, "UTF-8")
	        }
	
	        unknownMap
		        .sort { it.key }
		        .each{ k, v ->
		            def str = String.format("%6d\t%s", v, k)
		            printStream.println(str)
		        }

			int unknownCnt = unknownMap ? unknownMap.values().sum() as int : 0
			double unknownPct = knownCnt+unknownCnt ? (double)unknownCnt*100/(knownCnt+unknownCnt) : 0
	        println "Known: $knownCnt, unknown: $unknownCnt, " + String.format("%.1f", unknownPct) + "%"

            double unknownUniquePct = knownMap.size()+unknownMap.size() ? (double)unknownMap.size()*100/(knownMap.size()+unknownMap.size()) : 0
	        println "Known unique: ${knownMap.size()}, unknown unique: " + unknownMap.size() + ", " + String.format("%.1f", unknownUniquePct) + "%"

            Map unknownWordsMap = unknownMap.findAll { k,v -> k =~ /(?iu)^[а-яіїєґ][а-яіїєґ'-]*/ }
            double unknownUniqueWordPct = knownMap.size()+unknownWordsMap.size() ? (double)unknownWordsMap.size()*100/(knownMap.size()+unknownWordsMap.size()) : 0
            println "\tunknown unique (letters only): " + unknownWordsMap.size() + ", " + String.format("%.1f", unknownUniqueWordPct) + "%"
	    }
	
	    def printFrequencyStats() {
	
	        def printStream
	        if( options.output == "-" ) {
	            printStream = System.out
	            printStream.println "\n\n"
	        }
	        else {
	            def outputFile = new File(options.output.replaceFirst(/\.(txt|xml|json)$/, '') + '.freq.txt')
	            printStream = new PrintStream(outputFile, "UTF-8")
	        }
	
	        frequencyMap
	        .sort { it.key }
	        .each{ k, v ->
	            def str = String.format("%6d\t%s", v, k)
	            printStream.println(str)
	        }
	    }
	
	    def printLemmaFrequencyStats() {
	
	        def printStream
	        if( options.output == "-" ) {
	            printStream = System.out
	            printStream.println "\n\n"
	        }
	        else {
	            def outputFile = new File(options.output.replaceFirst(/\.(txt|xml|json)$/, '') + '.lemma.freq.txt')
	            printStream = new PrintStream(outputFile, "UTF-8")
	        }
	
            println ":: " + lemmaFrequencyPostagsMap.size()            
	        lemmaFrequencyMap
	            .sort { it.key }
                .each{ k, v ->
                    String amb = k in lemmaAmbigs ? "\tA" : ""
                    def str = String.format("%6d\t%s%s\t%s", v, k, amb, lemmaFrequencyPostagsMap[k].join(","))
                    printStream.println(str)
                }
	    }
	}

	
	
    static void main(String[] args) {

        def nlpUk = new TagText()
        
        def options = parseOptions(args)

        nlpUk.setOptions(options)

        // TODO: quick hack to support multiple files
        if( options.inputFiles && options.inputFiles != ["-"] ) {
            options.inputFiles.forEach{ filename ->
                options.output = ""
                options.input = filename
                nlpUk.setInputOutput(options)
                nlpUk.process()
                nlpUk.postProcess()
            }
        }
        else {
            nlpUk.process()
            nlpUk.postProcess()
        }
    }

}
