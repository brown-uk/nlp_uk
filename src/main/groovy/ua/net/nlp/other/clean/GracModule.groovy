package ua.net.nlp.other.clean

import java.util.regex.Pattern

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@PackageScope
@CompileStatic
class GracModule {
    private static final Pattern BROKEN_I = Pattern.compile(/\b([А-ЯІЇЄҐ]?[а-яіїєґ'\u2019\u02bc-]+) і ([а-яіїєґ'\u2019\u02bc-]+)\b/, Pattern.UNICODE_CHARACTER_CLASS)

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
        text = text.replaceAll(/(Івано)\h+([\u2013\u2011-])\h+(Франківськ)/, '$1$2$3')
        
        text = text.replaceAll(/([дД]епутата?)([А-ЯІЇЄҐ])/, '$1 $2')
        text = text.replace("каналуМнения", "каналу Мнения")
        text = text.replace("номеріжурналу", "номері журналу")
        text = text.replace(" зорутут", " зору тут")
        
//        text = t01.replaceAll(/йдетсь?я/, 'йдетьсья')
        // байдужосте, відповідальносте, відсутносте, досконалосте, діяльносте, промисловосте
//        >    640    ліття
        
        return text
    }


    String firtka(String text) {

        def m2 = BROKEN_I.matcher(text)
        text = m2.replaceAll{ mr ->
                def w1 = mr.group(1)
                def w2 = mr.group(2)

                if( ( ltModule.knownWord(w1) && ! (w1 ==~ /[гґдзклмнпрстфхцчшщ]/) )
                   || ltModule.knownWord(w2) && ! (w2 ==~ /[гґдзклмнпрстфхцчшщ]/) )
                    return mr.group(0)

                def newWord = "${w1}і${w2}"
                if( ltModule.knownWord(newWord) )
                    return newWord

                return mr.group(0)
            }

        return text
    }
}
