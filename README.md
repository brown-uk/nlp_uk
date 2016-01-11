This is a project to demonstrate NLP API from LanguageTool for Ukrainian language.

Це — проект демонстрації API для обробляння природної мови в LangaugeTool для української мови.


Використовує мову groovy (http://www.groovy-lang.org/)

Демонстрація для простих речень:
	groovy src/org/nlp_uk/demo/NlpUkExample.groovy

	* Показує розбиття тексту на речення
	* Показує аналіз лексем у тексті
	* Показує перевіку граматики та стилю

Демонстрація аналізу тексту вебстворінки:
	groovy src/org/nlp_uk/demo/NlpUkWebPage.groovy
	
	* Звантажує текст з архіву статті журналу «Тиджень» і виводить частоту вживання лем та тег частини мови
