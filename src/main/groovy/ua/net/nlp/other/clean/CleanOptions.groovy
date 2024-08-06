package ua.net.nlp.other.clean

import groovy.transform.CompileStatic
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters

@CompileStatic
public class CleanOptions {
    @Parameters(index = "0", description = "Directory to process. Default: current directory", arity="0..1")
    List<String> inputDirs
    @Option(names = ["-i", "--input"], arity="1", description = ["Input file"])
    String input
    @Option(names = ["-o", "--output"], arity="1", description = ["Output file ((default: input file/dir with \"-good\" added)"])
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
    @Option(names = ["--exclude-file"], description = ["Don't clean files listed in this file (just copy them)"])
    String excludeFromFile
    @Option(names = ["-d", "--debug"], description = ["Debug output"])
    boolean debug
    @Option(names = ["-z", "--markLanguages"], description = ["Mark text in another language, modes: none, mark, cut (supported language: Russian)"], defaultValue="none")
    MarkOption markLanguages = MarkOption.none
    @Option(names = ["--paragraph"], description = ["Tells if to split paragraph by single or double new line (for marking other languages)"], defaultValue = "double_nl")
    ParagraphDelimiter paragraphDelimiter = ParagraphDelimiter.double_nl
    @Option(names = ["-p", "--parallel"], description = ["Process files in parallel"])
    boolean parallel
//    @Option(names = ["-m", "--modules"], description = ["Extra cleanup: remove footnotes, page numbers etc. (supported modules: nanu)"])
    List<String> modules
    @Option(names = ["-x", "--disable-rules"], description = ["Rules to disable (supported: oi)"])
    List<String> disabledRules = []
    //        @Option(names = ["--singleThread"], description = ["Always use single thread (default is to use multithreading if > 2 cpus are found)"])
    //        boolean singleThread
    @Option(names = ["--simple"], description = ["Simple pass"], hidden = true)
    boolean simple
    @Option(names = ["-q", "--quiet"], description = ["Less output"])
    boolean quiet
    @Option(names= ["-h", "--help"], usageHelp= true, description= "Show this help message and exit.")
    boolean helpRequested

    List<String> arguments() { [] }
    
        
    public enum MarkOption {
        none, mark, cut
    }

    public enum ParagraphDelimiter {
        double_nl,
        single_nl,
        auto
    }

}
