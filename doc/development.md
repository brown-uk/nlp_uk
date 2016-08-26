## Підхоплення змін в *languagetool* або *dict_uk* в скриптах *nlp_uk*

Потрібно мати встановленим:
* git
* java (>=1.8)
* maven


### Як вносити зміни в *LanguageTool* і аналізувати їх в *nlp_uk*:

УВАГА: ці кроки потрібні лише, якщо ви хочете вносити зміни у LanguageTool (напр. правила зняття омонімії) і тегувати тексти з цими змінами.


* Витягнути проекти (git clone)
  * https://github.com/brown-uk/nlp_uk
  * https://github.com/languagetool-org/languagetool
* Зібрати та встановити ядро LT (в теці languagetool):
  * build.sh languagetool-core install
* Зібрати та встановити український модуль LT (в теці languagetool):
  * build.sh languagetool-language-modules/uk install
* Стерти кеш groovy grapes: `rm -rf $HOME/.groovy/grapes/org.languagetool`
* Запустити потрібний скрипт в модулі nlp_uk



### Як швидко вносити зміни в *LanguageTool* і аналізувати їх в *nlp_uk*:

УВАГА: ці кроки придатні, якщо ви постійно вносите зміни у LanguageTool (напр. працюєте над правилами зняття омонімії) і перевіряєте результати тегування.

* Витягнути проекти (git clone)
  * https://github.com/brown-uk/nlp_uk
  * https://github.com/languagetool-org/languagetool
  * створити файл nlp_uk/gradle.properties зі шляхом languagetool, напр. 
	`ltDir=/home/user/work/languagetool
    inputFile=../text/1`
* Зібрати та встановити ядро LT (в теці languagetool):
  * build.sh languagetool-core install
* Зібрати український модуль LT (в теці languagetool):
  * build.sh languagetool-language-modules/uk compile
* Запустити потрібний скрипт в модулі nlp_uk

* Запустити потрібне завдання через gradle:
    `./gradlew devTagText`


### Як вносити зміни в словник *dict_uk* і аналізувати їх в *nlp_uk*:

УВАГА: ці кроки потрібні лише, якщо ви хочете вносити зміни у словник і тегувати тексти з цими змінами.

* Витягнути всі три проекти (git clone)
  * https://github.com/brown-uk/nlp_uk
  * https://github.com/languagetool-org/languagetool
  * https://github.com/brown-uk/dict_uk
  * створити файл dict_uk/distr/language-dict-uk/gradle.properties зі шляхом languagetool, напр. 
	`ltDir=/home/user/work/languagetool`
* Зібрати ядро і утиліти LT (в теці languagetool):
  * build.sh languagetool-core install
  * build.sh languagetool-tools install
* Зібрати словник і скинути файли словника в languagetool (в теці dict_uk):
  * gradle expand deployToLT
* Зібрати український модуль LT (в теці languagetool):
  * build.sh languagetool-language-modules/uk install
* Стерти кеш groovy grapes: `rm -rf $HOME/.groovy/grapes/org.languagetool`
* Запустити потрібний скрипт в модулі nlp_uk

