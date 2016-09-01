#!/bin/env groovy

// This script reads all .txt files in given directory (default is "txt/") 
// and tries to find all with acceptable criterias for Ukrainian text (e.g. > 3k Ukrainian words)
// it also tries to fix broken encoding
// it also tries to clean up latin/cyrillic character mix
// output files go into <dir>/good/


import groovy.transform.Field


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

    if( text =~ /[а-яіїєґА-ЯІЇЄҐ]['’]?[a-zA-Z]/ ) {
        println "latin/cyrillic mix in $file.name"
        
        text = removeMix(text)
        
        if( text =~ /[а-яіїєґА-ЯІЇЄҐ]['’]?[a-zA-Z]/ ) {
            println "\tstill latin/cyrillic mix in $file.name"
        }
    }

    // fix weird apostrophes
    text = text.replaceAll(/([бпвмфгґкхжчшр])[\u0022`]([єїюя])/, /$1'$2/)


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
