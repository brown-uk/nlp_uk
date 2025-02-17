package ua.net.nlp.tools.tokenize

import groovy.transform.AutoClone
import groovy.transform.PackageScope
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import ua.net.nlp.tools.TextUtils.OptionsBase

@PackageScope
@AutoClone
class TokenizeOptions extends OptionsBase {
    @Parameters(index = "0", description = "Input files. Default: stdin", arity="0..")
    List<String> inputFiles
    @Option(names = ["-r", "--recursive"], description = "Tag all files recursively in the given directories")
    boolean recursive
    @Option(names = ["--list-file"], description = "Read files to tag from the file")
    String listFile

    @Option(names = ["-w", "--words"], description = ["Tokenize into words"])
    boolean words
    @Option(names = ["-u", "--onlyWords"], description = ["Remove non-words (assumes \"-w\")"])
    boolean onlyWords
    @Option(names = ["--preserveWhitespace"], description = "Preserve whitepsace tokens")
    boolean preserveWhitespace
    @Option(names = ["-s", "--sentences"], description = "Tokenize into sentences (default)")
    boolean sentences

    // internal for now
    String newLine = ' '
    
    TokenizeOptions() {
        outputFormat = OutputFormat.txt
    }
}
