package ua.net.nlp.tools.tokenize

import java.util.regex.Pattern

import groovy.transform.AutoClone
import groovy.transform.PackageScope
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import ua.net.nlp.tools.OptionsBase
import ua.net.nlp.tools.OutputFormat

@PackageScope
@AutoClone
class TokenizeOptions extends OptionsBase {
    @Parameters(index = "0", description = "Input files. Default: stdin", arity="0..")
    public List<String> inputFiles
    @Option(names = ["-r", "--recursive"], description = "Tag all files recursively in the given directories")
    public boolean recursive
    @Option(names = ["--list-file"], description = "Read files to tag from the file")
    public String listFile

    @Option(names = ["-w", "--words"], description = ["Tokenize into words"])
    public boolean words
    @Option(names = ["-u", "--onlyWords"], description = ["Remove non-words (assumes \"-w\")"])
    public boolean onlyWords
    @Option(names = ["--preserveWhitespace"], description = "Preserve whitepsace tokens")
    public boolean preserveWhitespace
    @Option(names = ["-s", "--sentences"], description = "Tokenize into sentences (default)")
    public boolean sentences
    @Option(names = ["--additionalSentenceSeparator"], description = "Additional pattern to split sentences by (regular expression). Note: this separator will be removed from the output.")
    public String additionalSentenceSeparator
    public Pattern additionalSentenceSeparatorPattern
    
    // internal for now
    public String newLine = ' '
    
    public TokenizeOptions() {
        outputFormat = OutputFormat.txt
    }
}
