package org.nlp_uk.tools

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

import groovy.transform.TypeChecked

class TextUtils {
    
    static int MAX_PARAGRAPH_SIZE = 200*1024

    static def processByParagraph(options, Closure closure, Closure resultClosure) {

        if( options.output == "-" || options.input == "-" ) {
            warnOnWindows();
        }

        def outputFile

        if( options.output == "-" ) {
            outputFile = System.out
        }
        else {
            if( ! options.noTag ) {
                outputFile = new File(options.output)
                outputFile.setText('')    // to clear out output file
                outputFile = new PrintStream(outputFile, "UTF-8")
            }
        }

        if( ! options.quiet && options.input == "-" ) {
            System.err.println ("reading from stdin...")
        }

        def inputFile = options.input == "-" ? System.in : new File(options.input)

        if( ! options.quiet ) {
            if( options.noTag ) {
                System.err.println("Collecting stats only...")
            }
            else
            if( options.output != "-" ) {
                System.err.println ("writing into ${options.output}")
            }
        }

        if( options.outputFormat.name() == "xml" ) {
            outputFile.println('<?xml version="1.0" encoding="UTF-8"?>')
            outputFile.println('<text>\n')
        }
        else if( options.outputFormat.name() == 'json' ) {
            outputFile.println('{')
            outputFile.println('  "sentences": [')
        }

		long tm1 = System.currentTimeMillis()
		int cores = Runtime.getRuntime().availableProcessors()
		if( cores > 2 && ! options.singleThread ) {
			System.err.println ("Found ${cores} cores, using parallel threads")
			processFileParallel(inputFile, outputFile, closure, (int)(cores), resultClosure)
		}
		else {
			processFile(inputFile, outputFile, closure, resultClosure)
		}
		if( ! options.quiet ) {
			long tm2 = System.currentTimeMillis()
			System.err.println "Time: " + (tm2-tm1) + " ms"
		}

        if( options.outputFormat.name() == 'xml' ) {
            outputFile.println('\n</text>')
        }
        else if( options.outputFormat.name() == 'json' ) {
            outputFile.println('\n  ]')
            outputFile.println('}')
        }

        return outputFile
    }

    static class OutputHandler {
        PrintStream outputFile
        boolean outStarted = false
        boolean jsonStarted = false

        void print(analyzed) {
            if( jsonStarted ) {
                outputFile.print(",\n")
            }
            else if( outStarted ){
                outputFile.print("\n")
            }
            outputFile.print(analyzed.tagged)
            if( analyzed.tagged.size() > 0 ) {
                outStarted = true
                if( ! jsonStarted && analyzed.tagged.endsWith('}') ) {
                    jsonStarted = true
                }
            }
        }
    }
    

	static void processFile(def inputFile, PrintStream outputFile, Closure closure, Closure resultClosure) {
        StringBuilder buffer = new StringBuilder()
        boolean notEmpty = false
        OutputHandler outputHandler = new OutputHandler(outputFile: outputFile)
        
        inputFile.eachLine('UTF-8', 0, { line ->
            buffer.append(line).append("\n")
            notEmpty |= line.trim().length() > 0

            if( (notEmpty
                    && buffer.lastIndexOf("\n\n") == buffer.length() - 2 )
                    || buffer.length() > MAX_PARAGRAPH_SIZE ) {
                def str = buffer.toString()

				try {
					def analyzed = closure(str)
                    outputHandler.print(analyzed)
					resultClosure(analyzed)
				}
				catch(Throwable e) {
					e.printStackTrace()
				}

				buffer = new StringBuilder()
                notEmpty = false
            }
        })

        if( buffer ) {
            def analyzed = closure(buffer.toString())
            outputHandler.print(analyzed)
            outputHandler.outputFile.println()
			resultClosure(analyzed)
        }
    }

	static void processFileParallel(def inputFile, PrintStream outputFile, Closure closure, int cores, Closure resultClosure) {
		ExecutorService executor = Executors.newFixedThreadPool(cores + 1) 	// +1 for consumer
		BlockingQueue<Future> futures = new ArrayBlockingQueue<>(cores*2)	// we need to poll for futures in order to keep the queue busy
        OutputHandler outputHandler = new OutputHandler(outputFile: outputFile)
        
		executor.submit {
            for(Future f = futures.poll(5, TimeUnit.MINUTES); ; f = futures.poll(5, TimeUnit.MINUTES)) {
                if( f == null ) {
                    continue
                }

//              println "queue size: " + futures.size()
                try {
                    def analyzed = f.get()
                    if( analyzed == null ) break;
                    outputHandler.print(analyzed)
                    resultClosure(analyzed)
                }
                catch(e) {
                    e.printStackTrace()
                    System.exit(1)
                }
            }
//          println "done polling"
		} as Callable

	
		StringBuilder buffer = new StringBuilder()
		boolean notEmpty = false

		inputFile.eachLine('UTF-8', 0, { String line ->
			buffer.append(line).append("\n")
			notEmpty |= line.trim().length() > 0

			if( (notEmpty
//					&& buffer.length() > 1000
					&& buffer.lastIndexOf("\n\n") == buffer.length() - 2 )
					|| buffer.length() > MAX_PARAGRAPH_SIZE ) {
				def str = buffer.toString()

				futures << executor.submit(new Callable<Object>() {
					public def call() {
						return closure(str)
					}
				})

				buffer = new StringBuilder()
				notEmpty = false
			}
		})

		futures << executor.submit {return null} as Callable
		executor.shutdown()
		executor.awaitTermination(1, TimeUnit.HOURS)

		if( buffer ) {
			def analyzed = closure(buffer.toString())
            outputHandler.print(analyzed)
            outputHandler.outputFile.println()
			resultClosure(analyzed)
		}

	}
	

    static void warnOnWindows() {
        // windows have non-unicode encoding set by default
        String osName = System.getProperty("os.name").toLowerCase()
        if ( osName.contains("windows")) {
            if( ! "UTF-8".equals(System.getProperty("file.encoding"))
                    || "UTF-8".equals(java.nio.charset.Charset.defaultCharset()) ) {
                System.setOut(new PrintStream(System.out,true,"UTF-8"))
                
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
    
    static List<String> notParts = ['себ-то', 'накінець-то', 'як-от', 'ф-но', 'все-таки', 'усе-таки', 'то-то',
        'тим-то', 'аби-то', 'єй-бо', 'їй-бо', 'от-от', 'от-от-от']
    
    // TODO: disambig
    /*
        отож-то conj:subord - розбиваємо й відразу присвоюємо отож conj (без варіантів)
        отож-бо conj:subord - розбиваємо й  присвоюємо отож conj (без варіантів)
        так-таки part - розбиваємо й присвоюємо лише так part
        так-то part|adv - розбиваємо й тоді так може бути adv|(рідше)part
        ото-то intj - треба розбивати на ото part + то part (ото-то разом - це теж part)
        тому-то conj:subord - розбиваємо лише на тільки conj:subord + то part
     */
    
    static Pattern WITH_PARTS = ~/(?iu)([а-яіїєґ][а-яіїєґ'\u2019\u02bc-]+)[-\u2013](бо|но|то|от|таки)$/
    
    static List<String> adjustTokens(List<String> words, boolean withHyphen) {
        List<String> newWords = []
        String hyph = withHyphen ? "-" : ""
        
        words.forEach { String word ->
            String lWord = word.toLowerCase().replace('\u2013', '-')
            if( lWord.contains('-') && ! (lWord in notParts) ) {
                def matcher = WITH_PARTS.matcher(word)

                if( matcher ) {
                    newWords << matcher[0][1] << hyph + matcher[0][2]
                    return
                }
            }

            newWords << word
        }
        
        return newWords
    }
    
}
