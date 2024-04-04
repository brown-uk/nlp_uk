package ua.net.nlp.tools.tag;

import groovy.transform.AutoClone
import groovy.transform.CompileStatic

import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import ua.net.nlp.tools.TextUtils.OptionsBase
import ua.net.nlp.tools.TextUtils.OutputFormat

public class TagOptions extends OptionsBase {

    @Parameters(index = "0", description = "Input files. Default: stdin", arity="0..")
    List<String> inputFiles
    @Option(names = ["-r", "--recursive"], description = "Tag all files recursively in the given directories")
    boolean recursive
    @Option(names = ["--list-file"], description = "Read files to tag from the file")
    String listFile

    @Option(names = ["--lemmaOnly"], description = "Prints only lemmas, implies: --outputFormat=txt --disambiguate=true")
    boolean lemmaOnly

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
    @Option(names = ["--separateDotAbbreviation"], description = "Will split the dot from abbreviation tokens into separate token")
    boolean separateDotAbbreviation

    @Option(names = ["-t", "--tokenFormat"], description = "Use <token> format (instead of <tokenReading>)")
    boolean tokenFormat
    @Option(names = ["-t1", "--singleTokenOnly"], description = "Print only one token per reading (-g is recommended with this option)")
    boolean singleTokenOnly

    @Option(names = ["-d", "--showDisambigRules"], description = "Show deterministic disambiguation rules applied")
    boolean showDisambigRules
    @Option(names = ["-g", "--disambiguate"], description = "Use statistics for disambiguation (implies -t1 abd -u)")
    boolean disambiguate
    @Option(names = ["-gr", "--disambiguationRate"], description = "Show a disambiguated token ratings")
    boolean showDisambigRate
    @Option(names = ["-gd", "--writeDisambiguationDebug"], description = "Write disambig debug info into a file")
    boolean disambiguationDebug
    @Option(names = ["-u", "--tagUnknown"], description = "Use statistics to tag unknown words")
    boolean tagUnknown
    @Option(names = ["-ur", "--tagUnknownWithRate"], description = "Use statistics to tag unknown words and print the rate")
    boolean unknownRate

    @Option(names = ["-m", "--module"], arity="1", description = "Alternative spelling module (only 1 at a time), supported modules: [zheleh, lesya]")
    List<String> modules

    @Option(names = ["--singleNewLineAsParagraph"], description = "Split paragraphs by single new line instead of two")
    boolean singleNewLineAsParagraph
    @Option(names = ["--sentencePerLine"], description = "Assume each line is a sentence (don't use internal sentence tokenizer).")
    boolean sentencePerLine

    @Option(names = ["--singleThread"], description = "Always use single thread (default is to use multithreading if > 2 cpus are found)")
    boolean singleThread
    @Option(names = ["--timing"], description = "Pring timing information", hidden = true)
    boolean timing
    @Option(names = ["--download"], description = "Download file with disambiguation statistics and semantic tags (for tagging from CLI only)")
    boolean download
    @Option(names = ["--progress"], description = "Pring progress information every <n> files", hidden = true)
    int progress=0

    enum Module { zheleh, lesya }
    
    void adjust() {
        if( modules ) {
            def allowed = Module.values().collect { m -> m.name() }
            modules.each { module ->
                if( ! (module in allowed) ) {
                    System.err.println("Error: invalid module $module")
                    System.exit(1)
                }
            }
        }
        
        if( lemmaOnly ) {
            outputFormat = OutputFormat.txt
            disambiguate = true
            singleTokenOnly = true
        }

        if( ! outputFormat ) {
            outputFormat = outputFormat.xml
        }
        else if( outputFormat == OutputFormat.txt ) {
            setLemmaForUnknown = true
        }

        if( showDisambigRate || disambiguationDebug ) {
            disambiguate = true
        }
        if( disambiguate ) {
            tokenFormat = true
            singleTokenOnly = true
            tagUnknown = true
        }
        
        if( singleTokenOnly ) {
            tokenFormat = true
        }
        if( unknownRate ) {
            tagUnknown = true
        }

        if( ! quiet ) {
            System.err.println "Output format: " + outputFormat
        }
    }

    boolean isSingleFile() {
      return ! recursive && ! listFile && inputFiles && inputFiles.size() == 1
    }

}
