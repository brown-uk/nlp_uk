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
        text = text.replace(/Крим SOS/, 'КримSOS')
        text = text.replace(/Євромайдан SOS/, 'ЄвромайданSOS')
        text = text.replace(/Чорнобиль Art/, 'ЧорнобильArt')
        text = text.replace(/Золотоноша City/, 'ЗолотоношаCity')
        text = text.replace(/Умань News/, 'УманьNews')
        text = text.replace('Хутро OFF', 'ХутроOFF')
        text = text.replaceAll(/(?iu)(Армія) (Inform)/, '$1$2')
        text = text.replaceAll(/(?iu)(Гоголь|Пуленк) (FEST|Train)/, '$1$2')
        text = text.replaceAll(/([a-z])(Переглянути)/, '$1$2')
        text = text.replace('Ник Life', 'НикLife')
        
        text = text.replace("пагороджен", "нагороджен")
        text = text.replace("голсоування", "голосування")
        text = text.replace("річ-чя", "річчя")
        text = text.replaceAll(/(Івано)\h+(Франківськ)/, '$1-$2')
        text = text.replaceAll(/(Івано)\h+([\u2013-])\h+(Франківськ)/, '$1$2$3')
        
        text = text.replaceAll(/([дД]епутата?)([А-ЯІЇЄҐ])/, '$1 $2')
        
//        text = t01.replaceAll(/йдетсь?я/, 'йдетьсья')
        // байдужосте, відповідальносте, відсутносте, досконалосте, діяльносте, промисловосте
//        >    640    ліття
        
        return text
    }
}
