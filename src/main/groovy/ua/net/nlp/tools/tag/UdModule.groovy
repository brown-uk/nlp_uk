package ua.net.nlp.tools.tag

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.languagetool.AnalyzedToken
import org.languagetool.AnalyzedTokenReadings
import org.languagetool.JLanguageTool
import org.languagetool.language.Ukrainian
import ua.net.nlp.tools.tag.TagTextCore.TTR
import ua.net.nlp.tools.tag.TagTextCore.TaggedSentence
import ua.net.nlp.tools.tag.TagTextCore.TaggedToken
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import java.util.regex.Pattern

@CompileStatic
@PackageScope
class UdModule {
    private Map<String, String> VESUM_TO_UD = [:]
    private static final Pattern PLURAL_PATTERN = ~ /noun.*:p:(?!.*:(ns|&pron|nv:abbr))/
    private static final Pattern PLURAL_GENDER_PATTERN = ~ /noun.*:([mfn])(?!.*:(ns|&pron)).*/
    
    Ukrainian language    
    TagOptions options
    
    
    void printSentence(TaggedSentence taggedSent, StringBuilder sb, int sentId) {
        if( sb.length() > 0 ) {
            sb.append("\n")
        }
        sb.append("# sent_id = $sentId\n")
        sb.append("# text = ${taggedSent.text}\n")
        
        taggedSent.tokens.eachWithIndex { TTR token, int idx ->
            sb.append("${idx+1}\t")
            
            def tkn = token.tokens[0]
            
            List<String> tagParts = tkn.tags
                .replace('conj:', 'conj+')
                .replace('noninfl:&predic', 'noninfl+&predic')
                .split(/:/) as List
            
            tagParts.removeIf{ it ==~ /xp[0-9]|&pron|&predic|&insert/ }
            
            def pos = tagParts[0]
            tagParts.remove(0)
            
            def udPos = VESUM_TO_UD[ pos.replace('+', ':') ]
            if( ! udPos ) {
                System.err.println "not found UD tags for $pos"
                System.exit(1)
            }
            
            def udPosParts = udPos.split(/; /)
            
            udPos = udPosParts[0]
            udPos = udPos.replace('Upos=', '')
            
            List<String> udTags = tagParts.collect { vesumTag ->
                if( vesumTag == 'obsc' ) vesumTag = 'vulg' 
                
                def ud = VESUM_TO_UD[vesumTag]
                if( ! ud ) {
                    System.err.println "not found UD tags for $vesumTag"
                    System.exit(1)
                }
                
                ud = ud.replaceFirst(/Gender=(Masc|Fem|Neut)/, '$0; Number=Sing')
            }
            .findAll { it != '-' }
            
            if( udPosParts.size() > 1 ) {
                udTags += udPosParts[1..-1]
            }

            addPluralGender(tkn, udTags)            

            String udTagsStr
            if( udTags ) {
                udTagsStr = udTags.collect {
                    it.split(/; |\|/)
                } .flatten()
                .sort()
                .join('|')
            }
            else {
                udTagsStr = '_'
            }
            
                
            def misc = []
            
            if( tkn.semtags ) {
                misc << "${tkn.semtags}"
            }
            if( idx < taggedSent.tokens.size()-1 ) {
                def whiteBeforeNext = taggedSent.tokens[idx+1].tokens[0].isWhitepaceBefore()
                if( whiteBeforeNext != null && ! whiteBeforeNext ) {
                    misc << "SpaceAfter=No"
                }
            }
            
            def miscStr = misc ? misc.join("|") : "_"
            
            sb.append("${tkn.value}\t${tkn.lemma}\t${udPos}\t${tkn.tags}\t${udTagsStr}\t_\t_\t${miscStr}")
            
            sb.append('\n')
        }

    }

    void addPluralGender(TaggedToken tkn, List<String> udTags) {
        // try to guess gender for plural
        if( PLURAL_PATTERN.matcher(tkn.tags).find() ) {
            
            if( tkn.lemma.startsWith("пів") && tkn.tags.contains("nv:ua_1992") )
                return
            
            def newTag = tkn.tags.replace(':subst', '')
                .replaceFirst(/:p:v_.../, ':[mfn]:v_naz(:&predic|:&insert)?')

            try {
                String[] singTokens = language.getSynthesizer().synthesize(new AnalyzedToken(tkn.value, tkn.tags, tkn.lemma), newTag, true)
                
                if( ! singTokens ) {
                    System.err.println("Failed to find gender for plural: $tkn")
                    return
                }
                
                def singTags = singTokens.collect {
                    List<AnalyzedTokenReadings> wd = language.getTagger().tag([it])
                    def postag = wd[0].getReadings()[0].getPOSTag()
                    PLURAL_GENDER_PATTERN.matcher(postag).replaceFirst('$1')
                }
                
                if( singTags.contains('m') ) {
                    udTags << "Gender=Masc"
                }
                else if( singTags.contains('f') ) {
                    udTags << "Gender=Fem"
                }
                else if( singTags.contains('n') ) {
                    udTags << "Gender=Neut"
                }
            }
            catch(e) {
                System.err.println("Failed to find gender for plural: $tkn, $e")
            }
        }

    }
    
    /*
    ID: Word index, integer starting at 1 for each new sentence; may be a range for multiword tokens; may be a decimal number for empty nodes (decimal numbers can be lower than 1 but must be greater than 0).
    FORM: Word form or punctuation symbol.
    LEMMA: Lemma or stem of word form.
    UPOS: Universal part-of-speech tag.
    XPOS: Optional language-specific (or treebank-specific) part-of-speech / morphological tag; underscore if not available.
    FEATS: List of morphological features from the universal feature inventory or from a defined language-specific extension; underscore if not available.
    HEAD: Head of the current word, which is either a value of ID or zero (0).
    DEPREL: Universal dependency relation to the HEAD (root iff HEAD = 0) or a defined language-specific subtype of one.
    DEPS: Enhanced dependency graph in the form of a list of head-deprel pairs.
    MISC: Any other annotation.
     */

    void init() {
        loadUDConversions()
    }
    
    void loadUDConversions() {
        def inR = new FileReader(new File(getClass().getResource('/ua/net/nlp/tools/ud/vesum-ud.csv').toURI()))
        Iterable<CSVRecord> records = CSVFormat.EXCEL.builder()
            .setSkipHeaderRecord(true)
            .build()
            .parse(inR)

        for (CSVRecord record : records) {
            String ud = record.get(0);
            String vesum = record.get(1);
            
            if( ! vesum /*|| ud == '-'*/ )
                continue

            vesum = vesum.replaceFirst(/v_zna:/, '').replaceFirst(/^adjp/, '&adjp')
                
            VESUM_TO_UD[vesum]=ud
        }
        
        println "Got ${VESUM_TO_UD.size()} UD conversions"
    }
}
