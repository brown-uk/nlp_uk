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
    
    
    @CompileDynamic
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
            def src = base ? new File(base, file+".txt") : getClass().getResourceAsStream("/stress/${file}.txt")
            String lines = src.getText("UTF-8")

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
                if( trimmed.indexOf(' ') <= 0 && trimmed.startsWith("/") ) {
//                  println "x: " + trimmed + " "  + trimmed.charAt(1) + " " + lastLemmaFull
                    int offset = trimmed[1] as int
                    List<Integer> lemmaAccents = Util.getAccentSyllIdxs(lastLemmaFull) ?: [1]
                    stresses[lastLemma][lastLemmaTags] << new StressInfo(base: lemmaAccents[0], offset: offset, comment: comment)
                    return
                }
                    
                assert trimmed.indexOf(' ') > 0, "Failed at $line"
                    
                def (word, tags) = trimmed.split(' ')
                if( ! line.startsWith(' ') ) {
                    lastLemmaFull = word
                    lastLemma = Util.stripAccent(word)
                    lastLemmaTags = Util.getTagKey(tags)
                }
                
                if( ! (lastLemma in stresses) ) {
                    stresses.put(lastLemma, new HashMap<>())
                }
                if( ! (lastLemmaTags in stresses[lastLemma]) ) {
                    stresses[lastLemma].put(lastLemmaTags, [])
                }

//              if( lastLemma == "аналізувати" ) println "$lastLemmaTags / $word + $tags"
                stresses[lastLemma][lastLemmaTags] << new StressInfo(word: word, tags: tags, comment: comment)
            }
        }

        long tm2 = System.currentTimeMillis()
        System.err.println("Loaded ${stresses.size()} stress forms, ${tm2-tm1}ms")
        
        return stresses
    }

} 
