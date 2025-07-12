package ua.net.nlp.other.clean

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import java.util.regex.Pattern

// NOTE: due to https://issues.apache.org/jira/browse/GROOVY-10918 we have to variate local variables
// it looks ugly but helps with OOM on big files, see also // ml comments to forcefully clear variables once we're done with them

@CompileStatic
@PackageScope
class CleanTextCore2 {
    final OutputTrait out
    LtModule ltModule
    String outDirName
    
    final CleanOptions options
    
    GracModule gracModule = new GracModule(out: out, ltModule: ltModule)
    SpacingModule spacingModule = new SpacingModule(out: out, ltModule: ltModule)
    LatCyrModule latCyrModule = new LatCyrModule(out: out, ltModule: ltModule)
    HyphenModule hyphenModule = new HyphenModule(out: out, ltModule: ltModule)
    ControlCharModule controlCharModule = new ControlCharModule(out: out, ltModule: ltModule)
    ApostropheModule apostropheModule = new ApostropheModule(out: out, ltModule: ltModule)
    MarkLanguageModule markLanguageModule = new MarkLanguageModule(out: out, ltModule: ltModule, options: options)
    
    
    CleanTextCore2(OutputTrait out, CleanOptions options, LtModule ltModule) {
        this.out = out
        this.options = options
        this.ltModule = ltModule
    }

    String cleanTextInternal(CleanRequest request) {
        def t00 = request.text.toString()
        
        if( t00.contains("\r") ) {
            t00 = t00.replace("\r", "")
        }

        if( ! checkEmptyLines(t00) )
            return null
        
        t00 = controlCharModule.removeControlChars(t00)
        
        t00 = gracModule.fix(t00)
//        t00 = gracModule.firtka(t00)
        
        t00 = fixTypos(t00)
        
        def t0 = fixQuotes(t00)
//t00 = null // ml
            
        // weird ї and й via combining characters
        if( t0.contains("\u0308") ) {
            t0 = t0.replaceAll(/[іi]\u0308/, 'ї')
            t0 = t0.replaceAll(/[ІI]\u0308/, 'Ї')
        }
        if( t0.contains("\u0306") ) {
            t0 = t0.replace(/и\u0306/, 'й')
            t0 = t0.replace(/И\u0306/, 'Й')
        }

        def t01 = fixOi(t0)
// t0 = null // ml

        t01 = apostropheModule.fixWeirdApostrophes(t01)

        t01 = apostropheModule.fixSpacedApostrophes(t01)
        
        def t10 = hyphenModule.removeSoftHyphens(t01)
//t01 = null // ml
        
        if( t10.contains('\u2028') ) {
            t10 = t10.replaceAll(/\u2028\n?/, '\n')
        }

        // digit 3 instead of letter З
        t10 = t10.replaceAll(/(?U)(^|[!?.][\h\v]+)3[аa]([\h\v]*[а-яіїєґА-ЯІЇЄҐ])/, '$1За$2')

        if( ! options.simple ) {

            def t12 = latCyrModule.fixCyrLatMix(t10)
    //t10 = null // ml
            if( ! t12 )
                return null
    
            if( ! checkTwoColumns(t12) )
                return null
    
    //        if( options.modules ) {
    //            t12 = runModules(t12, request, options)
    //        }
            t10 = t12
        }
            
        t10 = hyphenModule.separateLeadingHyphens(t10)

        if( ! options.simple ) {
            t10 = hyphenModule.fixDanglingHyphens(t10)
    
            t10 = fixSplitWords(t10)
    
            t10 = spacingModule.cleanupSpacing(t10)
    
            if( options.markLanguages != CleanOptions.MarkOption.none ) {
                def req2 = new CleanRequest(text: t10, file: request.file, outFile: request.outFile)
                t10 = markLanguageModule.markRussian(request.forText(t10), outDirName)
            }
        }

        if( request.dosNl ) {
            t10 = t10.replaceAll(/(?!<\r)\n/, "\r\n")
        }

        def pageNum = ~ /^\h*[-_\u2013\u2014~]\h*[0-9]{1,4}\h*[-_\u2013\u2014~]\h*$/
        def pageNumLines = t10.lines().findAll{String it -> pageNum.matcher(it).matches() }
        if( pageNumLines.size() >= 3 ) {
            out.println "\t\tNOTE: suspect page numbers (${pageNumLines.size()}) in the text: ${pageNumLines[0]}"
        }
                
        def lineLimit = 4096
        def longLineCnt = t10.lines().filter{ s -> s.length() > lineLimit }.count()
        if( longLineCnt ) {
            def longest = t10.lines().map{ s -> s.length()}.filter{ l -> l > lineLimit }.max{ a,b -> a.compareTo(b) }.get()
            out.println "\tNOTE: found lines longer than $lineLimit: $longLineCnt lines, longest: $longest"
        }

        def longWord = Pattern.compile("[а-яіїєґА-ЯІЇЄҐ'\u2019\u02BC]{36}")
        def longWordLines = t10.lines().filter{ s -> longWord.matcher(s).find() }.toList()
        if( longWordLines.size() ) {
            def m = longWord.matcher(t10)
            m.find()
            def sample = m.group(0)
            out.println "\tWARNING: found words longer than 36: ${longWordLines.size()} lines: $sample"
        }

        def wordBroken = Pattern.compile(/[\h-]([вжтч]ання|[вжчт]ення|лись|н?ість|нів|н?ня|ної|ност[іи]|вської|обов(?!['\u2019\u02BC])|ств[оа]|ськ(а|ий|им|ого|ому|ої)|ться|[ює]ть|[єю]ться|ючи)\b/, Pattern.UNICODE_CHARACTER_CLASS)
        def wordBrokenCnt = t10.lines().filter{ s -> wordBroken.matcher(s).find() }.count()
        if( wordBrokenCnt > 5 ) {
            def m = wordBroken.matcher(t10)
            m.find()
            out.println "\tWARNING: found words potentially broken: $wordBrokenCnt lines: ${getContext(m, t10)}"
        }

        t10
    }
    
    @CompileStatic
    String fixTypos(String text) {
        text = text.replaceAll(/(?U)тсья\b/, 'ться')

        text = text.replaceAll(/[а-яїієґА-ЯІЇЄҐ][а-яїієґ’ʼ'-]+(ннн|ттт)[а-яїієґ][а-яїієґ'’ʼ-]*/, { all, w1 ->
            String fix = all.replaceAll(/(?iu)н(нн)/, '$1').replaceAll(/(?iu)т(тт)/, '$1')
            ltModule.knownWord(fix) ? fix : all
        })

        
        text = text.replace(/дербюджет/, 'держбюджет')
        text = text.replace(/фінасуванн/, /фінансуванн/)
        text = text.replace(/адмінстрац/, /адміністрац/)
        text = text.replace("дистопад", "листопад")
        
        text = text.replaceAll(/(?U)\b(мдрд|мрлд|мрд)\b/, 'млрд')
        
        // too many FP: Тижденьі розмовляв, Владімір Ільіч
//        text = text.replaceAll(/ьі/, 'ы')
    
        text = text.replace("заборгованност", "заборгованост")
        text = text.replaceAll(/(?U)\bперс-(служб|центр|секрет|конф)/, 'прес-$1')
        text = text.replaceAll(/(повдіомле|повідмле|повідмоле)нн/, 'повідомленн')
        text = text.replace(/авіакастроф/, 'авіакатастроф')
        text = text.replace(/зазанач/, 'зазнач')
        text = text.replace(/йдетьсяу/, 'йдеться у')

    //TODO: будьласка
    }

    
    @CompileStatic
    String fixQuotes(String text) {
        if( text.contains("''") ) {
            text = text.replaceAll(/(?<!')''(?!')/, '"')
        }
        // sometimes quotes are escaped
        text = text.replace('\\"', '"')
        text = text.replaceAll(/([бвгґдзкмнпрстфхш])\\'([яєюї])/, '$1\'$2')

        text = text.replaceAll(/([а-яіїєґА-ЯІЇЄҐ])\u200B([а-яіїєґ])/, '$1$2')
        text = text.replaceAll(/([А-ЯІЄЇҐ])\u200B([А-ЯІЇЄҐ])/, '$1$2')
        text = text.replaceAll(/\u200B+/, ' ')
        text = text.replace(/\u02BA/, '"')

        // SINGLE LOW-9 QUOTATION MARK sometimes used as a comma
        text.replace('\u201A', ',')
    }
    
    
    @CompileStatic
    String fixOi(String text) {

        if( ! options.disabledRules.contains("oi") ) {
            // промисловоі
            def t0 = text.replaceAll(/(?U)([а-яїієґА-ЯІЇЄҐ][а-яїієґ'-]+[а-яїієґ])(о[іi])\b/, { all, w1, w2 ->
                String fix = "${w1}ої"
                ltModule.knownWord(fix) ? fix : all
            })
             
            // Нацполіціі
            def t1 = t0.replaceAll(/(?U)([а-яїієґА-ЯІЇЄҐ][а-яїієґ'-]+[а-яїієґ][стц])([іi][іi])\b/, { all, w1, w2 ->
                String fix = "${w1}ії"
                ltModule.knownWord(fix) ? fix : all
            })
//t0 = null // ml
            text = t1
        }
        text
    }
    

    @CompileStatic
    String fixSplitWords(String text) {
        int cnt = 0
        String regex = /([а-яіїєґА-ЯІЇЄҐ'ʼ’-]+)\n([ \t]*)([а-яіїєґ][а-яіїєґ'ʼ’-]*)([,;.!?])?/
        def t1 = text.replaceAll(regex, { List<String> it ->
            if( it[4] != "."    // we don't want to join ММК ім. Ілліча
                && it[1].length() + it[3].length() >= 4
                && ! (it[0] =~ /[А-ЯІЇЄҐ]{2,}/)
                && ! ltModule.knownWordTwoLang(it[1])
                && ! ltModule.knownWordTwoLang(it[3])
                && ltModule.knownWord(it[1] + it[3]) ) {
                cnt += 1
                // print "."
                it[1] + it[3] + (it[4] ?: "") + "\n" + it[2]
            }
            else {
                it[0]
            }
        })
        if( cnt ) {
            out.println "\t$cnt word splits removed"
        }
        t1
    }


    boolean checkTwoColumns(String text) {
        if( ! options.allowTwoColumns ) {

//            def t0 = CharBuffer.wrap(text, 0, 100*1024)
            def t0 = text.take(100*1024)
            
            if( t0.count("    ") >= 5 ) {
                def matcher = t0 =~ /(?ium)(.*?[а-яїієґ] {4,})[а-яіїєґ].{4}/
                def matchSize = matcher.size()
                if( matchSize >= 5 && matchSize > t0.count("\n") * 3 / 4 ) {
                    matcher.reset()
                    matcher.find()
                    def lines = []
                    int secondColStart = matcher.group(1).length()
                    for(int ii=0; ii<4; ii++) {
                        matcher.find()
                        if( secondColStart != matcher.group(1).length() )
                            return true
                        lines << matcher.group(0)
                    }
                    out.println "\tERROR: two columns detected, skipping...:"
                    out.println "\t${lines.join('\n\t')}"
                    return false
                }
            }
        }
        return true
    }

    @CompileStatic
    boolean checkEmptyLines(String text) {
        if( text.count("\n\n") > 5 ) {
            //def t0 = CharBuffer.wrap(text, 0, 100*1024)
            def t0 = text.take(100*1024)
            
//            def nonEmptyLines = text.getLines().findAll { it =~ /[^\s]/ }
//            if( nonEmptyLines.count { it.length() > 120 } > 5 ) {
//                _println "\tVery long lines found, probably unwrapped paragraphs..."
//                return true
//            }

            // headers often have titles without period
            
            def lines = t0.readLines()
            if( lines.size() > 2 ) {
                lines = lines[2..<lines.size()]
            }
            
            def t1 = lines.join("\n")
//            def matcher = text =~ /(?ius)[а-яїієґ0-9,—–-]\s*\n\n[а-яіїєґ0-9]/
            def matcher = t1 =~ /(?us)[а-яїієґА-ЯІЇЄҐ,:—–-]\s*\n\n[а-яіїєґ]/
            if( matcher ) {
                def context = getContext(matcher, t1)
                out.println "\tWARNING: Suspect empty lines inside the sentence: $context"
                return true
            }

//            def nonEmptyLineCnt = nonEmptyLines.size()
//            if( matcher.size() > nonEmptyLineCnt / 7 ) {
//                _println "\tWARNING: Suspect empty lines between sentences: ${matcher.size()}, total non-empty: $nonEmptyLineCnt"
//                return true
//            }
        }
        return true
    }
    
    
    static String getContext(java.util.regex.Matcher matcher, String text) {
        int start = Math.max(0, matcher.start() - 10)
        int end = Math.min(text.length(), matcher.end() + 10)
        String CONTEXT = text.substring(start, end).replace('\n', '\\n')
        return CONTEXT
    }


}
