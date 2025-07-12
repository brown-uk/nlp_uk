package ua.net.nlp.tools.tag

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import ua.net.nlp.tools.tag.TagTextCore.TaggedToken

@CompileStatic
@Canonical
@PackageScope
class TTR {
    List<TaggedToken> tokens
}

