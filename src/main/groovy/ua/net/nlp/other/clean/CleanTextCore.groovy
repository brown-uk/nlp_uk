#!/bin/env groovy

package ua.net.nlp.other.clean

// This script reads all .txt files in given directory (default is "txt/") 
// and tries to clean up the text ot make it more suitable for NLP
// The output files go into <file/dir>-good
// Cleanups:
// fix broken encoding (broken cp1251 etc)
// remove soft hyphen 
// replace weird apostrophe characters with correct one (')
// merge some simple word wraps
// remove backslash from escaped quotes
// weird ї and й via combining characters (U+0308)
// і instead of ї: промисловоі, нацполіціі
// clean up latin/cyrillic character mix
//   CO/CO2 with cyr/lat mix
//   degree Celcius with cyr
// digit 3 instead of letter З
// try to detect and skip two-column texts
// separate leading hyphen (e.g. -Алло! - проричав він в слухавку)
// fix dangling hyphen (at the end of the line)
// check and warn for spaced words (e.g. Н А Т А Л К А)
// mark/rate or remove Russian paragraphs
//
// NOTE: due to https://issues.apache.org/jira/browse/GROOVY-10918 we have to variate local variables
// it looks ugly but helps with OOM on big files, see also // ml comments to forcefully clear variables once we're done with them

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

import groovy.io.FileVisitResult
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import picocli.CommandLine
import picocli.CommandLine.ParameterException
import ua.net.nlp.other.clean.CleanOptions
import ua.net.nlp.other.clean.CleanOptions.MarkOption


@CompileStatic
class CleanTextCore {
    private static final String UTF8 = StandardCharsets.UTF_8.name()
    static final int CHUNK_LIMIT = 10*1024*1024
    
    // for higher quality text esp. short newspaper articles you need to keep it low ~100
    // for larger text with possible scanned sources you may want to go higher > 200
    // note: we count words with 2 letters and more
    int minUkrWordCount = 0

    CleanOptions options
    OutputTrait out = new OutputTrait()

    String outDirName
    
    LtModule ltModule = new LtModule()
    LatCyrModule latCyrModule = new LatCyrModule(out: out, ltModule: ltModule)
    MarkLanguageModule markLanguageModule = new MarkLanguageModule(out: out, ltModule: ltModule)
    EncodingModule encodingModule = new EncodingModule(out: out)
    HyphenModule hyphenModule = new HyphenModule(out: out, ltModule: ltModule)
    
    
    CleanTextCore(CleanOptions options) {
        this.options = options
        markLanguageModule.options = options
        out.options = options
        out.init()

        if( options.wordCount ) {
            minUkrWordCount = options.wordCount as int
        }
        if( minUkrWordCount > 0 ) {
            println "Minimum word limit: $minUkrWordCount"
        }
    }


    @CompileStatic
    static CleanOptions parseOptions(String[] argv) {
        CleanOptions options = new CleanOptions()
        CommandLine commandLine = new CommandLine(options)
        try {
            commandLine.parseArgs(argv)
            if (options.helpRequested) {
                commandLine.usage(System.out)
                System.exit 0
            }
        } catch (ParameterException ex) {
            System.err.println ex.message
            commandLine.usage(System.out)
            System.exit 1
        }

        return options
    }

    
    static int main(String[] args) {
        
        setup()

        def options = parseOptions(args)

        return new CleanTextCore(options).process()
    }
    
    private static void setup() {
        // windows have non-unicode encoding set by default
        if( ! "UTF-8".equals(System.getProperty("file.encoding"))
                || "UTF-8".equals(java.nio.charset.Charset.defaultCharset()) ) {
            System.setOut(new PrintStream(System.out,true,"UTF-8"))
        }
    }


    int process() {

        if( ! options.input ) {

            String dir = options.dir 
				? options.dir 
				: options.inputDirs
					? options.inputDirs[0]
					: "."

            File baseDir = new File(dir)
            List<File> files = []

            int maxDepth_ = options.recursive ? -1 : 0;

            baseDir.traverse(type: groovy.io.FileType.FILES, 
                maxDepth: maxDepth_,
                preDir  : { File it ->
                    if (it.name == 'good' || it.name.endsWith('-good') ) return FileVisitResult.SKIP_SUBTREE },
                ) { File it ->
//                if( it.isDirectory() && it.name == "good" )
//                    return FileVisitResult.SKIP_SUBTREE

                if( it.name.toLowerCase().endsWith(".txt") || it.name.endsWith(".csv") ) {
                    files << it
                }
            }
            
            println "Found ${files.size()} .txt files in $dir"
            
			if( files ) {
				outDirName = prepareDir(baseDir)
            
				processFiles(files, baseDir, null, outDirName)
			}
        }
        else {
            def inputFilename = options.input
            def outputFilename = options.output ?: inputFilename.replaceFirst(/\..*?$/, '.good$0')

            def outFile = new File(outputFilename)
            if( inputFilename != outputFilename && outFile.exists() ) {
                println "Removing $outFile"
                outFile.delete()
            }

            def files = [ new File(inputFilename) ]

            processFiles(files, null, outputFilename, null)
        }

        println "Done!"

		return 0
    }

    private prepareDir(File baseDir) {
//        def outDirName = baseDir.path + "/good" + (dir.canonicalPath - baseDir.canonicalPath)

        def outDirName = baseDir.path == "." ? "good" : baseDir.path + "-good"
        def outDirFile = new File(outDirName)
		println "Output directory: ${outDirFile.name}"
        outDirFile.mkdirs()

        if( ! outDirFile.isDirectory() ) {
            System.err.println "Output dir $outDirName does not exists"
            System.exit 1
        }

        def prevFiles = outDirFile.listFiles()

        if( prevFiles.size() > 0 ) {
            if( options.clean ) {
                println "Removing files from $outDirName"
                prevFiles.each { File f ->
                    boolean res = f.isDirectory() ? f.deleteDir() : f.delete()
                    if( ! res ) {
                        System.err.println "Failed to delete ${f.name}"
                        System.exit 1
                    }
                }
            }
            else {
                System.err.println "Output dir $outDirName is not empty (rerun with --clean if you want to remove those files)"
                System.exit 1
            }
        }

        return outDirName
    }

    @CompileStatic
    void processFiles(List<File> files, File baseDir, String outFilename, String outDirName) {
        
        def stream = options.parallel ? files.parallelStream() : files.stream()

        if( options.parallel ) {
            println "Processing ${files.size()} files in parallel"
        }
        else {
//            out.set(System.out)
            if( ! options.input ) {
                println "Processing ${files.size()} files"
            }
        }

		println ""

        stream.forEach{ File file ->
            out.init()

            if( ! options.input ) {
				Path pathAbsolute = Paths.get(file.absolutePath)
				Path pathBase = Paths.get(baseDir.absolutePath)
				Path pathRelative = pathBase.relativize(pathAbsolute);
                out.println "Looking at $pathRelative"
            }

            
            File outFile
            if( outFilename == null ) {
//                def outDirName = outDirName == "." ? "good" : outDirName + "-good/"
                new File(outDirName).mkdirs()

                Path pathAbsolute = Paths.get(file.absolutePath)
                Path pathBase = Paths.get(baseDir.absolutePath)
                Path pathRelative = pathBase.relativize(pathAbsolute);
                
                File parentDir = new File(outDirName, pathRelative.toString()).getParentFile()
                parentDir.mkdirs()
                outFile = new File(parentDir, file.getName())
            }
            else {
                outFile = new File(outFilename)
            }

            doCleanFile(file, outFile)
            
            out.flush()
        }

		println "\nDone"
    }
    
    
    @CompileStatic
    doCleanFile(File file, File outFile) {
        String text = cleanUp(file, options, outFile)

        if( ! text ) {
            if( ! options.keepInvalidFiles )
                return
        }

        // NOTE: only counting words with 2 or more letters to filter out noised texts
        if( text && ! verifyWordCounts(text, minUkrWordCount) ) {
            if( ! options.keepInvalidFiles )
                return
        }

//            _println "\tGOOD: $file.name\n"

        if( text != null ) {
            outFile.setText(text, UTF8)
        }
        else {
            out.println "\tCopying file as is"
            outFile.setBytes(file.getBytes())
        }
    }
    
    @CompileStatic
    String cleanUp(File file, CleanOptions options, File outFile) {
        String text = null
        
//        if( file.length() > 100*1024*1024 ) {
//            System.err.println "\tWARNING: It's highly recommended to keep your files for cleanup under 10MB"
//            System.err.println "\tThis way you prevent out of memory condition and can use parallel processing (via -p argument) better"
//        }

        text = encodingModule.getText(file)
        if( ! text )
            return text

        return cleanText(text, file, outFile)
    }

    @CompileStatic
    private static String escapeNl(String text) {
        text.replace("\r", "\\r").replace("\n", "\\n")
    } 

    @CompileStatic
    String cleanText(String text, File file, File outFile) {
        int nlIdx = text.indexOf("\n")
        int dosNlIdx = text.indexOf("\r\n")
        boolean dosNlPresent = dosNlIdx >= 0 && dosNlIdx+1 == nlIdx
        if( dosNlPresent ) {
            out.println "\tFirst new line is DOS-style, using DOS new line for the whole text ($dosNlIdx)"
        }
        cleanText2(new CleanRequest(text: text, file: file, outFile: outFile, dosNl: dosNlPresent))
    }
    
    static class CleanRequest {
        String text
        File file
        File outFile
        boolean dosNl
        
        String getLineBreak() { dosNl ? "\r\n" : "\n" }
        CleanRequest forText(String text) {
            new CleanRequest(text: text, file: file, outFile: outFile, dosNl: dosNl)
        }
    }
    
        
    @CompileStatic
    String cleanText2(CleanRequest request) {
        if( request.text.length() > CHUNK_LIMIT ) {
            cleanTextParallel(request)
        }
        else {
            cleanTextInternal(request)
        }
    }
    
    private String cleanTextParallel(CleanRequest request) {
        def text = request.text
        
        out.println "\tSize ${text.length()} - splitting into chunks (by ~$CHUNK_LIMIT chars)"
        boolean oldParallel = out.options.parallel
        out.options.parallel = true
        
        ExecutorService executor = Executors.newWorkStealingPool()
        List<Future<CharSequence>> futures = []

        def sb = new StringBuilder(text.length())

        for(int ii=0; ii < text.length(); ) {
            def len = Math.min(CHUNK_LIMIT, text.length()-ii)
            // if possible adjust splitting by new line (better by paragraphs but it's hard to find how paragraphs are split)
            int nlPos = text.indexOf(request.getLineBreak(), ii + len)
//                    out.println "\t::pos: $pos"
            if( nlPos > 0 && nlPos < ii + len + CHUNK_LIMIT/10 ) {
                len = (nlPos-ii)
//                    if( pos+10 < text.length() ) {
//                        def ch = escapeNl(text[pos-10..pos+10])
//                        out.println "\t::$ch"
//                    }
            }
            
            futures.add executor.submit(getCallableForChunk(ii, len, request))
            
            ii+=len
        }

        out.println "\tSubmitted ${futures.size()} chunks in parallel"
        
        executor.shutdown()
        
        futures.each { f ->
            sb.append(f.get())
        }
        
        executor.awaitTermination(1, TimeUnit.DAYS)
        
        out.options.parallel = oldParallel
        
        sb.toString()
    }
    
    
    @CompileStatic
    private Callable<CharSequence> getCallableForChunk(int pos, int len, CleanRequest request) {
        return {
            out.init()
            def chunk = request.text[pos..<pos+len]
//            out.debug "\tchunk at $pos, len: $len"
            out.println "\tchunk at $pos, len: $len: ${escapeNl(chunk.take(20))} ... ${escapeNl(request.text[pos+len-20..<pos+len])}"
            
            def t = cleanTextInternal(request.forText(chunk))
            out.flush()
            t
        } as Callable<CharSequence>
    }
    
    @CompileStatic
    String cleanTextInternal(CleanRequest request) {
        def t00 = request.text.toString()
        
        if( t00.contains("\r") ) {
            t00 = t00.replace("\r", "")
        }

        if( ! checkEmptyLines(t00) )
            return null
        
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

        // fix weird apostrophes
        t01 = t01.replaceAll(/(?iu)([бвгґдзкмнпрстфхш])[\"\u201D\u201F\u0022\u2018\u2032\u0313\u0384\u0092´`?*]([єїюя])/, /$1'$2/) // "
        t01 = t01.replaceAll(/(?iu)[´`]([аеєиіїоуюя])/, '\u0301$1')
//        t0 = t0.replaceAll(/(?iu)([а-яіїєґ'\u2019\u02BC\u2013-]*)[´`]([а-яіїєґ'\u2019\u02BC\u2013-]+)/, { all, w1, w2
//                  def fix = "$w1'$w2"
//                knownWord(fix) ? fix : all
//        }
        
        def t10 = hyphenModule.removeSoftHyphens(t01)
//t01 = null // ml
        
        if( t10.contains('\u2028') ) {
            t10 = t10.replaceAll(/\u2028\n?/, '\n')
        }

        // digit 3 instead of letter З
        t10 = t10.replaceAll(/\b3[аa]([\h\v]*[а-яіїєґА-ЯІЇЄҐ])/, 'За$1')

        t10 = t10.replaceAll(/(чоло|Людо)[ -](в[іі]к)/, '$1$2')


        def t12 = latCyrModule.fixCyrLatMix(t10)
//t10 = null // ml
        if( ! t12 )
            return null

        if( ! checkTwoColumns(t12) )
            return null


//        if( options.modules ) {
//            t12 = runModules(t12, request, options)
//        }

        t12 = hyphenModule.separateLeadingHyphens(t12)

        t12 = hyphenModule.fixDanglingHyphens(t12)

        t12 = fixSplitWords(t12)

        checkForSpacing(t12)

        if( options.markLanguages != MarkOption.none ) {
            def req2 = new CleanRequest(text: t12, file: request.file, outFile: request.outFile)
            t12 = markLanguageModule.markRussian(request.forText(t12), outDirName)
        }

        if( request.dosNl ) {
            t12 = t12.replaceAll(/(?!<\r)\n/, "\r\n")
        }
        
        t12
    }

    
    @CompileStatic
    String fixQuotes(String text) {
        if( text.contains("''") ) {
            text = text.replaceAll(/(?<!')''(?!')/, '"')
        }
        // sometimes quotes are escaped
        text = text.replace('\\"', '"')
        text = text.replaceAll(/([бвгґдзкмнпрстфхш])\\'([яєюї])/, '$1\'$2')
        
        text = text.replace(/U+200B/, '')
        text = text.replace(/U+02BA/, '"')

        // SINGLE LOW-9 QUOTATION MARK sometimes used as a comma
        text.replace('\u201A', ',')
    }
    
    
    @CompileStatic
    String fixOi(String text) {

        if( ! options.disabledRules.contains("oi") ) {
            // промисловоі
            def t0 = text.replaceAll(/([а-яїієґА-ЯІЇЄҐ][а-яїієґ'-]+[а-яїієґ])(о[іi])\b/, { all, w1, w2 ->
                String fix = "${w1}ої"
                ltModule.knownWord(fix) ? fix : all
            })
             
            // Нацполіціі
            def t1 = t0.replaceAll(/([а-яїієґА-ЯІЇЄҐ][а-яїієґ'-]+[а-яїієґ][стц])([іi][іi])\b/, { all, w1, w2 ->
                String fix = "${w1}ії"
                ltModule.knownWord(fix) ? fix : all
            })
//t0 = null // ml
            text = t1
        }
        text
    }

    @CompileStatic
	void checkForSpacing(String text) {
		def m = text =~ /([а-яіїєґА-ЯІЇЄҐ] ){5,}/
		if( m.find() ) {
			out.println "\tWARNING: Possible spacing in words, e.g \"${m.group(0)}\""
		}
	}


    @CompileStatic
	String fixSplitWords(String text) {
		int cnt = 0
		String regex = /([а-яіїєґА-ЯІЇЄҐ'ʼ’-]*)\n([ \t]*)([а-яіїєґ][а-яіїєґ'ʼ’-]*)([,;.!?])?/
		def t1 = text.replaceAll(regex, { List<String> it ->
			if( it[4] != "."	// we don't want to join ММК ім. Ілліча
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
				out.println "\tWARNING: Suspect empty lines inside the sentence"
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


    String runModules(CleanRequest request, CleanOptions options) {

        if( options.modules == 'nanu' ) {
//            text = new CleanTextNanu(out.get()).removeMeta(text, file, options)
        }
        else
            throw new IllegalArgumentException("cleanup not supported for ${options.modules}")

        return request.text
    }


    @CompileStatic
    static String getSample(String text) {
        text.take(80).replace('\n', '\\n')
    }


    @CompileStatic
    private boolean verifyWordCounts(String text, int minUkrWordCount) {
        def ukrWords = text.split(/[^А-ЯІЇЄҐёа-яіїєґё'’ʼ-]+/).findAll{ it ==~ /[А-ЩЬЮЯІЇЄҐа-щьюяіїєґ][А-ЩЬЮЯІЇЄҐа-щьюяіїєґ'’ʼ-]+/ }
        int ukrWordCount = ukrWords.size()
        if( minUkrWordCount == 0 ) {
            if( ukrWordCount == 0 ) {
				out.println "\tWARNING: 0 Ukrainian words: " + getSample(text) // + "\n\t" + ukrWords
            }
        }
        else if( ukrWordCount < minUkrWordCount ) {
            out.println "\tERROR: Less than $minUkrWordCount Ukrainian words ($ukrWordCount): " + getSample(text) // + "\n\t" + ukrWords
            return false
        }
        out.println "\tUkrainian words: $ukrWordCount"
        //    if( ukrWordCount < 300 ) println "\t\t: " + ukrWords

        // for really big text counting chars takes long time
        // we'll just evaluate first 1MB

        def lowerTextSample = text.take(1024*1024).toLowerCase()
        int ukrLetterCount = lowerTextSample.findAll { String it -> "іїєґ".contains(it) } .size()
        int rusLetterCount = lowerTextSample.findAll { String it -> "ыэъё".contains(it) } .size()

        def minUkrainianLetters = minUkrWordCount >= 20 ? minUkrWordCount / 20 : 0
        if( ukrLetterCount < minUkrainianLetters ) {
            out.println "\tERROR: Less than $minUkrainianLetters Ukrainian letters ($ukrLetterCount): " + getSample(text)
            return false
        }

        if( ukrLetterCount < rusLetterCount ) {
            out.println "\tERROR: Less Ukrainian letters ($ukrLetterCount) than Russian ($rusLetterCount), probably russian text: " + getSample(text)
            return false
        }

        return true
    }
    
}
