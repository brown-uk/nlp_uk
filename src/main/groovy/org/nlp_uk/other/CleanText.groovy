#!/bin/env groovy

// This script reads all .txt files in given directory (default is "txt/") 
// and tries to find all with acceptable criterias for Ukrainian text (e.g. > 3k Ukrainian words)
// output files go into <dir>/good/
// NOTE:
// it also tries to fix broken encoding
// it also tries to clean up latin/cyrillic character mix
// it also tries to replace weird apostrophe characters with correct one (')
// it also tries to detect and skip two-column texts
// it also tries to merge some simple word wraps

//@Grab(group='org.languagetool', module='language-uk', version='4.3-SNAPSHOT')
@Grab(group='org.languagetool', module='language-uk', version='4.2')


import groovy.transform.Field
import groovy.transform.CompileStatic
import org.languagetool.tagging.uk.*
import org.languagetool.*


// for higher quality text esp. short newspaper articles you need to keep it low ~100
// for larger text with possible scanned sources you may want to go higher > 200
// note: we count words with 2 letters and more
@Field static final MIN_UKR_WORD_COUNT = 80

def KNOWN_MIXES = 
[
    "ТаблоID": "Табло@@ID",
    "Фirtka": "Ф@@irtka"
]

@Field def latToCyrMap = [
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

@Field def cyrToLatMap = [:]

latToCyrMap.each{ k,v -> cyrToLatMap[v] = k }

int wcArgIdx = (args as List).indexOf('-wc')
if( wcArgIdx >= 0 && wcArgIdx < args.length-1 ) {
    MIN_UKR_WORD_COUNT = args[wcArgIdx+1] as int
}
println "Min Word limit: $MIN_UKR_WORD_COUNT"


def dir = args.length > 0 && ! args[0].startsWith('-') ? args[0] : "txt"
def outDir = "$dir/good"

def outDirFile = new File(outDir)
if( ! outDirFile.isDirectory() ) {
    System.err.println "Output dir $outDir does not exists"
    return 1
}

if( outDirFile.listFiles(new FilenameFilter() {
        public boolean accept(File idir, String name) {
            return name.toLowerCase().endsWith(".txt");
        }
    }).size() > 0 ) {

    System.err.println "Output dir $outDir has (old) .txt files"
    return 1
}

@Field
def tagger = new UkrainianTagger()


new File(dir).eachFile { file->
    if( ! file.name.endsWith(".txt") )
        return

    println "Looking at ${file.name}"

    def text = file.text

    if( text.contains("\u008D\u00C3") ) { // completly broken encoding for «ій»
       println "\tWARNING: nonfixable broken encoding found, garbage will be left in!"
    }

    if( text.contains("éîãî") ) {
        println "\tWARNING: broken encoding"
        
        // some text (esp. converted from pdf) have broken encoding in some lines and good one in others

        int convertedLines = 0
        int goodLines = 0
        text = text.split(/\n/).collect { String line->
            if( line.trim() && ! (line =~ /(?iu)[а-яіїєґ]/) ) {
                line = new String(line.getBytes("cp1252"), "cp1251")
                convertedLines += 1
            }
            else {
                goodLines += 1
            }
            line
        }
        .join('\n')

        
        if( text.contains("éîãî") ) {
           println "\tERROR: still broken: encoding mixed with good one"
           return
        }

//        text = text.replaceAll(/([бвгґдзклмнпрстфхцшщ])\?([єїюя])/, '$1\'$2')

        println "\tEncoding fixed (good lines: $goodLines, convertedLines: $convertedLines, text: " + getSample(text)
    }
    else if( file.getText("cp1251").contains("ок") ) {
        println "\tWARNING: cp1251 encoding"

        text = file.getText("cp1251")
        
        if( text.size() < 200 ) {
            println "\tFile size < 200 chars, probaby cp1251 conversion didn't work, skipping"
            return
        }
        
        println "\tEncoding converted: " + getSample(text)
    }

    if( text.contains("\uFFFD") ) {
        println "\tWARNING: File contains Unicode 'REPLACEMENT CHARACTER' (U+FFFD)"
//        return
    }

    // SINGLE LOW-9 QUOTATION MARK sometimes used as a comma
    text = text.replace('\u201A', ',')


    // fix weird apostrophes
    text = text.replaceAll(/([бпвмфгґкхжчшр])[\"\u201D\u201F\u0022\u2018´`*]([єїюя])/, /$1'$2/) // "

    if( text.contains("\u00AD") ) {
        println "\tremoving soft hyphens: "
        text = text.replaceAll(/\u00AD(\n?[ \t]*)([а-яіїєґ'ʼ’-]+)([,;.!?])?/, '$2$3$1')
    }


    if( text =~ /[а-яіїєґА-ЯІЇЄҐ][a-zA-Zóáíýúé]|[a-zA-Zóáíýúé][а-яіїєґА-ЯІЇЄҐ]/ ) {
        KNOWN_MIXES.each { k,v ->
            text = text.replace(k, v)
        }
    
        if( text =~ /[а-яіїєґА-ЯІЇЄҐ][a-zA-Zóáíýúé]|[a-zA-Zóáíýúé][а-яіїєґА-ЯІЇЄҐ]/ ) {
            println "\tlatin/cyrillic mix in $file.name"
        
            text = removeMix(text)
        
            if( text =~ /[а-яіїєґА-ЯІЇЄҐ][a-zA-Zóáíýúé]|[a-zA-Zóáíýúé][а-яіїєґА-ЯІЇЄҐ]/ ) {
                println "\tWARNING: still latin/cyrillic mix in $file.name"
            }
        }

        KNOWN_MIXES.each { k,v ->
            text = text.replace(v, k)
        }
    }
    
    // latin a and i
    text = text.replaceAll(/([а-яіїєґ]), a ([А-ЯІЇЄҐа-яіїєґ])/, '$1, а $2')
    text = text.replaceAll(/([а-яіїєґ]) i ([А-ЯІЇЄҐа-яіїєґ])/, '$1 і $2')


    if( ! args.contains("-nc") && text =~ /[а-яїієґ]-  +[а-яіїєґ].{4}/ ) {
        println "\tERROR: two columns detected, skipping..."
        return
    }

    if( text.contains('\u2028') ) {
        text = text.replaceAll(/\u2028\n?/, '\n')
    }


    if( text.contains("-\n") && text =~ /[а-яіїєґА-ЯІЇЄҐ]-\n/ ) {
        println "\tsuspect word wraps"
        def cnt = 0
        int cntWithHyphen = 0
        text = text.replaceAll(/([а-яіїєґА-ЯІЇЄҐ'ʼ’-]+)-\n([ \t]*)([а-яіїєґ'ʼ’-]+)([,;.!?])?/, { it ->

            if( ! knownWord(it[1] + "-" + it[3]) ) {
              if( knownWord(it[1] + it[3]) ) {
                cnt += 1
//                print "."
                it[1] + it[3] + (it[4] ?: "") + "\n" + it[2]
			  }
			  else {
				  it[0]
			  }
            }
			else {
				cntWithHyphen += 1
//                print ","
				it[1] + "-" + it[3] + (it[4] ?: "") + "\n" + it[2]
			}
        })
        if( cnt > 0 || cntWithHyphen > 0 ) {
            println ""
        }
        println "\t\t$cnt word wraps removed, $cntWithHyphen newlines after hyphen removed"
    }

    if( text =~ /¬ *\n/ ) {
        println "\tsuspect word wraps with ¬:"
        text = text.replaceAll(/([а-яіїєґА-ЯІЇЄҐ'ʼ’-]+)¬ *\n([ \t]*)([а-яіїєґ'ʼ’-]+)/, '$1$3\n$2')
        println "\t\t¬ word wraps removed"
    }

    // NOTE: only counting words with 2 or more letters to filter out noised texts
    def ukrWords = text.split(/[^А-ЯІЇЄҐёа-яіїєґё'’ʼ-]+/).findAll{ it ==~ /[А-ЩЬЮЯІЇЄҐа-щьюяіїєґ][А-ЩЬЮЯІЇЄҐа-щьюяіїєґ'’ʼ-]+/ }
    int ukrWordCount = ukrWords.size()
    if( ukrWordCount < MIN_UKR_WORD_COUNT ) {
        println "\tERROR: Less than $MIN_UKR_WORD_COUNT Ukrainian words ($ukrWordCount): " + getSample(text) // + "\n\t" + ukrWords
        return
    }
    println "\tUkrainian word count: $ukrWordCount"
//    if( ukrWordCount < 300 ) println "\t\t: " + ukrWords

    // for really big text counting chars takes long time
    // we'll just evaluate first 1000k

    def lowerTextSample = text.toLowerCase().take(1024*1024)
    int ukrLetterCount = lowerTextSample.findAll { "іїєґ".contains(it) } .size()
    int rusLetterCount = lowerTextSample.findAll { "ыэъё".contains(it) } .size()

    def minUkrainianLetters = MIN_UKR_WORD_COUNT / 20
    if( ukrLetterCount < minUkrainianLetters ) {
        println "\tERROR: Less than $minUkrainianLetters Ukrainian letters ($ukrLetterCount): " + getSample(text)
        return
    }

    if( ukrLetterCount < rusLetterCount ) {
        println "\tERROR: Less Ukrainian letters ($ukrLetterCount) than Russian ($rusLetterCount), probably russian text: " + getSample(text)
        return
    }


    println "\tGOOD: $file.name\n"

    new File("$outDir/$file.name").text = text
}


boolean knownWord(word) {
    return ! tagger.getAnalyzedTokens(word)[0].hasNoTag()
}


def removeMix(String text) {
    int count1 = 0
    int count2 = 0

    // latin digits

    while( text =~ /[XVI][ХІ]|[ХІ][XVI]/ ) {
        text = text.replaceAll(/([XVI])([ХІ])/, { all, lat, cyr ->
            lat + cyrToLatMap[cyr]
        })
        text = text.replaceAll(/([ХІ])([XVI])/, { all, cyr, lat ->
            cyrToLatMap[cyr] + lat
        })
    }

    // 1st tier

    // exclusively cyrillic letter followed by latin looking like cyrillic

    text = text.replaceAll(/([бвгґдєжзийклмнптфцчшщьюяБГҐДЄЖЗИЙЛПФХЦЧШЩЬЮЯ]['’ʼ]?)([aceiopxyABCEHIKMHOPTXYáÁéÉíÍḯḮóÓúýÝ])/, { all, cyr, lat ->
        count1 += 1
        cyr + latToCyrMap[lat]
    })

    // exclusively cyrillic letter preceeded by latin looking like cyrillic

    text = text.replaceAll(/(?i)([aceiopxyABCEHIKMHOPTXYáÁéÉíÍḯḮóÓúýÝ])(['’ʼ]?[бвгґдєжзийклмнптфцчшщьюяБГҐДЄЖЗИЙЛПФХЦЧШЩЬЮЯ])/, { all, lat, cyr ->
        count1 += 1
        latToCyrMap[lat] + cyr
    })


    text = text.replaceAll(/([bdfghjklmnrstuvwzDFGJLNQRSUVWZ]['’ʼ]?)([асеіорхуАВСЕНІКМНОРТХУ])/, { all, lat, cyr ->
        count2 += 2
        lat + cyrToLatMap[cyr]
    })

    text = text.replaceAll(/([асеіорхуАВСЕНІКМНОРТХУ])(['’ʼ]?[bdfghjklmnrstuvwzDFGJLNQRSUVWZ])/, { all, cyr, lat ->
        count2 += 2
        cyrToLatMap[cyr] + lat
    })


    // 2nd tier - try all cyrillic
    // if we convert all latin to cyrillic and find it in the dicitonary use conversion

    text = text.replaceAll(/[а-яіїєґА-ЯІЇЄҐ'ʼ’a-zA-ZáÁéÉíÍḯḮóÓúýÝ-]+/, { it ->

        if( it =~ /[а-яіїєґА-ЯІЇЄҐ]['’ʼ]?[aceiopxyABCEHIKMHOPTXYáÁéÉíÍḯḮóÓúýÝ]/
                || it =~ /[aceiopxyABCEHIKMHOPTXYáÁéÉíÍḯḮóÓúýÝ]['’ʼ]?[а-яіїєґА-ЯІЇЄҐ]/ ) {
//            println "Found mix in: $it, known to LT: " + knownWord(it)
            if( ! knownWord(it) ) {
                def fixed = it.replaceAll(/[aceiopxyABCEHIKMHOPTXYáÁéÉíÍḯḮóÓúýÝ]/, { lat -> latToCyrMap[lat] })
                def fixedCleaned = fixed.replace('\u0301', '')
//                println "\tfixed $fixed known to LT: " + knownWord(fixedCleaned)
                if( knownWord(fixedCleaned) ) {
                    count1 += 1
                    return fixed
                }
            }
        }
        return it
    })


    // 3nd tier - least reliable

    // latin letter that looks like cyrillic between 2 cyrillics

    text = text.replaceAll(/([а-яіїєґА-ЯІЇЄҐ]['’ʼ]?)([aceiopxyABCEHIKMHOPTXYáÁéÉíÍḯḮóÓúýÝ])(['’ʼ]?[а-яіїєґА-ЯІЇЄҐ])/, { all, cyr, lat, cyr2 ->
        count1 += 1
        cyr + latToCyrMap[lat] + cyr2
    })

    // cyrillic letter that looks like latin between 2 latin

    text = text.replaceAll(/([a-zA-Z]['’ʼ]?)([асеіорхуАВСЕНІКМНОРТХУ])(['’ʼ]?[a-zA-Z])/, { all, lat, cyr, lat2 ->
        count2 += 2
        lat + cyrToLatMap[cyr] + lat2
    })

    println "\tconverted $count1 lat->cyr, $count2 cyr->lat"

    return text
}

String getSample(String text) {
    text[0..<Math.min(text.size(), 80)].replace('\n', '\\n')
}
