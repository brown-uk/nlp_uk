# LanguageTool API NLP UK

This is a project to demonstrate NLP API from LanguageTool for Ukrainian language.

Це — проект демонстрації API для обробляння природної мови в LanguageTool для української мови.

Використовує мову [groovy](http://www.groovy-lang.org/), засоби для токенізації та тегування також мають скрипти-обгортки для python3.

Для запуску скриптів потрібно встановити мову [groovy](http://www.groovy-lang.org/) 

УВАГА: при першому запуску потрібно мережеве з'єднання, щоб скрипти могли звантажити потрібні модулі

ПРИМІТКА: скрипт gradle потрібен лише для розробників

Для невеликих текстів приклад розбиття та тегування також можна переглянути [на сторінці аналізу LanguageTool](https://community.languagetool.org/analysis?lang=uk)

Основні скрити аналізу текстів знаходяться в каталозі [src/main/groovy/org/nlp_uk/tools](src/main/groovy/org/nlp_uk/tools)


## Використання


### Утиліта аналізу тексту: TagText.groovy
### Утиліта розбиття тексту: TokenizeText.groovy
### Утиліта лематизації тексту: LemmatizeText.groovy

[докладніше про утиліти аналізу](src/main/groovy/org/nlp_uk/tools/README.md)


## Допоміжні утиліти:
[докладніше про допоміжні утиліти](src/main/groovy/org/nlp_uk/other/README.md)


### Використовувані програмні засоби

Для аналізу текстів використовується український модуль [LanguageTool](https://languagetool.org)

Для тегування лексем використовується версія словника української мови з проекту [ВЕСУМ](https://github.com/brown-uk/dict_uk)


## Ліцензія

Проект LanguageTool API NLP UK розповсюджується за умов ліцензії [GPL версії 3](https://www.gnu.org/licenses/gpl.html)

Copyright (c) 2018 Андрій Рисін (arysin@gmail.com)
