package ua.net.nlp.tools.stress

import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class StressInfo {
    String word
    String tags
    String comment
    int base
    int offset 
    
    String toString() { word ? String.format("%s %s", word, tags) : String.format("%d %d", base, offset) }
    
    
    static Map<String, Map<String, List<StressInfo>>> loadStressInfo() {
        Map<String, Map<String, List<StressInfo>>> stresses = new HashMap<>()
        
        long tm1 = System.currentTimeMillis()
        
        File base
        def stressDir = new File("data/stress")
        if( stressDir.isDirectory() ) {
            base = stressDir
            System.err.println("Loading stress info from $base")
        }
        else {
            System.err.println("Loading stress info from resource")
        }

        ["all_stress", "all_stress_prop", "add"].each { file ->
            String lines = base 
                ? new File(base, file+".txt").getText("UTF-8") 
                : getClass().getResourceAsStream("/stress/${file}.txt").getText("UTF-8")

            String lastLemmaFull
            String lastLemma
            String lastLemmaTags
            
            lines.eachLine { line ->
                String comment = null
                if( line.indexOf('#') >= 0 ) {
                    def parts = line.split(/\s*#/, 2)
                    line = parts[0]
                    if( parts.length > 1 ) {
                        comment = parts[1].trim()
                    }
                }
                
                String trimmed = line.trim()
                if( ! trimmed )
                    return
                
                // /1/
//                if( trimmed.indexOf(' ') <= 0 && trimmed.startsWith("/") ) {
////                  println "x: " + trimmed + " "  + trimmed.charAt(1) + " " + lastLemmaFull
//                    int offset = trimmed[1] as int
//                    List<Integer> lemmaAccents = Util.getAccentSyllIdxs(lastLemmaFull) ?: [1]
//                    stresses[lastLemma][lastLemmaTags] << new StressInfo(base: lemmaAccents[0], offset: offset, comment: comment)
//                    return
//                }
                    
                assert trimmed.indexOf(' ') > 0, "Failed at $line"
                    
                def orig = null
                def repl = null
                def parts = trimmed.split(' ')
                def word = parts[0]
                def tags = parts[1]
                
                if( Util.getSyllCount(word) > 1 && word.indexOf('\u0301') == -1 ) {
                    System.err.println "Missing stress in: $line"
                }
                
                tags = tags.replaceFirst(/(futr|pres)(:[1-3])/, '$1:s$2') 
                
                if( tags.contains(':/') ) {
                    def m = ~/([a-z0-9_]+):\/([a-z0-9_]+)/
                    def match = m.matcher(tags)
                    match.find()
                    orig = match.group(1)
                    repl = match.group(2)
                }
                
                if( ! line.startsWith(' ') ) {
                    lastLemmaFull = word
                    lastLemma = Util.stripAccent(word)
                    lastLemmaTags = Util.getTagKey(tags)
                    
                    if( word == 'сами́й' ) { // easier to hack special case
                        lastLemma = word
                    }
                }
                
                if( ! (lastLemma in stresses) ) {
                    stresses.put(lastLemma, new HashMap<>())
                }

                if( orig && repl ) {
                    if( ! line.startsWith(' ') ) { // for noun dual gender
                        def tags2 = tags.replace(orig+':/', '')
                        def lastLemmaTags2 = Util.getTagKey(tags2)
                        def info2 = new StressInfo(word: word, tags: tags2, comment: comment)
                        stresses[lastLemma].computeIfAbsent(lastLemmaTags2, k -> []) << info2
                        lastLemmaTags = lastLemmaTags.replace(':/'+repl, '')
                        tags = tags.replace(':/'+repl, '')
//                        println "=== ${lastLemma} ${lastLemmaTags2} ${info2}"
                    }
                    else { // for verb pres/futr
                        def tags2 = tags.replace(orig+':/', '')
                        def info2 = new StressInfo(word: word, tags: tags2, comment: comment)
                        stresses[lastLemma].computeIfAbsent(lastLemmaTags, k -> []) << info2
                        tags = tags.replace(':/'+repl, '')
                    }
                }

                def info = new StressInfo(word: word, tags: tags, comment: comment)
                stresses[lastLemma].computeIfAbsent(lastLemmaTags.replace(':/'+repl, ''), k -> []) << info
            }
        }

        long tm2 = System.currentTimeMillis()
        System.err.println("Loaded ${stresses.size()} stress forms, ${tm2-tm1}ms")
        
        return stresses
    }

} 
