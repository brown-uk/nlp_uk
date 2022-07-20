## Підхоплення змін в *languagetool* в скриптах *nlp_uk*

Потрібно мати встановленим:

* git
* java (>=11)
* maven
* gradle


### Як вносити зміни в *LanguageTool* і аналізувати їх в *nlp_uk*:

УВАГА: ці кроки потрібні лише, якщо ви хочете вносити зміни у LanguageTool (напр. правила зняття омонімії) і тегувати тексти з цими змінами.

* Витягнути проекти:
  * git clone https://github.com/brown-uk/nlp_uk
  * git clone https://github.com/languagetool-org/languagetool
* Зібрати та встановити ядро та український модуль LT (в теці languagetool):
  * ./build.sh languagetool-core install
  * ./build.sh languagetool-language-modules/uk install
* Поставити останню версію languagetool в скрипті, напр. в TagTextCore.groovy:
    `@Grab(group='org.languagetool', module='language-uk', version='5.9-SNAPSHOT')`
* Стерти кеш groovy grapes: `rm -rf $HOME/.groovy/grapes/org.languagetool`
* Запустити скрипт TagText.groovy в модулі nlp_uk


### Інтеграція *nlp_uk* в іншому проєкті:

УВАГА: цей варіант найкращий, якщо ви постійно вносите зміни у LanguageTool (напр. працюєте над правилами зняття омонімії) і перевіряєте результати тегування.

* Витягнути проєкт (git clone)
  * git clone https://github.com/brown-uk/nlp_uk
* Побудувати й встановити пакунок nlp_uk:
  * cd nlp_uk
  * ./gradlew publish
* В своєму проєкті додати залежність, напр. для gradle:

    repositories {
       mavenLocal()
       mavenCentral()
    }
    dependencies {
        implementation 'ua.net.nlp:nlp_uk:3.0-SNAPSHOT'
    }


* Приклад коду Java з використанням тегування через nlp_uk:
    * [TagTextWrapperRun.java](../src/example/java/ua/net/nlp/tools/TagTextWrapperRun.java)

