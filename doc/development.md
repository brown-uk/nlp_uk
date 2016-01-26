### Як вносити зміни в *dict_uk/languagetool* і аналізувати їх в *nlp_uk*:
* Витягнути всі три проекти (git clone)
  * https://github.com/arysin/dict_uk
  * https://github.com/arysin/nlp_uk
  * https://github.com/languagetool-org/languagetool
* Зібрати словник (в dict_uk)
  * gradle expandForRules
* Скинути файли словника в languagetool (в dict_uk/distr/language-dict-uk)
  * створити файл dict_uk/distr/language-dict-uk/gradle.properties зі шляхом languagetool, напр. 
	languagetoolDictDestDir = /home/username/work/languagetool/languagetool-language-modules/uk/src/main/resources/org/languagetool/resource/uk
  * gradle copyDictFiles
* Зібрати ядро LT та український модуль (в languagetool):
  * build.sh languagetool-core install
  * build.sh languagetool-language-modules/uk install
* Запустити потрібний скрипт в модулі nlp_uk 

**ЗАУВАГА**: якщо nlp_uk не підхоплює останню версію LT/словника, можливо потрібно очистити кеш grape (найпростіше стерти каталог $HOME/.groovy/grapes)
