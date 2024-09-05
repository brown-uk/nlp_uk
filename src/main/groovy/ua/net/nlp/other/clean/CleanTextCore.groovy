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

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

import groovy.io.FileVisitResult
import groovy.transform.CompileStatic
import picocli.CommandLine
import picocli.CommandLine.ParameterException


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
    LtModule ltModule = new LtModule()
    
    String outDirName
    List<String> exludeFiles = []
    
    
    CleanTextCore(CleanOptions options) {
        this.options = options
        out.options = options

        if( options.wordCount ) {
            minUkrWordCount = options.wordCount as int
        }
        if( minUkrWordCount > 0 ) {
            println "Minimum word limit: $minUkrWordCount"
        }
        
        if( options.excludeFromFile ) {
            exludeFiles = new File(options.excludeFromFile).readLines()
        }
    }


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

    
    static void main(String[] args) {
        
        setup()

        def options = parseOptions(args)

        new CleanTextCore(options).process()
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
                    if (it.name == 'good' || it.name.endsWith('-good') ) return FileVisitResult.SKIP_SUBTREE 
                },
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
            def fileOut = new OutputTrait(options: options)

            if( ! options.input ) {
				Path pathAbsolute = Paths.get(file.absolutePath)
				Path pathBase = Paths.get(baseDir.absolutePath)
				Path pathRelative = pathBase.relativize(pathAbsolute);
                fileOut.println "Looking at $pathRelative"
                if( file.name.startsWith(" ") || file.name.endsWith(" ") ) {
                    fileOut.println "\tWARNING: filename has leading or trailing spaces"
                }
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

            doCleanFile(file, outFile, fileOut)
            
            out.append(fileOut)
            out.flushAndPrint()
        }
    }
    

    void doCleanFile(File file, File outFile, OutputTrait out) {
        if( file.name in exludeFiles ) {
            out.println "\tExcluded file, copying as is ${file.name}"
            copyAsIs(file, outFile, out)
            return
        }
        
        if( file.size() == 0 ) {
            out.println "\tWARNING: Empty file ${file.name}"
            if( options.keepInvalidFiles ) {
                copyAsIs(file, outFile, out)
            }
            return
        }
        
        String text = cleanUp(file, options, outFile, out)

        if( ! text ) {
            if( ! options.keepInvalidFiles )
                return
        }

        // NOTE: only counting words with 2 or more letters to filter out noised texts
        if( text && ! verifyWordCounts(text, minUkrWordCount, out) ) {
            if( ! options.keepInvalidFiles )
                return
        }

//            _println "\tGOOD: $file.name\n"

        if( text != null ) {
            outFile.setText(text, UTF8)
        }
        else {
            copyAsIs(file, outFile, out)
        }
    }

    private copyAsIs(File file, File outFile, OutputTrait out) {
        out.println "\tCopying file as is"
        outFile.setBytes(file.getBytes())
    }
    
    String cleanUp(File file, CleanOptions options, File outFile, OutputTrait out) {
        String text = null
        
//        if( file.length() > 100*1024*1024 ) {
//            System.err.println "\tWARNING: It's highly recommended to keep your files for cleanup under 10MB"
//            System.err.println "\tThis way you prevent out of memory condition and can use parallel processing (via -p argument) better"
//        }

        EncodingModule encodingModule = new EncodingModule(out: out)
        text = encodingModule.getText(file)
        if( text == null ) // we have detected rtf or word
            return file.getText('UTF-8')

        if( ! text )
            return text

        return cleanText(text, file, outFile, out)
    }

    String cleanText(String text, File file, File outFile, OutputTrait out) {
        if( text =~ /\r(?!\n)/ ) {
            out.println "\tAdding U+000A to orphaned U+000D"
            text = text.replaceAll(/\r(?!\n)/, '\n')
        }
        
        int nlIdx = text.indexOf("\n")
        int dosNlIdx = text.indexOf("\r\n")
        boolean dosNlPresent = dosNlIdx >= 0 && dosNlIdx+1 == nlIdx
        if( dosNlPresent ) {
            out.println "\tFirst new line is DOS-style, using DOS new line for the whole text ($dosNlIdx)"
        }

        cleanText2(new CleanRequest(text: text, file: file, outFile: outFile, dosNl: dosNlPresent), out)
    }
    
        
    String cleanText2(CleanRequest request, OutputTrait out) {
        
        if( request.text.length() > CHUNK_LIMIT ) {
            cleanTextParallel(request, out)
        }
        else {
            def cleanTextCore2 = new CleanTextCore2(out, options, ltModule)
            cleanTextCore2.outDirName = outDirName
            
            cleanTextCore2.cleanTextInternal(request)
        }
    }
    
    private String cleanTextParallel(CleanRequest request, OutputTrait out) {
        def text = request.text
        
        out.println "\tSize ${text.length()} - splitting into chunks (by ~$CHUNK_LIMIT chars)"
        
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
            
            futures.add executor.submit(getCallableForChunk(ii, len, request, out))
            
            ii+=len
        }

        out.println "\tSubmitted ${futures.size()} chunks in parallel"
        
        executor.shutdown()
        
        futures.each { f ->
            sb.append(f.get())
        }
        
        executor.awaitTermination(1, TimeUnit.DAYS)
        
        sb.toString()
    }
    
    
    private Callable<CharSequence> getCallableForChunk(int pos, int len, CleanRequest request, OutputTrait out) {
        return {
            
            OutputTrait localOut = new OutputTrait(options: options)
            
            def chunk = request.text[pos..<pos+len]
//            out.debug "\tchunk at $pos, len: $len"
            def chunkStart = CleanUtils.escapeNl(chunk.take(20))
            def chunkEnd = CleanUtils.escapeNl(request.text[pos+len-20..<pos+len])
            localOut.println "\n\tchunk at $pos, len: $len: $chunkStart ... $chunkEnd"
            
            def cleanTextCore2 = new CleanTextCore2(localOut, options, ltModule)
            cleanTextCore2.outDirName = outDirName
            
            def t = cleanTextCore2.cleanTextInternal(request.forText(chunk))
            
            out.append(localOut)
            
            t
        } as Callable<CharSequence>
    }


    String runModules(CleanRequest request, CleanOptions options) {

        if( options.modules == 'nanu' ) {
//            text = new CleanTextNanu(out.get()).removeMeta(text, file, options)
        }
        else
            throw new IllegalArgumentException("cleanup not supported for ${options.modules}")

        return request.text
    }


    static String getSample(String text) {
        text.take(80).replace('\n', '\\n')
    }


    private static final Pattern UKR_WORD = ~/[А-ЩЬЮЯІЇЄҐа-щьюяіїєґ][А-ЩЬЮЯІЇЄҐа-щьюяіїєґ'’ʼ\u0301-]+/
    
    private boolean verifyWordCounts(String text, int minUkrWordCount, OutputTrait out) {
        def ukrWordCount = text.split(/[^А-ЯІЇЄҐёа-яіїєґё'’ʼ\u0301-]+/).count{ it ==~ UKR_WORD }
        if( minUkrWordCount == 0 ) {
            if( ukrWordCount == 0 ) {
				out.println "\tWARNING: 0 Ukrainian words: " + getSample(text) // + "\n\t" + ukrWords
            }
        }
        else if( ukrWordCount < minUkrWordCount ) {
            out.println "\tERROR: Less than $minUkrWordCount Ukrainian words ($ukrWordCount): " + getSample(text) // + "\n\t" + ukrWords
            return false
        }
        
        def lines = text.lines().count()
        out.println "\tUkrainian words: $ukrWordCount ($lines lines)"
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
