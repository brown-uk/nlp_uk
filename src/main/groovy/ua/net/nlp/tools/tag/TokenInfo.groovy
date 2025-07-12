package ua.net.nlp.tools.tag

import org.languagetool.AnalyzedTokenReadings

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@CompileStatic
@PackageScope
class TokenInfo {
    String cleanToken
    String cleanToken2
    AnalyzedTokenReadings[] tokens
    int idx
    List<TTR> taggedTokens
}
