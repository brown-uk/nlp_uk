package ua.net.nlp.tools

import groovy.transform.CompileStatic
import picocli.CommandLine.Option

@CompileStatic
public class OptionsBase {
    @Option(names = ["-i", "--input"], arity="1", description = ["Input file. Default: stdin"], defaultValue = "-")
    public String input
    @Option(names = ["-o", "--output"], arity="1", description = ["Output file"])
    public String output
    @Option(names = ["-q", "--quiet"], description = ["Less output"])
    public boolean quiet
    @Option(names= ["-h", "--help"], usageHelp= true, description= "Show this help message and exit.")
    public boolean helpRequested
    @Option(names = ["-n", "--outputFormat"], arity="1", description = "Output format: {xml (default), json, txt, vertical}", defaultValue = "xml")
    public OutputFormat outputFormat = OutputFormat.xml
    public boolean singleThread = true

    @Option(names = ["--splitHypenParts"], description = "If true parts in words like \"якби-то\" etc will be separate tokens.", defaultValue = "true")
    public boolean splitHyphenParts = true
    
    // internal
    public String xmlSchema

    public boolean isNoTag() {
        return false
    }
}
