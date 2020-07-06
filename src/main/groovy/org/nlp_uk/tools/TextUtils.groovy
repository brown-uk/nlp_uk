package org.nlp_uk.tools


class TextUtils {

    static int MAX_PARAGRAPH_SIZE = 200*1024

    static def processByParagraph(options, Closure closure) {

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

        if( options.xmlOutput ) {
            outputFile.println('<?xml version="1.0" encoding="UTF-8"?>')
            outputFile.println('<text>\n')
        }

        def buffer = new StringBuilder()
        inputFile.eachLine('UTF-8', 0, { line ->
            buffer.append(line).append("\n")

            def str = buffer.toString()
            if( str.endsWith("\n\n") && str.trim().length() > 0
                    || buffer.length() > MAX_PARAGRAPH_SIZE ) {

                def analyzed = closure(str)
                outputFile.print(analyzed)

                buffer = new StringBuilder()
            }
        })

        if( buffer ) {
            def analyzed = closure(buffer.toString())
            outputFile.print(analyzed)
        }

        if( options.xmlOutput ) {
            outputFile.println('\n</text>\n')
        }

        return outputFile
    }


    static void warnOnWindows() {
        // windows have non-unicode encoding set by default
        String osName = System.getProperty("os.name").toLowerCase()
        if ( osName.contains("windows")) {
            if( ! "UTF-8".equals(System.getProperty("file.encoding"))
                    || "UTF-8".equals(java.nio.charset.Charset.defaultCharset()) ) {
                println "Input/output charset: " + java.nio.charset.Charset.defaultCharset()
                println "On Windows to get unicode handled correctly you need to set environment variable before running expand:"
                println "\tbash (recommended):"
                println "\t\texport JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8"
                println "\tcmd:"
                println "\t\t(change Font to 'Lucida Console' in cmd window properties)"
                println "\t\tchcp 65001"
                println "\t\tset JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8"
            }
        }
    }

}

new TextUtils()
