package ua.net.nlp.other.clean

import java.util.function.Function
import java.util.regex.MatchResult
import java.util.regex.Pattern

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@PackageScope
class LatCyrModule {
    Map<String, String> KNOWN_MIXES =
    [
        "ТаблоID": "Табло@@ID",
        "Фirtka": "Ф@@irtka"
    ]

    Map<String, String> latToCyrMap = [
        'a' : 'а',
        'c' : 'с',
        'e' : 'е',
        'i' : 'і',
        'o' : 'о',
        'p' : 'р',
        'x' : 'х',
        'y' : 'у',
        'A' : 'А',
        'B' : 'В',
        'C' : 'С',
        'E' : 'Е',
        'H' : 'Н',
        'I' : 'І',
        'K' : 'К',
        'M' : 'М',
        'O' : 'О',
        'P' : 'Р',
        'T' : 'Т',
        'X' : 'Х',
        'Y' : 'У',
        "á" : "а́",
        "Á" : "А́",
        "é" : "е́",
        "É" : "Е́",
        "í" : "і́",
        "Í" : "І́",
        "ḯ" : "ї́",
        "Ḯ" : "Ї́",
        "ó" : "о́",
        "Ó" : "О́",
        "ú" : "и́",
        "ý" : "у́",
        "Ý" : "У́"
    ]

    Map<String,String> cyrToLatMap = [:]
    OutputTrait out
    LtModule ltModule
    
    LatCyrModule() {
        latToCyrMap.each{ String k, String v -> cyrToLatMap[v] = k }
    }
        
    @CompileStatic
    String fixLatinDigits(String text, int[] counts) {
        def m1 = text =~ /([XVI])([ХІ])/
        def m2 = text =~ /([ХІ])([XVI])/
        
        while( m1 || m2 ) {
            if( m1 ) {
                def t1 = m1.replaceAll( new Function<MatchResult, String>() { String apply(MatchResult mr) { // { mr -> // lat, cyr
                    def lat = mr.group(1)
                    def cyr = mr.group(2)
                    counts[1]++
                    lat.concat( cyrToLatMap[cyr] )
                } } )
                text = t1
            }
            if( m2 ) {
                def t2 = m2.replaceAll( new Function<MatchResult, String>() { String apply(MatchResult mr) { // { mr -> //all, cyr, lat ->
                    counts[1]++
                    cyrToLatMap[mr.group(1)].concat( mr.group(2) )
                } } )
                text = t2
            }
            
            m1 = text =~ /([XVI])([ХІ])/
            m2 = text =~ /([ХІ])([XVI])/
        }
        text
    }

    @CompileStatic
    String fixReliableCyr(String text, int[] counts) {
        // exclusively cyrillic letter followed by latin looking like cyrillic
//        def t1 = text.replaceAll(/([бвгґдєжзийклмнптфцчшщьюяБГҐДЄЖЗИЙЛПФХЦЧШЩЬЮЯ]['’ʼ]?)([aceiopxyABCEHIKMOPTXYáÁéÉíÍḯḮóÓúýÝ])/, { all, cyr, lat ->
        def m1 = text =~ /([бвгґдєжзийклмнптфцчшщьюяБГҐДЄЖЗИЙЛПФХЦЧШЩЬЮЯ]['’ʼ]?)([aceiopxyABCEHIKMOPTXYáÁéÉíÍḯḮóÓúýÝ])/
        def t1 = m1.replaceAll( new Function<MatchResult, String>() { String apply(MatchResult mr) { // { mr -> // all, cyr, lat
            def cyr = mr.group(1)
            def lat = mr.group(2)
            out.debug "mix: 1.1"
            counts[0] += 1
            cyr.concat(latToCyrMap[lat])
        } } )

        // exclusively cyrillic letter preceeded by latin looking like cyrillic

//        text.replaceAll(/([aceiopxyABCEHIKMOPTXYáÁéÉíÍḯḮóÓúýÝ])(['’ʼ]?[бвгґдєжзийклмнптфцчшщьюяБГҐДЄЖЗИЙЛПФХЦЧШЩЬЮЯ])/, { all, lat, cyr ->
        def m2 = t1 =~ /([aceiopxyABCEHIKMOPTXYáÁéÉíÍḯḮóÓúýÝ])(['’ʼ]?[бвгґдєжзийклмнптфцчшщьюяБГҐДЄЖЗИЙЛПФХЦЧШЩЬЮЯ])/
        def t2 = m2.replaceAll( new Function<MatchResult, String>() { String apply(MatchResult mr) { // { mr -> // lat, cyr
            def lat = mr.group(1)
            def cyr = mr.group(2)
            out.debug "mix: 1.2"
            counts[0] += 1
            assert cyr
            latToCyrMap[lat].concat(cyr)
        } } )
    }

    @CompileStatic
    String fixReliableLat(String text, int[] counts) {
        
//        def t1 = text.replaceAll(/([bdfghjklmnrstuvwzDFGJLNQRSUVWZ]['’ʼ]?)([асеіорхуАВСЕНІКМНОРТХУ])/, { all, lat, cyr ->
        def m1 = text =~ /([bdfghjklmnrstuvwzDFGJLNQRSUVWZ]['’ʼ]?)([асеіорхуАВСЕНІКМНОРТХУ])/
        text = m1.replaceAll( new Function<MatchResult, String>() { String apply(MatchResult mr) {
            def lat = mr.group(1)
            def cyr = mr.group(2)
            out.debug "mix: 1.3"
            counts[1] += 2
            assert cyrToLatMap[cyr]
            lat.concat(cyrToLatMap[cyr])
        } } )

//        def t2 = t1.replaceAll(/([асеіорхуАВСЕНІКМНОРТХУ])(['’ʼ]?[bdfghjklmnrstuvwzDFGJLNQRSUVWZ])/, { all, cyr, lat ->
        def m2 = text =~ /([асеіорхуАВСЕНІКМНОРТХУ])(['’ʼ]?[bdfghjklmnrstuvwzDFGJLNQRSUVWZ])/
        text = m2.replaceAll( new Function<MatchResult, String>() { String apply(MatchResult mr) {
            def cyr = mr.group(1)
            def lat = mr.group(2)
            out.debug "mix: 1.4"
            counts[1] += 2
            assert lat
            cyrToLatMap[cyr].concat(lat)
        } } )
    }
    
    @CompileStatic
    String fixCharBetweenOthers(String text, int[] counts) {
        // latin letter that looks like Cyrillic between 2 Cyrillics

        text = text.replaceAll(/([а-яіїєґА-ЯІЇЄҐ]['’ʼ]?)([aceiopxyABCEHIKMHOPTXYáÁéÉíÍḯḮóÓúýÝ])(['’ʼ]?[а-яіїєґА-ЯІЇЄҐ])/, { all, cyr, lat, cyr2 ->
            counts[0] += 1
            cyr + latToCyrMap[lat] + cyr2
        })

        // Cyrillic letter that looks like Latin between 2 Latin

        text = text.replaceAll(/([a-zA-Z]['’ʼ]?)([асеіорхуАВСЕНІКМНОРТХУ])(['’ʼ]?[a-zA-Z])/, { all, lat, cyr, lat2 ->
            counts[1] += 2
            lat + cyrToLatMap[cyr] + lat2
        })
    }
    
    @CompileStatic
    String fixToAllCyrillic(String text, int[] counts) {
        // 2nd tier - try all Cyrillic
        // if we convert all Latin to Cyrillic and find it in the dictionary use conversion

        text.replaceAll(/[а-яіїєґА-ЯІЇЄҐ'ʼ’a-zA-ZáÁéÉíÍḯḮóÓúýÝ-]+/, { String it ->

            if( it =~ /[а-яіїєґА-ЯІЇЄҐ]['’ʼ]?[aceiopxyABCEHIKMHOPTXYáÁéÉíÍḯḮóÓúýÝ]/
                    || it =~ /[aceiopxyABCEHIKMHOPTXYáÁéÉíÍḯḮóÓúýÝ]['’ʼ]?[а-яіїєґА-ЯІЇЄҐ]/ ) {
                //            println "Found mix in: $it, known to LT: " + knownWord(it)
                if( ! ltModule.knownWord(it) ) {
                    def fixed = it.replaceAll(/[aceiopxyABCEHIKMHOPTXYáÁéÉíÍḯḮóÓúýÝ]/, { String lat -> latToCyrMap[lat] })
                    def fixedCleaned = fixed.replace('\u0301', '')
                    //                println "\tfixed $fixed known to LT: " + knownWord(fixedCleaned)
                    if( ltModule.knownWord(fixedCleaned) ) {
                        counts[0] += 1
                        return fixed
                    }
                }
            }
            return it
        })
    }
        
    @CompileStatic
    String removeMix(String text) {
        int[] counts = [0, 0]

        // latin digits
        text = fixLatinDigits(text, counts)
        // 1st tier

        text = fixReliableCyr(text, counts)
        text = fixReliableLat(text, counts)
        
        // 2nd tier

        text = fixToAllCyrillic(text, counts)

        // 3nd tier - least reliable

        text = fixCharBetweenOthers(text, counts)
        
        out.println "\tconverted ${counts[0]} lat->cyr, ${counts[1]} cyr->lat"

        return text
    }


    static final Pattern MIX_1 = ~ /[а-яіїєґА-ЯІЇЄҐ][a-zA-Zóáíýúé]|[a-zA-Zóáíýúé][а-яіїєґА-ЯІЇЄҐ]/
    
    @CompileStatic
    String fixCyrLatMix(String text, File file) {
        // фото зhttp://www
        text = text.replaceAll(/(?iu)([а-яіїєґ])(http)/, '$1 $2')
        
        if( MIX_1.matcher(text).find() ) {
            KNOWN_MIXES.each { String k, String v ->
                text = text.replace(k, v)
            }

            if( MIX_1.matcher(text).find() ) {
                out.println "\tlatin/cyrillic mix"

                text = removeMix(text)

                if( MIX_1.matcher(text).find() ) {
                    out.println "\tWARNING: still Latin/Cyrillic mix"
                }
            }

            KNOWN_MIXES.each { String k, String v ->
                text = text.replace(v, k)
            }
        }

        // Latin a, o, i, and y
        text = text.replaceAll(/([^a-z])[,;–—-] a ([А-ЯІЇЄҐа-яіїєґ])/, '$1, а $2')
        text = text.replaceAll(/([^a-z]) i ([А-ЯІЇЄҐа-яіїєґ])/, '$1 і $2')
        text = text.replaceAll(/([^a-z]) o ([А-ЯІЇЄҐа-яіїєґ])/, '$1 о $2')
        text = text.replaceAll(/([^a-z]) y ([А-ЯІЇЄҐа-яіїєґ])/, '$1 у $2')
        
        return text
    }
}
