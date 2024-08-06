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
        // stenograms from Rada
        text = text.replaceAll("(?iu)С е с і й н и й +з а л +В е р х о в н о ї +Р а д и", "Сесійний зал Верховної Ради")
        text = text.replace("О п л е с к и", "Оплески")
        text = text.replace("П і с л я п е р е р в и", "Після перерви")

        text = text.replace("пагороджен", "нагороджен")
        
//        text = t01.replaceAll(/йдетсь?я/, 'йдетьсья')
        // байдужосте, відповідальносте, відсутносте, досконалосте, діяльносте, промисловосте
//        >    640    ліття
        
        return text
    }
}
