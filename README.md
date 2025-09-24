# LanguageTool API NLP UK

This is a project to demonstrate NLP API from LanguageTool for Ukrainian language.

Це — проект демонстрації API для обробляння природної мови в LanguageTool для української мови.

Використовує мову [groovy](http://www.groovy-lang.org/), засоби для токенізації та тегування також мають скрипти-обгортки для python3 та java.
Рекомендована версія groovy - 4.0.22 або новіше.

Для запуску скриптів потрібно встановити мову [groovy](http://www.groovy-lang.org/) 

УВАГА: при першому запуску потрібно мережеве з'єднання, щоб скрипти могли звантажити потрібні модулі

ПРИМІТКА: файли gradle потрібен лише для розробників

Основні скрити аналізу текстів знаходяться в каталозі [src/main/groovy/ua/net/nlp/tools](src/main/groovy/ua/net/nlp/tools)

Тегувальник підтримує розмітку UD (Universal Dependencies).

## Використання


### Утиліта розбиття тексту: TokenizeText.groovy
### Утиліта аналізу тексту: TagText.groovy

[докладніше про утиліти аналізу](doc/README_tools.md)


## Допоміжні утиліти:
[докладніше про допоміжні утиліти](doc/README_other.md)


## Використання (найпростіший шлях)

Встановити JDK 17 (https://www.oracle.com/java/technologies/downloads/#jdk17-windows)

### Чистити файл
UNIX:<br/>
`./gradlew -q cleanText -Pargs="-i <мій-файл.txt>"`<br/>
Windows:<br/>
`gradlew.bat -q cleanText -Pargs="-i <мій-файл.txt>"`

Буде створено файл <мій-файл.good.txt> в якому виправлено знайдені проблеми зі словами.

### Тегувати файл
UNIX:<br/>
`./gradlew -q tagText -Pargs="-i <мій-файл.txt> -su"`<br/>
Windows:<br/>
`gradlew.bat -q tagText -Pargs="-i <мій-файл.txt> -su"`

Буде створено файл <мій-файл.tagged.xml>. Прапорець "-su" генерує файл невідомих слів.

## Робота офлайн (без доступу до інтернету)

Локально:
`./gradlew copyRuntimeLibs`
це стягне потрібні залежності у build/lib
потім скопіювати все на потрібну систему і запускати:
`./gradlew --offline tagText -PlocalLib -Pargs="-g <file.txt>"`

## Використовувані програмні засоби

Для аналізу текстів використовується український модуль [LanguageTool](https://languagetool.org)

Для тегування лексем використовується словник української мови з проекту [ВЕСУМ](https://github.com/brown-uk/dict_uk)


## Ліцензія

Проект LanguageTool API NLP UK розповсюджується за умов ліцензії [GPL версії 3](https://www.gnu.org/licenses/gpl.html)

Copyright (c) 2022 Андрій Рисін (arysin@gmail.com)
