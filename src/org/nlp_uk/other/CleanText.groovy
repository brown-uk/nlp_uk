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

@Grab(group='org.languagetool', module='language-uk', version='3.5')


import groovy.transform.Field
import org.languagetool.tagging.uk.*
import org.languagetool.*


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
    'Y' : 'У'
]

@Field def cyrToLatMap = [:]

latToCyrMap.each{ k,v -> cyrToLatMap[v] = k }


def dir = args.length > 0 ? args[0] : "txt"
def outDir = "$dir/good"

def outDirFile = new File(outDir)
if( ! outDirFile.isDirectory() ) {
    System.err.println "Output dir $outDir does not exists"
    return 1
}
    

def tagger = new UkrainianTagger()


new File(dir).eachFile { file->
    if( ! file.name.endsWith(".txt") )
        return


    def text = file.text

    if( text.contains("éîãî") ) {
        println "WARNING: broken encoding in $file.name"

        text = new String(text.getBytes("cp1252"), "cp1251")
        
        if( text.contains("éîãî") ) {
           println "ERROR: still broken: encoding mixed with good one in $file.name"
           return
        }
        
        println "\tEncoding fixed: " + text[0..80]
    }
    else if( file.getText("cp1251").contains("ок") ) {
        println "WARNING: cp1251 encoding in $file.name"

        text = file.getText("cp1251")
        
        if( text.size() < 200 ) {
            println "File size < 200 chars, probaby cp1251 conversion didn't work, skipping"
            return
        }
        
        println "\tEncoding converted: " + text[0..80]
    }



    if( text =~ /[а-яіїєґА-ЯІЇЄҐ]['’]?[a-zA-Z]/ ) {
        println "latin/cyrillic mix in $file.name"
        
        text = removeMix(text)
        
        if( text =~ /[а-яіїєґА-ЯІЇЄҐ]['’]?[a-zA-Z]/ ) {
            println "\tstill latin/cyrillic mix in $file.name"
        }
    }

    // fix weird apostrophes
    text = text.replaceAll(/([бпвмфгґкхжчшр])[\u0022`]([єїюя])/, /$1'$2/)


    if( text =~ /[а-яїієґ]-  +[а-яіїєґ]/ ) {
           println "ERROR: two columns detected in $file.name, skipping..."
           return
    }


    if( text.contains("\u00AD") ) {
        println "removing soft hyphens: "
        text = text.replaceAll(/\u00AD(\n?[ \t]*)([а-яіїєґ'ʼ’-]+)([,;.!?])?/, '$2$3$1')
    }

    if( text.contains("-\n") ) {
        println "suspect word wraps: "
        text = text.replaceAll(/([а-яіїєґА-ЯІЇЄҐ'ʼ’-]+)-\n([ \t]*)([а-яіїєґ'ʼ’-]+)([,;.!?])?/, { it ->

            if( tagger.getAnalyzedTokens(it[0])[0].hasNoTag()
                    && ! tagger.getAnalyzedTokens(it[1] + it[3])[0].hasNoTag() ) {
                print "."
                it[1] + it[3] + (it[4] ?: "") + "\n" + it[2]
            }
            else {
                it[0]
            }
        })
        println ""
    }


    if( text.split(/[ \t\n,;.]/).findAll{ it ==~ /[А-ЯІЇЄҐа-яіїєґ'’ʼ-]+/ }.size() < 3000 ) {
        println "Less that 3k words in $file.name"
        return
    }

    if( text.count("і") < 10 || text.count("ї") < 10 ) {
        println "Not enouogh Ukrainian letters in $file.name"
        return
    }


    println "GOOD: $file.name"

    new File("$outDir/$file.name").text = text
}


def unknownWord(word) {
    tagger.getAnalyzedTokens(word)[0].hasNoTag()
}



def removeMix(text) {
    def count1 = 0
    def count2 = 0

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

    text = text.replaceAll(/([бвгґдєжзийклмнптфцчшщьюяБГҐДЄЖЗИЙЛПФХЦЧШЩЬЮЯ])([aceiopxyABCEHIKMHOPTXY])/, { all, cyr, lat ->
        count1 += 1
        cyr + latToCyrMap[lat]
    })
    text = text.replaceAll(/(?i)([aceiopxyABCEHIKMHOPTXY])([бвгґдєжзийклмнптфцчшщьюяБГҐДЄЖЗИЙЛПФХЦЧШЩЬЮЯ])/, { all, lat, cyr ->
        count1 += 1
        cyr + latToCyrMap[lat]
    })

    text = text.replaceAll(/([bdfghjklmnrstuvwzDFGJLNQRSUVWZ])([асеіорхуАВСЕНІКМНОРТХУ])/, { all, lat, cyr ->
        count2 += 2
        lat + cyrToLatMap[cyr]
    })
    text = text.replaceAll(/([асеіорхуАВСЕНІКМНОРТХУ])([bdfghjklmnrstuvwzDFGJLNQRSUVWZ])/, { all, cyr, lat ->
        count2 += 2
        lat + cyrToLatMap[cyr]
    })
    

    // 2nd tier

    text = text.replaceAll(/([а-яіїєґА-ЯІЇЄҐ])([aceiopxyABCEHIKMHOPTXY])([а-яіїєґА-ЯІЇЄҐ])/, { all, cyr, lat, cyr2 ->
        count1 += 1
        cyr + latToCyrMap[lat] + cyr2
    })

    text = text.replaceAll(/([a-zA-Z])([асеіорхуАВСЕНІКМНОРТХУ])([a-zA-Z])/, { all, lat, cyr, lat2 ->
        count2 += 2
        lat + cyrToLatMap[cyr] + lat2
    })

    println "\tconverted $count1 lat->cyr, $count2 cyr->lat"

    return text
}
