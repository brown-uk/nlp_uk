package ua.net.nlp.tools.tag;

import picocli.CommandLine
import picocli.CommandLine.Option
import picocli.CommandLine.ParameterException
import picocli.CommandLine.Parameters


public class TagOptions {
    enum OutputFormat { txt, xml, json }

    @Option(names = ["-i", "--input"], arity="1", description = "Input file. Default: stdin")
    String input
    @Parameters(index = "0", description = "Input files. Default: stdin", arity="0..")
    List<String> inputFiles
    @Option(names = ["-o", "--output"], arity="1", description = "Output file (default: <input file base name> + .tagged.txt/.xml/.json) or stdout if input is stdin")
    String output
    @Option(names = ["-x", "--xmlOutput"], description = "Output in xml format (deprecated: use --outputFormat)")
    boolean xmlOutput
    @Option(names = ["-n", "--outputFormat"], arity="1", description = "Output format: {xml (default), json, txt}", defaultValue = "xml")
    OutputFormat outputFormat

    @Option(names = ["-sh", "--homonymStats"], description = "Collect homohym statistics")
    boolean homonymStats
    @Option(names = ["-su", "--unknownStats"], description = "Collect unknown words statistics")
    boolean unknownStats
    @Option(names = ["-sfu", "--filterUnknown"], description = "Filter out unknown words with non-Ukrainian character combinations")
    boolean filterUnknown
    @Option(names = ["-sf", "--frequencyStats"], description = "Collect word frequency")
    boolean frequencyStats
    @Option(names = ["-sl", "--lemmaStats"], description = "Collect lemma frequency")
    boolean lemmaStats

    @Option(names = ["-e", "--semanticTags"], description = "Add semantic tags")
    boolean semanticTags
    @Option(names = ["-l", "--tokenPerLine"], description = "One token per line (for .txt output only)")
    boolean tokenPerLine
    @Option(names = ["-k", "--noTag"], description = "Do not write tagged text (only perform stats)")
    boolean noTag
    @Option(names = ["--setLemmaForUnknown"], description = "Fill lemma for unknown words (default: empty lemma)")
    boolean setLemmaForUnknown

    @Option(names = ["-t", "--tokenFormat"], description = "Use <token> format (instead of <tokenReading>)")
    boolean tokenFormat
    @Option(names = ["-t1", "--singleTokenOnly"], description = "Print only one token per reading (-g is recommended with this option)")
    boolean singleTokenOnly

    @Option(names = ["-d", "--showDisambigRules"], description = "Show deterministic disambiguation rules applied")
    boolean showDisambigRules
    @Option(names = ["-g", "--disambiguate"], description = "Use statistics for disambiguation")
    boolean disambiguate
    @Option(names = ["-gr", "--disambiguationRate"], description = "Show a disambiguated token ratings")
    boolean showDisambigRate
    @Option(names = ["-gd", "--writeDisambiguationDebug"], description = "Write disambig debug info into a file")
    boolean disambiguationDebug
    @Option(names = ["-u", "--tagUnknown"], description = "Use statistics to tag unknown words")
    boolean tagUnknown

    boolean partsSeparate = true
    
    @Option(names = ["-m", "--module"], arity="1", description = "Alternative spelling module (only 1 at a time), supported modules: [zheleh, lesya]")
    List<String> modules
    
    @Option(names = ["--singleThread"], description = "Always use single thread (default is to use multithreading if > 2 cpus are found)")
    boolean singleThread
    @Option(names = ["-q", "--quiet"], description = "Less output")
    boolean quiet
    @Option(names = ["--timing"], description = "Pring timing information", hidden = true)
    boolean timing
    @Option(names= ["-h", "--help"], usageHelp= true, description= "Show this help message and exit.")
    boolean helpRequested
    @Option(names = ["--download"], description = "Download file with disambiguation statistics and semantic tags (for tagging from CLI only)")
    boolean download

    
//    public enum DisambigModule {
//        frequency,
//        wordEnding,
//        context
//    }
    
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
            tokenFormat = true
        }
        if( showDisambigRate || disambiguationDebug ) {
            disambiguate = true
        }
        if( outputFormat == OutputFormat.txt ) {
            setLemmaForUnknown = true
        }

        if( ! quiet ) {
            println "Output format: " + outputFormat
            if( disambiguate ) {
                println "Disambig: " + disambiguate
            }
        }
    }

}
