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
        def t0 = text
        
        boolean cont = true
        for(int ii=0; ii<10; ii++) {
            cont = false

            def m1 = t0 =~ /([XVI])([ХІ])/

            if( m1 ) {
                cont = true
// t0 = null // ml
                t0 = m1.replaceAll( new Function<MatchResult, String>() { String apply(MatchResult mr) { // { mr -> // lat, cyr
                    def lat = mr.group(1)
                    def cyr = mr.group(2)
                    counts[1]++
                    lat.concat( cyrToLatMap[cyr] )
                } } )
            }
            
            def m2 = t0 =~ /([ХІ])([XVI])/
            if( m2 ) {
                cont = true
// t0 = null // ml
                t0 = m2.replaceAll( new Function<MatchResult, String>() { String apply(MatchResult mr) { // { mr -> //all, cyr, lat ->
                    counts[1]++
                    cyrToLatMap[mr.group(1)].concat( mr.group(2) )
                } } )
            }
            
        }
        t0
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
// t1 = null // ml
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
        def t1 = m1.replaceAll( new Function<MatchResult, String>() { String apply(MatchResult mr) {
            def lat = mr.group(1)
            def cyr = mr.group(2)
            out.debug "mix: 1.3"
            counts[1] += 2
            assert cyrToLatMap[cyr]
            lat.concat(cyrToLatMap[cyr])
        } } )

//        def t2 = t1.replaceAll(/([асеіорхуАВСЕНІКМНОРТХУ])(['’ʼ]?[bdfghjklmnrstuvwzDFGJLNQRSUVWZ])/, { all, cyr, lat ->
        def m2 = t1 =~ /([асеіорхуАВСЕНІКМНОРТХУ])(['’ʼ]?[bdfghjklmnrstuvwzDFGJLNQRSUVWZ])/
// t1 = null // ml
        m2.replaceAll( new Function<MatchResult, String>() { String apply(MatchResult mr) {
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

        def t1 = text.replaceAll(/([а-яіїєґА-ЯІЇЄҐ]['’ʼ]?)([aceiopxyABCEHIKMHOPTXYáÁéÉíÍḯḮóÓúýÝ])(['’ʼ]?[а-яіїєґА-ЯІЇЄҐ])/, { all, cyr, lat, cyr2 ->
            counts[0] += 1
            cyr + latToCyrMap[lat] + cyr2
        })

        // Cyrillic letter that looks like Latin between 2 Latin

        t1.replaceAll(/([a-zA-Z]['’ʼ]?)([асеіорхуАВСЕНІКМНОРТХУ])(['’ʼ]?[a-zA-Z])/, { all, lat, cyr, lat2 ->
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
        def t1 = fixLatinDigits(text, counts)
        // 1st tier

        def t2 = fixReliableCyr(t1, counts)
// t1 = null // ml
        def t3 = fixReliableLat(t2, counts)
// t2 = null // ml
        
        // 2nd tier

        def t4 = fixToAllCyrillic(t3, counts)
// t3 = null // ml
        
        // 3nd tier - least reliable

        def t5 = fixCharBetweenOthers(t4, counts)
// t4 = null // ml
        out.println "\tconverted ${counts[0]} lat->cyr, ${counts[1]} cyr->lat"

        return t5
    }


    static final Pattern MIX_1 = ~ /[а-яіїєґА-ЯІЇЄҐ][a-zA-Zóáíýúé]|[a-zA-Zóáíýúé][а-яіїєґА-ЯІЇЄҐ]/
    
    @CompileStatic
    String fixCyrLatMix(String text) {
        // фото зhttp://www
        def t0 = text.replaceAll(/(?iu)([а-яіїєґ])(http)/, '$1 $2')
        
        
        // CO/CO2 with cyr/lat mix
        t0 = t0.replaceAll(/\b(СO|CО)(2?)\b/, 'CO$2')
        // CO2 with cyr
        t0 = t0.replaceAll(/\bСО2\b/, 'CO2')
        // degree Celcius with cyr
        t0 = t0.replaceAll(/\b[\u00B0\u00BA][СC]\b/, '\u00B0C')

        
        if( MIX_1.matcher(t0).find() ) {
            KNOWN_MIXES.each { String k, String v ->
                text = text.replace(k, v)
            }

            if( MIX_1.matcher(t0).find() ) {
                out.println "\tlatin/cyrillic mix"

                def t1 = removeMix(t0)
// t0 = null // ml

                if( MIX_1.matcher(t1).find() ) {
                    out.println "\tWARNING: still Latin/Cyrillic mix"
                }
                t0 = t1
// t1 = null // ml
            }

            KNOWN_MIXES.each { String k, String v ->
                text = text.replace(v, k)
            }
        }

        // Latin a, o, i, and y
        def t1 = t0.replaceAll(/([^a-z])[,;–—-] a ([А-ЯІЇЄҐа-яіїєґ])/, '$1, а $2')
// t0 = null // ml
        def t2 = t1.replaceAll(/([^a-z]) i ([А-ЯІЇЄҐа-яіїєґ])/, '$1 і $2')
// t1 = null // ml
        def t3 = t2.replaceAll(/([^a-z]) o ([А-ЯІЇЄҐа-яіїєґ])/, '$1 о $2')
// t2 = null // ml
        def t4 = t3.replaceAll(/([^a-z]) y ([А-ЯІЇЄҐа-яіїєґ])/, '$1 у $2')
// t3 = null // ml
        
        return t4
    }
}
