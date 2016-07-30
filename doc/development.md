### Як вносити зміни в *dict_uk/languagetool* і аналізувати їх в *nlp_uk*:

УВАГА: ці кроки потрібні лише, якщо ви хочете вносити зміни у словник і тегувати тексти з цими змінами.

Потрібно мати встановленим:
* git
* java (>=1.8)
* gradle
* maven

* Витягнути всі три проекти (git clone)
  * https://github.com/brown-uk/dict_uk
  * https://github.com/brown-uk/nlp_uk
  * https://github.com/languagetool-org/languagetool
  * створити файл dict_uk/distr/language-dict-uk/gradle.properties зі шляхом languagetool, напр. 
	`languagetoolDictDestDir = /home/username/work/languagetool/languagetool-language-modules/uk/src/main/resources/org/languagetool/resource/uk`
* Зібрати ядро і утиліти LT (в теці languagetool):
  * build.sh languagetool-core install
  * build.sh languagetool-tools install
* Зібрати словник і скинути файли словника в languagetool (в теці dict_uk):
  * gradle expand deployToLT
* Зібрати український модуль LT (в теці languagetool):
  * build.sh languagetool-language-modules/uk install
* Запустити потрібний скрипт в модулі nlp_uk 

**ЗАУВАГА**: якщо nlp_uk не підхоплює останню версію LT/словника, можливо потрібно очистити кеш grape (найпростіше стерти каталог $HOME/.groovy/grapes/org.languagetool)
