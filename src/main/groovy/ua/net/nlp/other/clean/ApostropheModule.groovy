package ua.net.nlp.other.clean

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@PackageScope
@CompileStatic
class ApostropheModule {
    OutputTrait out
    LtModule ltModule

    String fixWeirdApostrophes(String t01) {
        // fix weird apostrophes
        t01 = t01.replaceAll(/(?iu)([бвгґдзкмнпрстфхш])[\"\u201D\u201F\u0022\u2018\u2032\u0313\u0384\u0092´`?*]([єїюя])/, /$1'$2/) // "
        t01 = t01.replaceAll(/(?iu)[´`]([аеєиіїоуюя])/, '\u0301$1')
//        t0 = t0.replaceAll(/(?iu)([а-яіїєґ'\u2019\u02BC\u2013-]*)[´`]([а-яіїєґ'\u2019\u02BC\u2013-]+)/, { all, w1, w2
//                  def fix = "$w1'$w2"
//                knownWord(fix) ? fix : all
//        }

        return t01
    }
    
    String fixSpacedApostrophes(String t01) {
        t01 = t01.replaceAll(/([а-яїієґА-ЯІЇЄҐ]+) (['\u2019\u02BC][яєюїЯЄЮЇ][а-яіїєґА-ЯІЇЄҐ]+)/, { all, w1, w2 ->
            String fix = "${w1}${w2}"
            ltModule.knownWord(fix) ? fix : all
        })

        return t01
    }
}
