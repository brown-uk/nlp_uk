package ua.net.nlp.other.clean

import java.util.function.Function
import java.util.regex.MatchResult
import java.util.regex.Pattern

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@PackageScope
class LatCyrModule {
    private static final String TEMP_EMPTY = "\uE117"
    
    private static final Map<String, String> KNOWN_MIXES =
    [
        "ТаблоID": "Табло\uE117ID",
        "Фirtka": "Ф\uE117irtka",
        "СхідSide": "Схід\uE117Side",
        "ГолосUA": "Голос\uE117UA",
        "ОsтаNNя": "Оsта\uE117NNя",
        "DepоДніпро": "Depo\uE117Дніпро",
        "DepoДніпро": "Depo\uE117Дніпро"
        // ЧорнобильRenaissance
        // НашSoft
    ]

    private static final Map<String, String> latToCyrMap = [
        'a' : 'а',
        'c' : 'с',
        'e' : 'е',
        'i' : 'і',
        'o' : 'о',
        'p' : 'р',
        'x' : 'х',
        'y' : 'у',
        'r' : 'г',
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

    private static final Map<String,String> cyrToLatMap = [:]
    
    static {
        latToCyrMap.each{ String k, String v -> cyrToLatMap[v] = k }
    }

    OutputTrait out
    LtModule ltModule
    
        
    @CompileStatic
    String fixLatinDigits(String text, int[] counts) {
        def t0 = text

        t0 = t0.replaceAll(/(?U)\b[XХ]VП/, 'XVII') 
        t0 = t0.replaceAll(/(?U)\b[XХ]VШ/, 'XVIII')
                
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
        def m1 = text =~ /([бвгґдєжзийклмнптфцчшщьюяѣБГҐДЄЖЗИЙЛПФХЦЧШЩЬЮЯ]['’ʼ]?)([aceiopxyABCEHIKMOPTXYáÁéÉíÍḯḮóÓúýÝ])/
        def t1 = m1.replaceAll( new Function<MatchResult, String>() { String apply(MatchResult mr) { // { mr -> // all, cyr, lat
            def cyr = mr.group(1)
            def lat = mr.group(2)
            out.debug "mix: 1.1"
            counts[0] += 1
            cyr.concat(latToCyrMap[lat])
        } } )

        // exclusively cyrillic letter preceeded by latin looking like cyrillic

//        text.replaceAll(/([aceiopxyABCEHIKMOPTXYáÁéÉíÍḯḮóÓúýÝ])(['’ʼ]?[бвгґдєжзийклмнптфцчшщьюяБГҐДЄЖЗИЙЛПФХЦЧШЩЬЮЯ])/, { all, lat, cyr ->
        def m2 = t1 =~ /([aceiopxyABCEHIKMOPTXYáÁéÉíÍḯḮóÓúýÝ])(['’ʼ]?[бвгґдєжзийклмнптфцчшщьюяѣБГҐДЄЖЗИЙЛПФХЦЧШЩЬЮЯ])/
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
        def m2 = t1 =~ /([асеіорхуАВСЕНІКМНОРТХУ])(['’ʼ]?[bdfgjklmnrstuvwzDFGJLNQRSUVWZ])/ // h is often == ѣ
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
    
    private static Pattern SMALL_UK_BIG_EN = ~ /([а-яіїєґ])([A-Z])/
    
    // в нашійTwitter-трансляції
    @CompileStatic
    String fixToSplit(String text, int[] counts) {

        text.replaceAll(/[а-яіїєґА-ЯІЇЄҐ'ʼ’a-zA-Z-]+/, { String it ->

            def m = SMALL_UK_BIG_EN.matcher(it)
            if( m ) {
                def split = m.replaceFirst('$1 $2')
                def parts = split.split(' ')
                
                if( parts[0].length() >= 2 && parts[1].length() >= 3 
                    && ltModule.knownWord(parts[0])
                    && ltModule.knownWordEn(parts[1]) ) {
                    out.debug "mix: 2.1"
                    counts[0] += 1
                    return split
                }
            }
            return it
        })
    }

    // ignoring best man'ом
    private static Pattern TO_ALL_CYR_WORD = ~/[а-яіїєґА-ЯІЇЄҐ]['’ʼ]?[aceiopxyrABCEHIKMHOPTXYáÁéÉíÍḯḮóÓúýÝ]|[aceiopxyrABCEHIKMHOPTXYáÁéÉíÍḯḮóÓúýÝ][а-яіїєґА-ЯІЇЄҐ]/
    private static Pattern TO_ALL_CYR_SYMB = ~/[aceiopxyrABCEHIKMHOPTXYáÁéÉíÍḯḮóÓúýÝ]/
    
    @CompileStatic
    String fixToAllCyrillic(String text, int[] counts) {
        // 2nd tier - try all Cyrillic
        // if we convert all Latin to Cyrillic and find it in the dictionary, use the conversion

        text.replaceAll(/[а-яіїєґА-ЯІЇЄҐ\u0301'ʼ’a-zA-ZáÁéÉíÍḯḮóÓúýÝ-]+/, { String it ->

            if( TO_ALL_CYR_WORD.matcher(it) ) {
                //            println "Found mix in: $it, known to LT: " + knownWord(it)
                if( (it.length() >= 3 || it =~ /[ІI][ТT]|[ТT][іiеe]|[НH][іiаaуy]/) 
                        && ! ltModule.knownWord(it) ) {
                    def fixed = TO_ALL_CYR_SYMB.matcher(it).replaceAll{ MatchResult lat -> latToCyrMap[lat.group()] }
//                    def fixedCleaned = fixed.replace('\u0301', '')
                    //                println "\tfixed $fixed known to LT: " + knownWord(fixedCleaned)
                    if( ltModule.knownWord(fixed) ) {
                        out.debug "mix: 2 - all cyr"
                        counts[0] += 1
                        return fixed
                    }
                }
            }
            return it
        })
    }

    private static final Pattern toLatinPattern = ~/[a-zA-Z]['’ʼ]?[асеіорхуАВСЕНІКМОРТХУ]|[асеіорхуАВСЕНІКМОРТХУ]['’ʼ]?[a-zA-Z]/
    
    @CompileStatic
    String fixToAllEnglish(String text, int[] counts) {
        // 2nd tier - try all Latin
        // if we convert all Cyrillic to Latin and find it in the English dictionary use conversion

        text.replaceAll(/[а-яіїєґА-ЯІЇЄҐa-zA-Z][а-яіїєґА-ЯІЇЄҐ'ʼ’a-zA-Z-]{3,}(?![0-9])/, { String it ->

            if( toLatinPattern.matcher(it) ) {
                //println "Found mix in: $it, known to LT: " // + knownWord(it)
//                if( ! ltModule.knownWord(it) ) {
                    def fixed = it.replaceAll(/[асеіорхуАВСЕНІКМОРТХУ]/, { String cyr -> cyrToLatMap[cyr] })
                    if( ltModule.knownWordEn(fixed) ) {
//                        out.debug "mix: 2 - all English"
                        counts[0] += 1
                        return fixed
                    }
//                }
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

        def t40 = fixToSplit(t3, counts)
        
        def t41 = fixToAllCyrillic(t40, counts)
// t3 = null // ml

        def t5 = fixToAllEnglish(t41, counts)
        
        // 3nd tier - least reliable

        def t6 = fixCharBetweenOthers(t5, counts)
        
// t4 = null // ml
        out.println "\tconverted ${counts[0]} lat->cyr, ${counts[1]} cyr->lat"

        return t6
    }


    static final Pattern MIX_1 = ~ /[а-яіїєґА-ЯІЇЄҐ][a-zA-Zóáíýúé]|[a-zA-Zóáíýúé][а-яіїєґА-ЯІЇЄҐ]/
//    static final Pattern APO_ENDING = ~ /(?U)([a-zA-Z]+)(['’ʼ][а-яіїє]{1,5})\b/
    
    @CompileStatic
    String fixCyrLatMix(String text) {
        // фото зhttp://www
        def t0 = text.replaceAll(/(?iu)([а-яіїєґ])(http)/, '$1 $2')
        
        t0 = t0.replaceAll(/([A-ZŁА-ЯІЇЄҐ]\.?)\h+(О[рp]\.)\h+([сc][іi])/, '$1 Op. ci')
        
        t0 = t0.replace("СOVID", "COVID") // Cyillic C
        // CO/CO2 with cyr/lat mix
        t0 = t0.replaceAll(/(?U)\b(СO|CО)(2?)\b/, 'CO$2')
        // CO2 with cyr
        t0 = t0.replaceAll(/(?U)\bСО2\b/, 'CO2')
        // degree Celcius with cyr
        t0 = t0.replaceAll(/(?U)\b[\u00B0\u00BA][СC]\b/, '\u00B0C')
        // 70-oї
        t0 = t0.replaceAll(/-oї/, '-ої')
        // -iон
        t0 = t0.replaceAll(/-iон/, '-іон')
        // 70-pічний
        t0 = t0.replaceAll(/-pіч/, '-річ')


        if( MIX_1.matcher(t0).find() ) {
            t0 = t0.replaceAll(/(?iu)([а-яіїєґ])(Fest|Train|Inform|SOS|Art|City|News)/, '$1\uE117$2')
            // this does not allow to split "нашійTwitter"
//            t0 = t0.replaceAll(/(?iu)([а-яіїєґ])([A-Z])/, '$1\uE117$2')
            
            KNOWN_MIXES.each { String k, String v ->
                t0 = t0.replace(k, v)
            }

//            t0 = APO_ENDING.matcher(t0).replaceAll('$1\uE010$2')
            
            if( MIX_1.matcher(t0).find() ) {
                out.println "\tlatin/cyrillic mix"

                def t1 = removeMix(t0)
// t0 = null // ml

                def m1 = MIX_1.matcher(t1)
                if( m1.find() ) {
                    String context = CleanTextCore2.getContext(m1, t1)
                    def totalLines = t1.lines().count()
                    def mixLines = t1.lines().filter{l -> MIX_1.matcher(l).find()}.count()
                    out.println "\t\tWARNING: still Latin/Cyrillic mix: $context: $mixLines of $totalLines lines"
                }
                t0 = t1
// t1 = null // ml
            }

//            t0 = t0.replace('\uE010', '')
            
            t0 = t0.replace(TEMP_EMPTY, '')
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
