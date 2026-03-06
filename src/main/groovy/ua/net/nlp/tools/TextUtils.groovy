package ua.net.nlp.tools

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Function
import java.util.regex.Pattern
import java.util.stream.Stream
import org.apache.commons.io.IOUtils
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import picocli.CommandLine.Option
import ua.net.nlp.tools.tag.TagOptions


@CompileStatic
public class TextUtils {
    
    static int BUFFER_SIZE = 4*1024
    static int MAX_PARAGRAPH_SIZE = 200*1024

    static class IOFiles {
        String inputName
        String outputName
        InputStream inputStream
        PrintStream outputStream
        boolean noTag

        public Stream<String> getLines() {
            if( inputStream )
                return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
            
            return inputName == "-"
                ? new Scanner(System.in).findAll(".*").map(mr -> mr.group())
                : Files.lines(Path.of(inputName))
        }
        
        public PrintStream getOutputStream(OptionsBase options) {
            if( options.noTag )
                return null
            
            if( outputStream == null ) {
                if( outputName == "-" ) {
                    outputStream = System.out
                }
                else {
                    if( ! noTag ) {
                        // System.err.println "Still tagging"
                        def of = new File(outputName)
                        of.setText('')    // to clear out output file
                        outputStream = new PrintStream(of, "UTF-8")
                    }
                }
            }
            return outputStream;
        }
    }

    static IOFiles prepareInputOutput(OptionsBase options) {
        
        if( ! options.quiet && options.input == "-" ) {
            System.err.println("Reading from stdin...")
        }

        if( ! options.quiet ) {
            if( options.noTag ) {
                System.err.println("Collecting stats only...")
            }
            else if( options.output != "-" ) {
                System.err.println ("writing into ${options.output}")
            }
        }
        
        def ioFiles = new IOFiles(inputName: options.input, outputName: options.output, noTag: options.isNoTag())

        return ioFiles
    }
    
    static void processByParagraph(OptionsBase options, Closure closure, Closure resultClosure) {
        IOFiles files = prepareInputOutput(options)
        processByParagraphInternal(options, files, closure, resultClosure)
    }
    
    static void processByParagraphInternal(OptionsBase options, IOFiles ioFiles, Closure processClosure, Closure resultClosure) {
        boolean parallel = false
        int cores = Runtime.getRuntime().availableProcessors()
        if( cores > 2 && ! options.singleThread ) {
            if( ! options.quiet ) {
                System.err.println ("Found ${cores} cores, using parallel threads")
            }
            parallel = true
        }
        
        def outputStream = ioFiles.getOutputStream(options)
                    
        initOutput(outputStream, options)

		long tm1 = System.currentTimeMillis()
		if( parallel ) {
			processFileParallel(ioFiles, options, processClosure, (int)(cores), resultClosure)
		}
		else {
			processFile(ioFiles, options, processClosure, resultClosure)
		}

		if( ! options.quiet ) {
			long tm2 = System.currentTimeMillis()
			System.err.println "Time: ${(tm2-tm1)} ms"
		}
        
        closeOutput(outputStream, options)
    }

    private static void initOutput(PrintStream outputStream, OptionsBase options) {
        if( options.noTag )
            return
        
        if( options.outputFormat == OutputFormat.xml ) {
            outputStream.println('<?xml version="1.0" encoding="UTF-8"?>')
            if( options.xmlSchema ) {
                outputStream.println("<text xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                        + "\n xsi:noNamespaceSchemaLocation=\"${options.xmlSchema}\">\n")
            }
            else {
                outputStream.println('<text>\n')
            }
        }
        else if( options.outputFormat == OutputFormat.json ) {
            outputStream.println('{')
            outputStream.println('  "sentences": [')
        }
    }

    private static void closeOutput(PrintStream outputStream, OptionsBase options) {
        if( options.noTag )
            return
            
        if( options.outputFormat == OutputFormat.xml ) {
            outputStream.println('\n</text>')
        }
        else if( options.outputFormat == OutputFormat.json ) {
            outputStream.println('\n  ]')
            outputStream.println('}')
        }
//            else {
//                outputFile.println('\n')
//            }
    }
    
    @CompileStatic
    static class OutputHandler {
        PrintStream outputStream
        OptionsBase options
        boolean outStarted = false
        boolean jsonStarted = false

        void print(ResultBase analyzed) {
            if( options.noTag )
                return
            
            if( jsonStarted ) {
                outputStream.print(",\n")
            }
            else if( outStarted ){
                outputStream.print("\n")
            }
            outputStream.print(analyzed.tagged)
            if( analyzed.tagged ) {
                outStarted = true
                if( options.outputFormat.name() == 'json'
                        && ! jsonStarted
                        && (analyzed.tagged.endsWith('}') || analyzed.tagged.endsWith('[') ) ) {
                    jsonStarted = true
                }
            }
        }
    }
    

    @CompileStatic
	static <T extends ResultBase> void processFile(IOFiles ioFiles, OptionsBase options, Function<String, T> analyzeFunction, Consumer<T> postProcessClosure) {
        StringBuilder buffer = new StringBuilder(BUFFER_SIZE)
        boolean notEmpty = false
        OutputHandler outputHandler = new OutputHandler(outputStream: ioFiles.getOutputStream(), options: options)

        Stream<String> lines = ioFiles.getLines()
        lines.each{ String line ->
            
//        inputStream.eachLine('UTF-8', 0, { String line ->
            buffer.append(line).append("\n")
            notEmpty |= line.trim().length() > 0

            if( (notEmpty
                    && buffer.lastIndexOf("\n\n") == buffer.length() - 2 )
                    || buffer.length() > MAX_PARAGRAPH_SIZE ) {
                def str = buffer.toString()

				try {
					ResultBase analyzed = analyzeFunction.apply(str)
                    outputHandler.print(analyzed)
					postProcessClosure(analyzed)
				}
				catch(Throwable e) {
					e.printStackTrace()
				}

				buffer = new StringBuilder(BUFFER_SIZE)
                notEmpty = false
            }
        }

        if( buffer ) {
            ResultBase analyzed = analyzeFunction(buffer.toString())
            outputHandler.print(analyzed)
            if( outputHandler.outputStream ) {
                outputHandler.outputStream.println()
            }
            try {
                postProcessClosure(analyzed)
			}
			catch(e) {
			  e.printStackTrace()
			}
        }
        
        if( outputHandler.outputStream ) {
            outputHandler.outputStream.flush()
        }
    }

    @CompileStatic
	static void processFileParallel(IOFiles ioFiles, OptionsBase options, Function<String, ResultBase> processClosure, int cores, Consumer<ResultBase> postProcessClosure) {
		ExecutorService executor = Executors.newFixedThreadPool(cores + 1) 	// +1 for consumer
		BlockingQueue<Future> futures = new ArrayBlockingQueue<>(cores*2)	// we need to poll for futures in order to keep the queue busy
        OutputHandler outputHandler = new OutputHandler(outputStream: ioFiles.getOutputStream(), options: options)
        
		executor.submit(new Callable() {
            def call() {
                for(Future<ResultBase> f = futures.poll(15, TimeUnit.MINUTES); ; f = futures.poll(15, TimeUnit.MINUTES)) {
                    if( f == null ) {
                        continue
                    }
    
    //              println "queue size: " + futures.size()
                    try {
                        ResultBase analyzed = f.get()
                        if( analyzed == null ) break;
                        outputHandler.print(analyzed)
                        postProcessClosure.accept(analyzed)
                    }
                    catch(e) {
                        e.printStackTrace()
                        System.exit(1)
                    }
                }
//          println "done polling"
            }
		})

		StringBuilder buffer = new StringBuilder(BUFFER_SIZE)
		boolean notEmpty = false

        Stream<String> lines = ioFiles.getLines()
        
        lines.each{ String line ->
        
			buffer.append(line).append("\n")
			notEmpty |= line.trim().length() > 0

			if( (notEmpty
//					&& buffer.length() > 1000
					&& buffer.lastIndexOf("\n\n") == buffer.length() - 2 )
					|| buffer.length() > MAX_PARAGRAPH_SIZE ) {
				def str = buffer.toString()

				futures << executor.submit(new Callable<Object>() {
					public def call() {
						return processClosure(str)
					}
				})

				buffer = new StringBuilder(BUFFER_SIZE)
				notEmpty = false
			}
		}

		futures << executor.submit {return null} as Callable
		executor.shutdown()
		executor.awaitTermination(1, TimeUnit.HOURS)

		if( buffer ) {
			def analyzed = processClosure(buffer.toString())
            outputHandler.print(analyzed)
            if( outputHandler.outputStream ) {
                outputHandler.outputStream.println()
            }
			postProcessClosure(analyzed)
		}

	}
	

    static void warnOnWindows() {
        // windows have non-unicode encoding set by default
        String osName = System.getProperty("os.name").toLowerCase()
        if ( osName.contains("windows")) {
            if( ! "UTF-8".equals(System.getProperty("file.encoding"))
                    || ! StandardCharsets.UTF_8.equals(java.nio.charset.Charset.defaultCharset()) ) {
                System.setOut(new PrintStream(System.out,true,"UTF-8"))
        
                println "file.encoding: " + System.getProperty("file.encoding")
                println "defaultCharset: " + java.nio.charset.Charset.defaultCharset()
                
                println "Input/output charset: " + java.nio.charset.Charset.defaultCharset()
                println "On Windows to get unicode handled correctly you need to set environment variable before running expand:"
//                println "\tbash (recommended):"
//                println "\t\texport JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8"
                println "\tPowerShell:"
//                println "\t\t(change Font to 'Lucida Console' in cmd window properties)"
                println "\t\tchcp 65001"
//                println "\t\tset JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8"
            }
        }
    }

    // common helpers
    
    static List<String> notParts = ['себ-то', 'цеб-то', 'як-от', 'ф-но', 'то-то', // 'все-таки', 'усе-таки', 
        'тим-то', 'аби-то', 'єй-бо', 'їй-бо', 'от-от', 'от-от-от', 'ото-то']
    
    // TODO: disambig
    /*
        отож-то conj:subord - розбиваємо й відразу присвоюємо отож conj (без варіантів)
        отож-бо conj:subord - розбиваємо й  присвоюємо отож conj (без варіантів)
        так-таки part - розбиваємо й присвоюємо лише так part
        так-то part|adv - розбиваємо й тоді так може бути adv|(рідше)part
        ото-то intj - треба розбивати на ото part + то part (ото-то разом - це теж part)
        тому-то conj:subord - розбиваємо лише на тільки conj:subord + то part
     */
    
    public static Pattern WITH_PARTS = ~/(?iu)([а-яіїєґ][а-яіїєґ'\u2019\u02bc-]+)[-\u2013](бо|но|то|от|таки)$/
    
    static List<String> adjustTokens(List<String> words, boolean withHyphen) {
        List<String> newWords = []
        String hyph = withHyphen ? "-" : ""
        
        words.forEach { String word ->
            String lWord = word.toLowerCase().replace('\u2013', '-')
            if( lWord.contains('-') && ! (lWord in notParts) ) {
                def matcher = WITH_PARTS.matcher(word)

                if( matcher ) {
                    newWords << matcher.group(1) << hyph + matcher.group(2)
                    return
                }
            }

            newWords << word
        }
        
        return newWords
    }
    
    static List<String> splitWithPart(String word) {
        if( word.indexOf('-') == -1 && word.indexOf('\u2013') == -1 )
            return null
        
        String lWord = word.toLowerCase().replace('\u2013', '-')
        if( ! (lWord in notParts) ) {
            def matcher = WITH_PARTS.matcher(word)
            if( matcher ) {
                return [matcher.group(1), "-" + matcher.group(2)]
            }
        }

        return null
    }

    @Canonical
    public static class ResultBase {
        public String tagged
        
        ResultBase(String str) {
            tagged = str
        }
    }
    

}
