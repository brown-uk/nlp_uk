# Disambiguation in TagText

There two layers where the disambiguation is happening:

1. LanguageTool - this disambiguation is used in LanguageTool rules and thus need to be more precise:
    - Simple disard: [the list](https://github.com/languagetool-org/languagetool/blob/master/languagetool-language-modules/uk/src/main/resources/org/languagetool/resource/uk/disambig_remove.txt) (approximately 600 rules)
    - Disambiguation based on rules. [the rule file](https://github.com/languagetool-org/languagetool/blob/master/languagetool-language-modules/uk/src/main/resources/org/languagetool/resource/uk/disambiguation.xml) (approximately 470 rules)
    - For most complicated disambiguation rules the logic is implemented in Java. [source code](https://github.com/languagetool-org/languagetool/blob/master/languagetool-language-modules/uk/src/main/java/org/languagetool/tagging/disambiguation/uk/UkrainianHybridDisambiguator.java) (approximately 10 rules)

2. Statistical disambiguation in TagText. This approach is based on statistics collected from [BRUK](https://github.com/brown-uk/corpus) corpus.
