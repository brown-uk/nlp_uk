package ua.net.nlp.other.clean

import java.nio.charset.StandardCharsets

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@PackageScope
@CompileStatic
class EncodingModule {
    private static final String UTF8 = StandardCharsets.UTF_8.name()

    OutputTrait out
    
    String getText(File file) {
        String text = null
        
        if( file.length() > 10 ) {
            FileInputStream fis = new FileInputStream(file)
            byte[] bytes = new byte[1024]
            fis.read(bytes);
            fis.close()

            if( file.length() > 100 ) {
                if( file.bytes[0..3] == [0x50, 0x4B, 0x03, 0x04] ) {
                    out.println "\tERROR: found zip file, perhaps it's a Word document?"
                    return null
                }

                if( new String(bytes).startsWith("{\\rtf") ) {
                    out.println "\tERROR: found \"{\\rtf\", perhaps it's an RTF document?"
                    return null
                }
            }
            
            if( ! isUTF8(bytes) ) {
                out.println "\tWARNING: file is not in UTF-8 encoding"
                
                text = file.getText(UTF8)
                text = fixEncoding(text, file)
                if( text == null )
                    return null
            }
        }
        
        if( text == null ) {
            text = file.getText(UTF8)
        }
        text
    }
    
    String tryEncoding(File file, String encoding) {
        def cp1251Text = file.getText(encoding)
        if( cp1251Text =~ /(?iu)[сц]ьк|ння|від|[іи]й|ої|ти| [ійвузао] | н[еі] | що / ) {
            return cp1251Text
        }
        return null
    }

    @CompileStatic
    String fixEncoding(String text, File file) {
        if( text.contains("\u008D\u00C3") ) { // completely broken encoding for «ій»
            out.println "\tWARNING: nonfixable broken encoding found, garbage will be left in!"
            return null
        }

        if( text.contains("éîãî") ) {
            out.println "\tWARNING: broken encoding"

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
                out.println "\tERROR: still broken: encoding mixed with good one"
                return null
            }

            //        text = text.replaceAll(/([бвгґдзклмнпрстфхцшщ])\?([єїюя])/, '$1\'$2')

            out.println "\tEncoding fixed (good lines: $goodLines, convertedLines: $convertedLines, text: " + CleanTextCore.getSample(text)
        }
        else {
            ["cp1251", "utf-16"].each { encoding ->
                String decodedText = tryEncoding(file, encoding)
                if( decodedText ) {
                    out.println "\tWARNING: $encoding encoding found"
    
                    text = decodedText
    
                    if( text.size() < 10 ) {
                        out.println "\tFile size < 10 chars, probaby $encoding conversion didn't work, skipping"
                        return null
                    }
    
                    out.println "\tEncoding converted: " + CleanTextCore.getSample(text)
                }
            }
        }

        if( text.contains("\uFFFD") ) {
            out.println "\tERROR: File contains Unicode 'REPLACEMENT CHARACTER' (U+FFFD)"
            return null
        }

        return text
    }

    @CompileStatic
    private static boolean isUTF8(byte[] pText) {

        int expectedLength = 0;

        for (int i = 0; i < pText.length && i < 300; i++) {
            if ((pText[i] & 0b10000000) == 0b00000000) {
                expectedLength = 1;
            } else if ((pText[i] & 0b11100000) == 0b11000000) {
                expectedLength = 2;
            } else if ((pText[i] & 0b11110000) == 0b11100000) {
                expectedLength = 3;
            } else if ((pText[i] & 0b11111000) == 0b11110000) {
                expectedLength = 4;
            } else if ((pText[i] & 0b11111100) == 0b11111000) {
                expectedLength = 5;
            } else if ((pText[i] & 0b11111110) == 0b11111100) {
                expectedLength = 6;
            } else {
                return false;
            }

            while (--expectedLength > 0) {
                if (++i >= pText.length) {
                    return false;
                }
                if ((pText[i] & 0b11000000) != 0b10000000) {
                    return false;
                }
            }
        }

        return true;
    }
}
