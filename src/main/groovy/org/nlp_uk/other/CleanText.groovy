#!/bin/env groovy

package org.nlp_uk.other


import static org.nlp_uk.other.CleanText.MarkOption.none

// This script reads all .txt files in given directory (default is "txt/") 
// and tries to find all with acceptable criterias for Ukrainian text (e.g. > 3k Ukrainian words)
// output files go into <dir>/good/
// NOTE:
// it also tries to fix broken encoding
// it also tries to clean up latin/cyrillic character mix
// it also tries to replace weird apostrophe characters with correct one (')
// it also tries to detect and skip two-column texts
// it also tries to merge some simple word wraps

@GrabConfig(systemClassLoader=true)
@Grab(group='org.languagetool', module='language-uk', version='5.3')
//@Grab(group='org.languagetool', module='language-uk', version='5.4-SNAPSHOT')
@Grab(group='org.languagetool', module='language-ru', version='5.3')
@Grab(group='ch.qos.logback', module='logback-classic', version='1.2.3')
@Grab(group='info.picocli', module='picocli', version='4.6.+')

@GrabConfig(systemClassLoader=true)
@Grab(group='org.codehaus.groovy', module='groovy-cli-picocli', version='3.0.6')
@Grab(group='org.languagetool', module='language-uk', version='5.1')
//@Grab(group='org.languagetool', module='language-uk', version='5.2-SNAPSHOT')

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Function
import java.util.regex.Matcher
import java.util.regex.Pattern
import picocli.CommandLine
import picocli.CommandLine.Option
import picocli.CommandLine.ParameterException
import groovy.transform.TypeChecked
import groovy.io.FileVisitResult
import groovy.transform.CompileStatic

import org.languagetool.tagging.uk.*
import org.languagetool.tokenizers.SRXSentenceTokenizer
import org.languagetool.tokenizers.uk.UkrainianWordTokenizer
import org.languagetool.tagging.ru.RussianTagger
import org.languagetool.AnalyzedToken
import org.languagetool.language.Ukrainian
//import org.nlp_uk.other.CleanTextNanu


class CleanText {
    private static final String UTF8 = StandardCharsets.UTF_8.name();

    // for higher quality text esp. short newspaper articles you need to keep it low ~100
    // for larger text with possible scanned sources you may want to go higher > 200
    // note: we count words with 2 letters and more
    int minUkrWordCount = 0

    Map<String, String> KNOWN_MIXES =
    [
        "ТаблоID": "Табло@@ID",
        "Фirtka": "Ф@@irtka"
    ]

    Map<String, String> latToCyrMap = [
        'a' : 'а',
        'c' : 'с',
        'e' : 'е',
        'i' : 'і',
        'o' : 'о',
        'p' : 'р',
        'x' : 'х',
        'y' : 'у',
        'A' : 'А',
        'B' : 'В',
        'C' : 'С',
        'E' : 'Е',
        'H' : 'Н',
        'I' : 'І',
        'K' : 'К',
        'M' : 'М',
        'O' : 'О',
        'P' : 'Р',
        'T' : 'Т',
        'X' : 'Х',
        'Y' : 'У',
        "á" : "а́",
        "Á" : "А́",
        "é" : "е́",
        "É" : "Е́",
        "í" : "і́",
        "Í" : "І́",
        "ḯ" : "ї́",
        "Ḯ" : "Ї́",
        "ó" : "о́",
        "Ó" : "О́",
        "ú" : "и́",
        "ý" : "у́",
        "Ý" : "У́"
    ]

    Map<String,String> cyrToLatMap = [:]

    @Lazy
    UkrainianTagger ukTagger = { new UkrainianTagger() }()
	@Lazy
	def ruTagger = { new RussianTagger() }()
    @Lazy
    SRXSentenceTokenizer ukSentTokenizer = new SRXSentenceTokenizer(new Ukrainian())
    @Lazy
    UkrainianWordTokenizer ukWordTokenizer = new UkrainianWordTokenizer()

    CleanOptions options

    ThreadLocal<PrintStream> out = new ThreadLocal<>()
    ThreadLocal<ByteArrayOutputStream> outSw = new ThreadLocal<>()
    String outDirName
            

    CleanText(def options) {
        this.options = options

        latToCyrMap.each{ String k, String v -> cyrToLatMap[v] = k }

        if( options.wordCount ) {
            minUkrWordCount = options.wordCount as int
        }
        if( minUkrWordCount > 0 ) {
            println "Minimum word limit: $minUkrWordCount"
        }
    }

    void debug(str) {
        if( options.debug ) {
            println "\tDEBUG: $str"
        }
    }

    static class CleanOptions {
        //        @Parameters(arity="1", paramLabel="input", description="The file(s) whose checksum to calculate.")
        @Option(names = ["-i", "--input"], arity="1", description = ["Input file"])
        String input
        @Option(names = ["-o", "--output"], arity="1", description = ["Output file ((default: input file with .out added before extention)"])
        String output
        @Option(names = ["--dir"], arity="1", description = ["Directory to process *.txt in (default is current directory)"])
        String dir
        @Option(names = ["-k", "--keepInvalidFiles"], description = ["Do not discard invalid files"])
        boolean keepInvalidFiles
        @Option(names = ["-n", "--allowTwoColumns"], description = ["do not discard two-column text"])
        boolean allowTwoColumns
        @Option(names = ["-w", "--wordCount"], description = "Minimum Ukrainian word count")
        int wordCount
        @Option(names = ["-c", "--clean"], description = ["Clean old files in <dir>-good/ directory"])
        boolean clean
        @Option(names = ["-r", "--recursive"], description = ["Process directories recursively"])
        boolean recursive
        @Option(names = ["-d", "--debug"], description = ["Debug output"])
        boolean debug
        @Option(names = ["-z", "--markLanguages"], description = ["Mark text in another language, modes: none, mark, cut (supported language: Russian)"], defaultValue="none")
        MarkOption markLanguages = none
        @Option(names = ["-p", "--parallel"], description = ["Process files in parallel"])
        boolean parallel
        @Option(names = ["-m", "--modules"], description = ["Extra cleanup: remove footnotes, page numbers etc. (supported modules: nanu)"])
        List<String> modules
//        @Option(names = ["--singleThread"], description = ["Always use single thread (default is to use multithreading if > 2 cpus are found)"])
//        boolean singleThread
        @Option(names = ["-q", "--quiet"], description = ["Less output"])
        boolean quiet
        @Option(names= ["-h", "--help"], usageHelp= true, description= "Show this help message and exit.")
        boolean helpRequested

    }
    
    enum MarkOption {
        none, mark, cut
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

        def options = parseOptions(args)

        return new CleanText(options).process()
    }

    void println(String txt) {
        out.get().println(txt)
    }


    int process() {

        if( ! options.input ) {

            def dir = options.dir 
				? options.dir 
				: options.arguments()
					? options.arguments()[0]
					: "."

            File baseDir = new File(dir)
            def files = []

            int maxDepth_ = options.recursive ? -1 : 0;

            baseDir.traverse(type: groovy.io.FileType.FILES, 
                maxDepth: maxDepth_,
                preDir       : { if (it.name == 'good' || it.name.endsWith('-good') ) return FileVisitResult.SKIP_SUBTREE },
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
                        System.err.println "Failed to delete $fn"
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

    void processFiles(files, baseDir, outFilename, outDirName) {
        
        def stream = options.parallel ? files.parallelStream() : files.stream()

        if( options.parallel ) {
            println "Processing ${files.size()} files in parallel"
        }
        else {
            out.set(System.out)
            if( ! options.input ) {
                println "Processing ${files.size()} files"
            }
        }

		println ""

        stream.forEach{ File file ->
            if( options.parallel ) {
                def byteStream = new ByteArrayOutputStream()
                outSw.set(byteStream)
                out.set(new PrintStream(byteStream))
            }

            if( ! options.input ) {
				Path pathAbsolute = Paths.get(file.absolutePath)
				Path pathBase = Paths.get(baseDir.absolutePath)
				Path pathRelative = pathBase.relativize(pathAbsolute);
                println "Looking at " + pathRelative
            }

			String origText = file.getText(UTF8)
            String text = origText

            text = cleanUp(text, file, options)
            if( ! text ) {
                if( options.parallel ) {
                    out.get().flush()
                    System.out.println(outSw.get().toString(UTF8))
                }
				
				if( ! options.keepInvalidFiles ) 
					return
            }

            // NOTE: only counting words with 2 or more letters to filter out noised texts
            if( text && ! verifyWordCounts(text, minUkrWordCount) ) {
                if( options.parallel ) {
                    out.get().flush()
                    System.out.println(outSw.get().toString(UTF8))
                }
				if( ! options.keepInvalidFiles ) 
					return
            }


//            println "\tGOOD: $file.name\n"


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

			if( text != null ) {
				outFile.setText(text, UTF8)
			}
			else {
				println "\tCopying file as is"
				outFile.bytes = file.bytes
			}

            if( options.parallel ) {
                out.get().flush()
                System.out.println(outSw.get().toString(UTF8))
            }
        }

		println "\nDone"
    }


    String removeSoftHyphens(String text) {
        if( text.contains("\u00AD") ) {
            println "\tremoving soft hyphens: "
//            text = text.replaceAll(/[ \t]*\u00AD[ \t]*([а-яіїєґА-ЯІЇЄҐ'ʼ’-]+)([,;.!?])?/, '$1$2')
//            text = text.replaceAll(/\u00AD(?!\n {10,}[А-ЯІЇЄҐ])(\n?[ \t]*)([а-яіїєґА-ЯІЇЄҐ'ʼ’-]+)([,;.!?])?/, '$2$3$1')
            text = text.replaceAll(/([а-яіїєґА-ЯІЇЄҐa-zA-Z])\u00AD+(\n[ \t]*)([а-яіїєґА-ЯІЇЄҐa-zA-Z'ʼ’-]+)([,;.!?])?/, '$1$3$4$2')
            text = text.replaceAll(/([а-яіїєґА-ЯІЇЄҐa-zA-Z:. ])\u00AD+([а-яіїєґА-ЯІЇЄҐa-zA-Z'ʼ’ -])/, '$1$2')
//            text = text.replaceAll(/(?i)([А-ЯІЇЄҐ:. ])\u00AD+([А-ЯІЇЄҐ'ʼ’ -])/, '$1$2')
//            text = text.replaceAll(/([А-ЯІЇЄҐA-Z])\u00AD(\n[ \t]*)([А-ЯІЇЄҐA-Z'ʼ’-]+)([,;.!?])?/, '$1$3$4$2')
           // text = text.replace('\u00AD', '-')
        }
        return text
    }
    
    
    @CompileStatic
    String cleanUp(String text, File file, CleanOptions options) {
        if( file.length() > 100 && file.bytes[0..3] == [0x50, 0x4B, 0x03, 0x04] ) {
            println "\tERROR: found zip file, perhaps it's a Word document?"
            return null
        }
        if( file.length() > 100 && text.startsWith("{\rtf") ) {
            println "\tERROR: found \"{\rtf\", perhaps it's an RTF document?"
            return null
        }

		int nlIdx = text.indexOf("\n")
		int dosNlIdx = text.indexOf("\r\n")
		boolean dosNlPresent = dosNlIdx >= 0 && dosNlIdx+1 == nlIdx
		
        if( text.contains("\r") ) {
//            println "\tRemoving \\r"
            text = text.replace("\r", "")
        }

		if( ! isUTF8(file.bytes) ) {
            println "\tWARNING: file is not in UTF-8 encoding"
			text = fixEncoding(text, file)
			if( text == null )
				return null
		}

        if( ! checkEmptyLines(text) )
            return null             
        
        if( text.contains("''") ) {
            text = text.replaceAll(/(?<!')''(?!')/, '"')
        }

        // SINGLE LOW-9 QUOTATION MARK sometimes used as a comma
        text = text.replace('\u201A', ',')

        // weird ї and й via combining characters
        text = text.replaceAll(/[іi]\u0308/, 'ї')
        text = text.replace(/и\u0306/, 'й')

        // fix weird apostrophes
        text = text.replaceAll(/([бвгґдзкмнпрстфхш])[\"\u201D\u201F\u0022\u2018\u2032\u0313\u0384\u0092´`?*]([єїюя])/, /$1'$2/) // "

        text = removeSoftHyphens(text)

        if( text.contains('\u2028') ) {
            text = text.replaceAll(/\u2028\n?/, '\n')
        }

        // digit 3 instead of letter З
        text = text.replaceAll(/\b3[аa]([\h\v]*[а-яіїєґА-ЯІЇЄҐ])/, 'За$1')

        // CO/CO2 with cyr/lat mix
        text = text.replaceAll(/\b(СO|CО)(2?)\b/, 'CO$2')
        // CO2 with cyr
        text = text.replaceAll(/\bСО2\b/, 'CO2')
        

        text = fixCyrLatMix(text, file)
        if( ! text )
            return null

        if( ! checkTwoColumns(text) )
            return null


        if( options.modules ) {
            text = removeMeta(text, file, options)
        }

        text = separateLeadingHyphens(text)

//        text = removeSoftHyphens(text)

        text = fixDanglingHyphens(text, file)

		text = fixSplitWords(text, file)
		
		checkForSpacing(text, file)
		
		if( dosNlPresent ) {
			println "\tFirst new line is DOS-style, using DOS new line for the whole text"
			text = text.replaceAll(/(?!<\r)\n/, "\r\n")
		}
		
		if( options.markLanguages != none ) {
		    text = markRussian(text, file)
		}

        text
    }

	String markRussian(String text, File file) {
        Pattern pattern = ~/(?s)<span lang="ru"( rate="[0-9.]+")?>(.*?)<\/span>/
        
        // clean previous marks
        text = pattern.matcher(text).replaceAll('$2')
        
        // by paragraphs now
		List<String> chunks = text.split(/\n\n/) // ukSentTokenizer.tokenize(text)
		
        def ruChunks = []
        
		text = chunks
            .collect { String sent ->
                float ukRate, ruRate
                (ukRate, ruRate) = evalChunk(sent)
                
                if( ukRate < ruRate ) {
                    ruRate = Math.round(ruRate * 100)/100
                    String marked = "<span lang=\"ru\" rate=\"${ruRate}\">$sent</span>".replaceFirst(/(?s)([\h\v]+)(<\/span>)$/, '$2$1')
                    if( options.markLanguages == MarkOption.mark ) {
                        marked
                    }
                    else {
                        ruChunks << marked
                        '<span lang="ru">---</span>'
                    }
                }
                else {
                    sent
                }
        
            }
			.join("\n\n")
           
        if( options.markLanguages == MarkOption.cut ) {
            String ruText = ruChunks.join("\n\n")
            def ruFile
            if( outDirName ) {
                ruFile = new File(outDirName, file.name.replaceFirst(/\.txt/, '.ru.txt'))
            }
            else {
                ruFile = new File(file.absolutePath.replaceFirst(/\.txt/, '.ru.txt'))
            }
            ruFile.setText(ruText, UTF8)
        }

        text
    }

    def evalChunk(String text) {
        
        double ukCnt = 0
        int ruCnt = 0
        int totalCnt = 0
        int ruCharCnt = 0

        def chunks = ukWordTokenizer.tokenize(text)
            .findAll{ it && it ==~ /(?ius)[а-яіїєґё'\u2019\u02BC\u0301-]+/ }
            .collect { it.replaceAll(/^['\u2019\u02BC]+|['\u2019\u02BC]+$/, '') }
            .findAll { it ==~ /(?ius)[а-яіїєґё'\u2019\u02BC\u0301-]+/ }

        if( chunks.isEmpty() )
            return [1.0, 0.0]
            
        int ukSum = 0
        int ruSum = 0
        int ukSum10 = 0
        int ruSum10 = 0
        for(String w: chunks) {
            
            int ukWeight = getUkWordRating(w)
            ukSum += ukWeight
            if( ukWeight == 10 ) {
                ukSum10 += ukWeight
            }
            def debugStr = "$w: uk: $ukWeight"
            if( ukWeight < 10 ) {
                int ruWeight = w =~ /(?iu)[ыэёъ]/ ? 10 : knownWordRu(w) ? 8 : 0
                
                // workaround for 1-letter abbreviations that Russian tagger claims to know
                if( w ==~ /(?iu)[бвгдежзклмнпрстуфхцчшщю]/ ) {
                    ruWeight = 0
                }
                 
                ruSum += ruWeight

                if( ruWeight == 10 ) {
                    ruSum10 += ruWeight
                }
                debugStr += ", ru: $ruWeight"
            }
//            println debugStr
        }

        float ukRate = ukSum / chunks.size() / 10
        float ruRate = ruSum / chunks.size() / 10
        
        if( ruSum10 > 0 && ukSum10 == 0 ) {
            ruRate = 1
        }

        //println ":: uk: $ukRate ru: $ruRate words: ${chunks.size()}"

        if( ukRate == 0 && ruRate > 0.1 ) {
            ruRate = 1
        }

//        println "check:: '${text.trim()}' : $ukRate, $ruRate"
        
        [ukRate, ruRate]
    }

	@CompileStatic
	private static List<String> splitWithDelimiters(String str, Pattern delimPattern) {
		List<String> parts = new ArrayList<String>();
	
		Matcher matcher = delimPattern.matcher(str);
	
		int lastEnd = 0;
		while (matcher.find()) {
		  int start = matcher.start();
	
		  if (lastEnd != start) {
			String nonDelim = str.substring(lastEnd, start);
			parts.add(nonDelim);
		  }
	
		  String delim = matcher.group();
		  parts.add(delim);
	
		  lastEnd = matcher.end();
		}
	
		if (lastEnd != str.length()) {
		  String nonDelim = str.substring(lastEnd);
		  parts.add(nonDelim);
		}
	
		return parts;
    }
	


	void checkForSpacing(text, file) {
		
		def m = text =~ /([а-яіїєґА-ЯІЇЄҐ] ){5,}/
		if( m.find() ) {
			println "\tWARNING: Possible spacing in words, e.g \"" + m[0][0] + "\""
		}
		
	}


	String fixSplitWords(String text, File file) {
		int cnt = 0
		String regex = /([а-яіїєґА-ЯІЇЄҐ'ʼ’-]*)\n([ \t]*)([а-яіїєґ][а-яіїєґ'ʼ’-]*)([,;.!?])?/
		text = text.replaceAll(regex, { List<String> it ->
			if( it[4] != "."	// we don't want to join ММК ім. Ілліча
				&& it[1].length() + it[3].length() >= 4
				&& ! (it[0] =~ /[А-ЯІЇЄҐ]{2,}/)
				&& ! knownWordTwoLang(it[1])
				&& ! knownWordTwoLang(it[3])
				&& knownWord(it[1] + it[3]) ) {
				cnt += 1
				// print "."
				it[1] + it[3] + (it[4] ?: "") + "\n" + it[2]
			}
			else {
				it[0]
			}
		})
		if( cnt ) {
			println "\t$cnt word splits removed"
		}
		text
	}


    boolean checkTwoColumns(text) {
        if( ! options.allowTwoColumns ) {

            if ( text.length() > 100*1024 ) {
                text = text.take(100*1024)
            }

            if( text.count("    ") >= 5 ) {
                def matcher = text =~ /(?ium)(.*?[а-яїієґ] {4,})[а-яіїєґ].{4}/
                matcher.find()
                def matchSize = matcher.size()
                if( matchSize >= 5
                && matchSize > text.count("\n") * 3 / 4
                && matcher[0][1].length() == matcher[2][1].length()
                && matcher[0][1].length() == matcher[4][1].length() ) {
                    println "\tERROR: two columns detected, skipping...:"
                    println "\t${matcher[0][0]}\n\t${matcher[2][0]}\n\t${matcher[4][0]}"
                    return false
                }
            }
        }
        return true
    }

    boolean checkEmptyLines(text) {
        if( text.count("\n\n") > 5 ) {
            if ( text.length() > 100*1024 ) {
                text = text.take(100*1024)
            }

//            def nonEmptyLines = text.getLines().findAll { it =~ /[^\s]/ }
//            if( nonEmptyLines.count { it.length() > 120 } > 5 ) {
//                println "\tVery long lines found, probably unwrapped paragraphs..."
//                return true
//            }

			// headers often have titles without period
			
			def lines = text.readLines()
			if( lines.size() > 2 ) {
				lines = lines[2..<lines.size()]
			}
			
			text = lines.join("\n")
//            def matcher = text =~ /(?ius)[а-яїієґ0-9,—–-]\s*\n\n[а-яіїєґ0-9]/
            def matcher = text =~ /(?us)[а-яїієґА-ЯІЇЄҐ,:—–-]\s*\n\n[а-яіїєґ]/
			if( matcher ) {
				println "\tWARNING: Suspect empty lines inside the sentence"
				return true
			}

//            def nonEmptyLineCnt = nonEmptyLines.size()
//            if( matcher.size() > nonEmptyLineCnt / 7 ) {
//                println "\tWARNING: Suspect empty lines between sentences: ${matcher.size()}, total non-empty: $nonEmptyLineCnt"
//                return true
//            }
        }
        return true
    }


    String removeMeta(String text, File file, def options) {

        if( options.modules == 'nanu' ) {
//            text = new CleanTextNanu(out.get()).removeMeta(text, file, options)
        }
        else
            throw new IllegalArgumentException("cleanup not supported for " + options.removeMeta)

        return text
    }


    boolean knownWord(String word) {
        try {
            return ! ukTagger.getAnalyzedTokens(word)[0].hasNoTag()
        }
        catch (Exception e) {
            System.err.println("Failed on word: " + word)
            throw e
        }
    }

	boolean knownWordTwoLang(String word) {
		try {
			return ! ukTagger.getAnalyzedTokens(word)[0].hasNoTag() \
				|| ! ruTagger.getAnalyzedTokens(word)[0].hasNoTag()
		}
		catch (Exception e) {
			System.err.println("Failed dual lang on word: " + word)
			throw e
		}
	}

    int getUkWordRating(String word) {
        if( word =~ /(?iu)[іїєґ'\u2019\u02BC]|^й$/ )
            return 10
        
        try {
            List<AnalyzedToken> token = ukTagger.getAnalyzedTokens(word) 
            if( token[0].hasNoTag() )
                return 0
            if( token.find { AnalyzedToken t -> t.getPOSTag().contains(":bad") } )
                return 2
//            if( token.find { AnalyzedToken t -> t.getPOSTag() =~ /:prop:geo|noun:inanim:.:v_kly/ } )
//                return 5
            return 8
        }
        catch (Exception e) {
            System.err.println("Failed on word: " + word)
            throw e
        }
    }

    boolean knownWordRu(String word) {
        try {
            return ! ruTagger.getAnalyzedTokens(word)[0].hasNoTag()
        }
        catch (Exception e) {
            System.err.println("Failed on word: " + word)
            throw e
        }
    }

    String removeMix(String text) {
        int count1 = 0
        int count2 = 0

        // latin digits

        while( text =~ /[XVI][ХІ]|[ХІ][XVI]/ ) {
            text = text.replaceAll(/([XVI])([ХІ])/, { all, lat, cyr ->
                lat + cyrToLatMap[cyr]
            })
            text = text.replaceAll(/([ХІ])([XVI])/, { all, cyr, lat ->
                cyrToLatMap[cyr] + lat
            })
        }

        // 1st tier

        // exclusively cyrillic letter followed by latin looking like cyrillic

        text = text.replaceAll(/([бвгґдєжзийклмнптфцчшщьюяБГҐДЄЖЗИЙЛПФХЦЧШЩЬЮЯ]['’ʼ]?)([aceiopxyABCEHIKMOPTXYáÁéÉíÍḯḮóÓúýÝ])/, { all, cyr, lat ->
            debug "mix: 1.1"
            count1 += 1
            cyr + latToCyrMap[lat]
        })

        // exclusively cyrillic letter preceeded by latin looking like cyrillic

        text = text.replaceAll(/([aceiopxyABCEHIKMOPTXYáÁéÉíÍḯḮóÓúýÝ])(['’ʼ]?[бвгґдєжзийклмнптфцчшщьюяБГҐДЄЖЗИЙЛПФХЦЧШЩЬЮЯ])/, { all, lat, cyr ->
            debug "mix: 1.2"
            count1 += 1
            latToCyrMap[lat] + cyr
        })


        text = text.replaceAll(/([bdfghjklmnrstuvwzDFGJLNQRSUVWZ]['’ʼ]?)([асеіорхуАВСЕНІКМНОРТХУ])/, { all, lat, cyr ->
            debug "mix: 1.3"
            count2 += 2
            lat + cyrToLatMap[cyr]
        })

        text = text.replaceAll(/([асеіорхуАВСЕНІКМНОРТХУ])(['’ʼ]?[bdfghjklmnrstuvwzDFGJLNQRSUVWZ])/, { all, cyr, lat ->
            debug "mix: 1.4"
            count2 += 2
            cyrToLatMap[cyr] + lat
        })


        // 2nd tier - try all Cyrillic
        // if we convert all Latin to Cyrillic and find it in the dictionary use conversion

        text = text.replaceAll(/[а-яіїєґА-ЯІЇЄҐ'ʼ’a-zA-ZáÁéÉíÍḯḮóÓúýÝ-]+/, { it ->

            if( it =~ /[а-яіїєґА-ЯІЇЄҐ]['’ʼ]?[aceiopxyABCEHIKMHOPTXYáÁéÉíÍḯḮóÓúýÝ]/
            || it =~ /[aceiopxyABCEHIKMHOPTXYáÁéÉíÍḯḮóÓúýÝ]['’ʼ]?[а-яіїєґА-ЯІЇЄҐ]/ ) {
                //            println "Found mix in: $it, known to LT: " + knownWord(it)
                if( ! knownWord(it) ) {
                    def fixed = it.replaceAll(/[aceiopxyABCEHIKMHOPTXYáÁéÉíÍḯḮóÓúýÝ]/, { lat -> latToCyrMap[lat] })
                    def fixedCleaned = fixed.replace('\u0301', '')
                    //                println "\tfixed $fixed known to LT: " + knownWord(fixedCleaned)
                    if( knownWord(fixedCleaned) ) {
                        count1 += 1
                        return fixed
                    }
                }
            }
            return it
        })


        // 3nd tier - least reliable

        // latin letter that looks like Cyrillic between 2 Cyrillics

        text = text.replaceAll(/([а-яіїєґА-ЯІЇЄҐ]['’ʼ]?)([aceiopxyABCEHIKMHOPTXYáÁéÉíÍḯḮóÓúýÝ])(['’ʼ]?[а-яіїєґА-ЯІЇЄҐ])/, { all, cyr, lat, cyr2 ->
            count1 += 1
            cyr + latToCyrMap[lat] + cyr2
        })

        // Cyrillic letter that looks like Latin between 2 Latin

        text = text.replaceAll(/([a-zA-Z]['’ʼ]?)([асеіорхуАВСЕНІКМНОРТХУ])(['’ʼ]?[a-zA-Z])/, { all, lat, cyr, lat2 ->
            count2 += 2
            lat + cyrToLatMap[cyr] + lat2
        })

        println "\tconverted $count1 lat->cyr, $count2 cyr->lat"

        return text
    }

    @CompileStatic
    static String getSample(String text) {
        text[0..<Math.min(text.size(), 80)].replace('\n', '\\n')
    }


    @CompileStatic
    String tryCp1251(File file) {
        def cp1251Text = file.getText("cp1251")
        if( cp1251Text =~ /(?iu)[сц]ьк|ння|від|[іи]й|ої|ти| [ійвузао] | н[еі] | що / ) {
			return cp1251Text
		}
		return null
    }

    @CompileStatic
    String fixEncoding(String text, File file) {
        if( text.contains("\u008D\u00C3") ) { // completely broken encoding for «ій»
            println "\tWARNING: nonfixable broken encoding found, garbage will be left in!"
			return null
        }

        if( text.contains("éîãî") ) {
            println "\tWARNING: broken encoding"

            // some text (esp. converted from pdf) have broken encoding in some lines and good one in others

            int convertedLines = 0
            int goodLines = 0
            text = text.split(/\n/).collect { String line->
                if( line.trim() && ! (line =~ /(?iu)[а-яіїєґ]/) ) {
                    line = new String(line.getBytes("cp1252"), "cp1251")
                    convertedLines += 1
                }
                else {
                    goodLines += 1
                }
                line
            }
            .join('\n')


            if( text.contains("éîãî") ) {
                println "\tERROR: still broken: encoding mixed with good one"
                return null
            }

            //        text = text.replaceAll(/([бвгґдзклмнпрстфхцшщ])\?([єїюя])/, '$1\'$2')

            println "\tEncoding fixed (good lines: $goodLines, convertedLines: $convertedLines, text: " + getSample(text)
        }
        else {
			String cp1251Text = tryCp1251(file)
			if( cp1251Text ) {
                println "\tWARNING: cp1251 encoding found"

                text = cp1251Text

                if( text.size() < 10 ) {
                    println "\tFile size < 10 chars, probaby cp1251 conversion didn't work, skipping"
                    return null
                }

                println "\tEncoding converted: " + getSample(text)
			}
        }

        if( text.contains("\uFFFD") ) {
            println "\tERROR: File contains Unicode 'REPLACEMENT CHARACTER' (U+FFFD)"
            return null
        }

        return text
    }

//    @CompileStatic
    String fixCyrLatMix(String text, File file) {

        if( text =~ /[а-яіїєґА-ЯІЇЄҐ][a-zA-Zóáíýúé]|[a-zA-Zóáíýúé][а-яіїєґА-ЯІЇЄҐ]/ ) {
            KNOWN_MIXES.each { String k, String v ->
                text = text.replace(k, v)
            }

            if( text =~ /[а-яіїєґА-ЯІЇЄҐ][a-zA-Zóáíýúé]|[a-zA-Zóáíýúé][а-яіїєґА-ЯІЇЄҐ]/ ) {
                println "\tlatin/cyrillic mix"

                text = removeMix(text)

                if( text =~ /[а-яіїєґА-ЯІЇЄҐ][a-zA-Zóáíýúé]|[a-zA-Zóáíýúé][а-яіїєґА-ЯІЇЄҐ]/ ) {
                    println "\tWARNING: still Latin/Cyrillic mix"
                }
            }

            KNOWN_MIXES.each { String k, String v ->
                text = text.replace(v, k)
            }
        }

        // Latin a, o and i
        text = text.replaceAll(/([а-яіїєґ]), a ([А-ЯІЇЄҐа-яіїєґ])/, '$1, а $2')
        text = text.replaceAll(/([а-яіїєґ]) i ([А-ЯІЇЄҐа-яіїєґ])/, '$1 і $2')
		text = text.replaceAll(/([а-яіїєґ]) o ([А-ЯІЇЄҐа-яіїєґ])/, '$1 о $2')
		
        return text
    }

//    @CompileStatic
    String fixDanglingHyphens(String text, File file) {
        if( text.contains("-\n") && text =~ /[а-яіїєґА-ЯІЇЄҐ]-\n/ ) {
            println "\tsuspect word wraps"
            def cnt = 0
            int cntWithHyphen = 0

            // e.g.: депутат-\n«мажоритарник»
            text = text.replaceAll(/([а-яіїєґА-ЯІЇЄҐ-]+)-\n([ \t]*)([«„"][а-яіїєґ'ʼ’-]+[»“"])([,;.!?])?/, { List<String> it ->
                cntWithHyphen += 1
                it[1] + "-" + it[3] + (it[4] ?: "") + "\n" + it[2]
            })

			def first = null
            text = text.replaceAll(/([а-яіїєґА-ЯІЇЄҐ'ʼ’-]+)-\n([ \t]*)([а-яіїєґ'ʼ’-]+)([,;.!?])?/, { List<String> it ->
				if( ! first )
					first = it[0] ? it[0].replace('\n', "\\n") : it[0]
                //            println "== " + (it[1] + "-" + it[3]) + ", known: " + knownWord(it[1] + "-" + it[3])
                // consider words with two or more hyphens with one of them before end of line to be rare
                if( /*it[0].count('-') > 1 ||*/ ! knownWord(it[1] + "-" + it[3]) ) {
                    //                println "== " + (it[1] + it[3]) + ", known: " + knownWord(it[1] + it[3])
                    if( knownWord(it[1] + it[3]) ) {
                        cnt += 1
                        //                print "."
                        it[1] + it[3] + (it[4] ?: "") + "\n" + it[2]
                    }
                    else {
                        it[0]
                    }
                }
                else {
                    cntWithHyphen += 1
                    //                print ","
                    it[1] + "-" + it[3] + (it[4] ?: "") + "\n" + it[2]
                }
            })
            println "\t\t$cnt word wraps removed, $cntWithHyphen newlines after hyphen removed"
            if( cnt == 0 && cntWithHyphen == 0 ) {
                println "\t\tfirst match: \"$first\""
            }
        }

        if( text =~ /¬ *\n/ ) {
            println "\tsuspect word wraps with ¬:"
            text = text.replaceAll(/([а-яіїєґА-ЯІЇЄҐ'ʼ’-]+)¬ *\n([ \t]*)([а-яіїєґ'ʼ’-]+)/, '$1$3\n$2')
            println "\t\t¬ word wraps removed"
        }

        return text
    }

    String separateLeadingHyphens(String text) {

        def regex = ~/^([-\u2013\u2014])([А-ЯІЇЄҐ][а-яіїєґ'ʼ’-]+|[а-яіїєґ'ʼ’-]{4,})/

        boolean newLineEnd = text.endsWith("\n")

        def converted = 0
        text = text.readLines()
        .collect { line ->
            def matcher = regex.matcher(line)
            if( matcher ) {
              //println ":: ${matcher[0][1]}"
              def match = matcher[0]
                if( knownWord(match[2]) ) {
                    converted += 1
                    line = matcher.replaceFirst('$1 $2')
                }
            }
            line
        }
        .join("\n")

        if( converted ) {
            println "\tConverted leading hyphens: ${converted}"
        }

        if( newLineEnd ) {
            text += "\n"
        }

		
		def regex2 = ~/ -[а-яіїєґ]{4,}/
		if( regex2.matcher(text) ) {
			int cnt = 0
			def first = null
			
			text.readLines()
			.each { line ->
				def matcher = regex2.matcher(line)
				while( matcher.find() ) {
					// println ":: " + line
					cnt += 1
					if( ! first )
						first = matcher[0]
				}
			}
			if( cnt ) {
				println "\tWARNING: found $cnt suspicious hypens after space, e.g. \"$first\""
			}
		}
		
        text
    }

//    @CompileStatic
    private boolean verifyWordCounts(String text, int minUkrWordCount) {
        def ukrWords = text.split(/[^А-ЯІЇЄҐёа-яіїєґё'’ʼ-]+/).findAll{ it ==~ /[А-ЩЬЮЯІЇЄҐа-щьюяіїєґ][А-ЩЬЮЯІЇЄҐа-щьюяіїєґ'’ʼ-]+/ }
        int ukrWordCount = ukrWords.size()
        if( minUkrWordCount == 0 ) {
            if( ukrWordCount == 0 ) {
				println "\tWARNING: 0 Ukrainian words: " + getSample(text) // + "\n\t" + ukrWords
            }
        }
        else if( ukrWordCount < minUkrWordCount ) {
            println "\tERROR: Less than $minUkrWordCount Ukrainian words ($ukrWordCount): " + getSample(text) // + "\n\t" + ukrWords
            return false
        }
        println "\tUkrainian words: $ukrWordCount"
        //    if( ukrWordCount < 300 ) println "\t\t: " + ukrWords

        // for really big text counting chars takes long time
        // we'll just evaluate first 1000k

        def lowerTextSample = text.toLowerCase().take(1024*1024)
        int ukrLetterCount = lowerTextSample.findAll { String it -> "іїєґ".contains(it) } .size()
        int rusLetterCount = lowerTextSample.findAll { String it -> "ыэъё".contains(it) } .size()

        def minUkrainianLetters = minUkrWordCount >= 20 ? minUkrWordCount / 20 : 0
        if( ukrLetterCount < minUkrainianLetters ) {
            println "\tERROR: Less than $minUkrainianLetters Ukrainian letters ($ukrLetterCount): " + getSample(text)
            return false
        }

        if( ukrLetterCount < rusLetterCount ) {
            println "\tERROR: Less Ukrainian letters ($ukrLetterCount) than Russian ($rusLetterCount), probably russian text: " + getSample(text)
            return false
        }

        return true
    }
	
    @CompileStatic
	private static boolean isUTF8(byte[] pText) {

		int expectedLength = 0;

		for (int i = 0; i < pText.length && i < 300; i++) {
			if ((pText[i] & 0b10000000) == 0b00000000) {
				expectedLength = 1;
			} else if ((pText[i] & 0b11100000) == 0b11000000) {
				expectedLength = 2;
			} else if ((pText[i] & 0b11110000) == 0b11100000) {
				expectedLength = 3;
			} else if ((pText[i] & 0b11111000) == 0b11110000) {
				expectedLength = 4;
			} else if ((pText[i] & 0b11111100) == 0b11111000) {
				expectedLength = 5;
			} else if ((pText[i] & 0b11111110) == 0b11111100) {
				expectedLength = 6;
			} else {
				return false;
			}

			while (--expectedLength > 0) {
				if (++i >= pText.length) {
					return false;
				}
				if ((pText[i] & 0b11000000) != 0b10000000) {
					return false;
				}
			}
		}

		return true;
	}
}
