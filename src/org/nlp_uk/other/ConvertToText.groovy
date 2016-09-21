#!/bin/env groovy

println "== " + args

def srcDir = args.length > 0 ? args[0] : "pdf"
def targetDir = args.length > 1 ? args[1] : "txt"

exec("rm $targetDir/*.txt")
exec("rm $targetDir/multicol/*.txt")


new File(srcDir).eachFile { file ->

  if( file.isDirectory() )
    return

  def lowercaseName = file.name.toLowerCase()

    println "== " + file.name

  def txtFilename = file.name.replaceFirst(/\.[^.]+$/, ".txt")


  def convertCmd

  if( lowercaseName.endsWith(".txt") ) {
    // noop
  }
  else if( lowercaseName.endsWith(".pdf") ) {
    convertCmd = "pdftotext -layout -nopgbrk -enc UTF-8 $srcDir/file.name $txtFilename"
  }
  else if( lowercaseName.endsWith(".djvu") ) {
    convertCmd = "djvutxt $srcDir/$file.name $txtFilename"
    println "== " + convertCmd
  }
  else {
    System.err.println("Skipping not supported extension: $file.name")
    return
  }

    println "== " + convertCmd


  if( convertCmd )
    if( exec(convertCmd) )
      return


  if( new File(txtFilename).text =~ /[‑-]   +[а-яіїєґ]/ ) {
    println "Multiple col: $txtFilename"
    exec("mv $txtFilename $targetDir/multicol")
  }
  else {
    println "Single col: $txtFilename"
    exec("mv $txtFilename $targetDir/")
  }

}

def exec(String cmd) {
//    println "Executing: $cmd"
    def proc = cmd.execute()

    def b = new StringBuffer()
    proc.consumeProcessErrorStream(b)

    def exit = proc.waitFor()

    if( proc.text )
      print proc.text
    if( b )
      print b.toString()

    return exit
}
