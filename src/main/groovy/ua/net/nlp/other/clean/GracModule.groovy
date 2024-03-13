package ua.net.nlp.other.clean

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@PackageScope
@CompileStatic
class GracModule {
    OutputTrait out
    LtModule ltModule

    String fix(String text) {
        
        // 1-time GRAC
//        t10 = t10.replaceAll(/(чоло|Людо)[ -](в[іі]к)/, '$1$2')

        // TODO: remove later - temp fix
//        text = text.replace(/Крим SOS/, 'КримSOS')
//        text = text.replace(/Євромайдан SOS/, 'ЄвромайданSOS')
//        text = text.replace(/Армія Inform/, 'АрміяInform')
//        text = text.replace(/Чорнобиль Art/, 'ЧорнобильArt')
//        text = text.replace(/Золотоноша City/, 'ЗолотоношаCity')
//        text = text.replace(/Умань News/, 'УманьNews')

//        text = text.replace("пагороджен", "нагороджен")
        
//        t01 = t01.replaceAll(/йдетсь?я/, 'йдетьсья')
        // байдужосте, відповідальносте, відсутносте, досконалосте, діяльносте, промисловосте
//        >    640    ліття
        
        return text
    }
}
