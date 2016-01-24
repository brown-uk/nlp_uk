This is a project to demonstrate NLP API from LanguageTool for Ukrainian language.

Це — проект демонстрації API для обробляння природної мови в LangaugeTool для української мови.

Використовує мову groovy (http://www.groovy-lang.org/)

### Демонстрація можливостей для простих речень:
groovy src/org/nlp_uk/demo/NlpUkExample.groovy

* Показує розбиття тексту на речення
* Показує аналіз лексем у тексті
* Показує перевірку граматики та стилю

### Демонстрація аналізу тексту вебсторінки:
groovy src/org/nlp_uk/demo/NlpUkWebPage.groovy

* Звантажує текст з архіву статті журналу «Тиждень» і виводить частоту вживання лем та тег частини мови

###Утиліта аналізу тексту:
groovy TagText.groovy -i <input_file> -o <output_file>

* Аналізує текст і записує результат у виходовий файл:
** розбиває на речення
** розбиває на лексеми
** проставляє теги для лексем
** робить зняття омонімії (наразі алгоритм зняття омонімії досить базовий)

Для тегування лексем наразі використовується версія словник української мови з проекту https://github.com/arysin/dict_uk
для перевірки правил в LanguageTool, в майбутньому планується використання повнішої «корпусної» версії словника.



### Як працювати з dict__uk та nlp__uk разом:
* Витягнути всі три проекти (git clone)
  * https://github.com/arysin/dict_uk
  * https://github.com/arysin/nlp_uk
  * https://github.com/languagetool-org/languagetool
* Зібрати словник (в dict_uk)
  * gradle expandForRules
* Скинути файли словника в languagetool (в dict_uk/distr/language-dict-uk)
  * gradle copyDictFiles
* Зібрати ядро LT та український модуль (в languagetool):
  * build.sh languagetool-core install
  * build.sh languagetool-language-modules/uk install
* Запустити потрібний скрипт в модулі nlp_uk 

**ЗАУВАГА**: якщо nlp_uk не підхоплює останню версію LT/словника, можливо потрібно очистити кеш grape (найпростіше стерти каталог $HOME/.groovy/grapes)
