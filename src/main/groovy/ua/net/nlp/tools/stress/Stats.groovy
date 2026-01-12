package ua.net.nlp.tools.stress

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@CompileStatic
@PackageScope
class Stats {
    static class StatsCnt {
        int cnt = 0
        Set<String> tags = [] as Set
    }
  
    Map<String, StatsCnt> unknown = [:].withDefault{ new StatsCnt() } 
    Map<String, Integer> homonyms = [:].withDefault { 0 }
    
    void add(Stats stats) { 
        stats.unknown.each { k,v -> this.unknown[k].cnt += v.cnt; this.unknown[k].tags += v.tags }
        stats.homonyms.each { k,v -> this.homonyms[k] += v }
    }
    
    void addUnknown(String token, List<String> tags) {
        if( tags ) {
            if( ! tags.find{ it =~ /:prop|:abbr/ } ) {
                token = token.toLowerCase()
            }
            unknown[token].tags += tags
        }
        unknown[token].cnt += 1
    }
    
    void addHomonyms(String joined, List<String> tags) {
        if( tags ) {
            if( ! tags.find{ it =~ /:prop|:abbr/ } ) {
                joined = joined.toLowerCase()
            }
        }
        homonyms[joined] += 1
    }
    
    void collectStats(String filename) {
        File out = new File("${filename}.stress.unknown.txt")
        out.text = ''
        
        println "Unkowns: ${unknown.size()}"
        
//        stats.unknown.findAll { k,v -> ! v }.each { k,v -> println ":: $k $v" }
        
        unknown.toSorted { it -> -it.value.cnt * 1000 + it.key.charAt(0) as int }.each{ k, v ->
            out << "$k ${v.cnt}\t\t\t${v.tags}\n"
        }

        println "Omographs: ${homonyms.size()}"
        
        out << "\n"
        homonyms.toSorted { it -> -it.value * 1000 + it.key.charAt(0) as int }.each{ k, v ->
            out << "$k $v\n"
        }
    }

}
