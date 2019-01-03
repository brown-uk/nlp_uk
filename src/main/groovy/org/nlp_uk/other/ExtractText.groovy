#!/bin/env groovy

// this script tries to extract text from multiple document formats

//package org.nlp_uk.other


def srcDir = args.length > 0 ? args[0] : "pdf"
def targetDir = args.length > 1 ? args[1] : "txt"


println "Source dir: $srcDir, target dir: $targetDir"


exec("rm $targetDir/*.txt")
exec("rm $targetDir/multicol/*.txt")


new File(srcDir).eachFile { file ->

  if( file.isDirectory() )
    return

  def filename = file.name
  def lowercaseName = filename.toLowerCase()

  def txtFilename = filename.replaceFirst(/\.[^.]+$/, ".txt")


  def convertCmd

  if( lowercaseName.endsWith(".txt") ) {
    if( srcDir != targetDir ) {
      convertCmd = "cp $srcDir/$filename ."
    }
    //else noop
  }
  else if( lowercaseName.endsWith(".pdf") ) {
    convertCmd = "pdftotext -layout -nopgbrk -enc UTF-8 $srcDir/$filename $txtFilename"
  }
  else if( lowercaseName.endsWith(".djvu") ) {
    convertCmd = "djvutxt $srcDir/$filename $txtFilename"
  }
  else if( lowercaseName.endsWith(".fb2") ) {
    convertCmd = "unoconv -f txt -o $txtFilename $srcDir/$filename"
  }
  else if( lowercaseName.endsWith(".epub") ) {
    convertCmd = "ebook-convert $srcDir/$filename $txtFilename"
  }
  else if( lowercaseName.endsWith(".doc") || lowercaseName.endsWith(".docx") || lowercaseName.endsWith(".rtf") ) {
    convertCmd = "unoconv -f txt -o $txtFilename $srcDir/$filename"
  }
  else {
    System.err.println("Skipping not supported extension: $filename")
    return
  }
  
  println "Extracting from $filename..."


  if( convertCmd )
    if( exec(convertCmd) )
      return


  if( new File(txtFilename).text =~ /[‑-]   +[а-яіїєґ]/ ) {
    println "\tmultiple col"
    exec("mv $txtFilename $targetDir/multicol")
  }
  else {
    println "\tsingle col"
    exec("mv $txtFilename $targetDir/")
  }

}

def exec(String cmd) {
//    println "Executing: $cmd"
    def proc = cmd.execute()

    def b = new StringBuffer()
    proc.consumeProcessErrorStream(b)

    def exit = proc.waitFor()

    try {
      if( proc.text )
        print proc.text
      }
    catch(Exception e) {
      System.err.println("Failed to read stream: " + e.getMessage())
    }

    if( b )
      print b.toString()

    return exit
}
