## Підхоплення змін в *languagetool* або *dict_uk* в скриптах *nlp_uk*

Потрібно мати встановленим:
* git
* java (>=1.8)
* maven


### Як вносити зміни в *LanguageTool* і аналізувати їх в *nlp_uk*:

УВАГА: ці кроки потрібні лише, якщо ви хочете вносити зміни у LanguageTool (напр. правила зняття омонімії) і тегувати тексти з цими змінами.


* Витягнути проекти:
  * git clone https://github.com/brown-uk/nlp_uk
  * git clone https://github.com/languagetool-org/languagetool
* Зібрати та встановити ядро та український модуль LT (в теці languagetool):
  * ./build.sh languagetool-core install
  * ./build.sh languagetool-language-modules/uk install
* Стерти кеш groovy grapes: `rm -rf $HOME/.groovy/grapes/org.languagetool`
* Запустити потрібний скрипт в модулі nlp_uk



### Як швидко вносити зміни в *LanguageTool* і аналізувати їх в *nlp_uk*:

УВАГА: цей варіант найкращий, якщо ви постійно вносите зміни у LanguageTool (напр. працюєте над правилами зняття омонімії) і перевіряєте результати тегування.

* Витягнути проекти (git clone)
  * git clone https://github.com/brown-uk/nlp_uk
  * git clone https://github.com/languagetool-org/languagetool
* Створити файл nlp_uk/gradle.properties зі шляхом до languagetool, напр. 
	`ltDir=../languagetool`
* Зібрати та встановити ядро LT (в теці nlp_uk):
  * ./gradlew installLtCore
* Витягнути залежності LT (в теці nlp_uk):
  * ./gradlew prepareLtUk
* Працювати у циклі (в теці nlp_uk):
  * Зредагувати файл правил зняття омонімії (../languagetool/languagetool-language-modules/uk/src/main/resources/org/languagetool/resource/uk/disambiguation.xml)
  * Зібрати український модуль LT та протестувати правила:
    * ./gradlew buildLtUk
  * Запустити завдання через gradle (наразі підтримується лише тегування):
    `./gradlew devTagText -PinputFile=text/1.txt`
    (вивід буде у файлі 1.tagged.txt)
  * На Unix/MacOS до попередньої команди можна додати завдання devTagTextDiff, воно створить файл 1.tagged.txt.diff з різницею між 1.tagged.txt.old і 1.tagged.txt
  * Можна вимикати правила зняття омонімії за допомогою параметра, напр. -PdisabledRules=NOUN_OR_ADJ_ROBOCHYI,CAPITAL_LETTER_INSIDE_SENTENCE


### Як вносити зміни в словник *dict_uk* і аналізувати їх в *nlp_uk*:

УВАГА: ці кроки потрібні лише, якщо ви хочете вносити зміни у словник і тегувати тексти з цими змінами.

* Витягнути всі три проекти (git clone)
  * https://github.com/brown-uk/nlp_uk
  * https://github.com/languagetool-org/languagetool
  * https://github.com/brown-uk/dict_uk
  * створити файл dict_uk/distr/language-dict-uk/gradle.properties зі шляхом languagetool, напр. 
	`ltDir=/home/user/work/languagetool`
* Зібрати ядро і утиліти LT (в теці languagetool):
  * ./build.sh languagetool-core install
  * ./build.sh languagetool-tools install
* Зібрати словник і скинути файли словника в languagetool (в теці dict_uk):
  * ./gradlew expand deployToLT
* Зібрати український модуль LT (в теці languagetool):
  * ./build.sh languagetool-language-modules/uk install
* Стерти кеш groovy grapes: `rm -rf $HOME/.groovy/grapes/org.languagetool`
* Запустити потрібний скрипт в модулі nlp_uk

