#!/bin/env groovy

package ua.net.nlp.other

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

@GrabConfig(systemClassLoader=true)
@Grab(group='org.languagetool', module='language-uk', version='6.1')
@Grab(group='org.languagetool', module='language-ru', version='6.1')
@Grab(group='ch.qos.logback', module='logback-classic', version='1.4.+')
@Grab(group='info.picocli', module='picocli', version='4.6.+')

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Function
import java.util.regex.MatchResult
import java.util.regex.Matcher
import java.util.regex.Pattern
import picocli.CommandLine
import picocli.CommandLine.Option
import picocli.CommandLine.ParameterException
import groovy.io.FileVisitResult
import groovy.transform.CompileStatic

import org.languagetool.tagging.uk.*
import org.languagetool.tokenizers.SRXSentenceTokenizer
import org.languagetool.tokenizers.uk.UkrainianWordTokenizer
import org.slf4j.Logger
import org.languagetool.tagging.Tagger
import org.languagetool.tagging.ru.RussianTagger
import org.languagetool.AnalyzedToken
import org.languagetool.language.Ukrainian
//import ua.net.nlp.other.CleanTextNanu


@CompileStatic
class CleanText {

    @groovy.transform.SourceURI
    static URI SOURCE_URI
    static String SCRIPT_DIR=new File(SOURCE_URI).parent

    static void main(String[] args) {
//        warnForEncoding()
        
        long tm1 = System.currentTimeMillis()
        
        def cl = new GroovyClassLoader()
        cl.addClasspath(SCRIPT_DIR + "/../../../../")

        def resourceDir = SCRIPT_DIR + "/../../../../../resources"
        if( ! new File(resourceDir).isDirectory() ) {
//            println "making missing dir: $resourceDir"
            new File(resourceDir).mkdirs()
        }
        cl.addClasspath(resourceDir)
        
        def basePkg = CleanText.class.getPackageName()
        def tagTextClass = cl.loadClass("${basePkg}.clean.CleanTextCore")
        def m = tagTextClass.getMethod("main", String[].class)
        def mArgs = [args].toArray() // new Object[]{args} - Eclips chokes on this

        long tm2 = System.currentTimeMillis()

        if( "--timing" in args ) {        
            System.err.println("Loaded classes in ${tm2-tm1} ms")
        }
        m.invoke(null, mArgs)
    }
}
