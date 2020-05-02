#!/bin/env groovy

package org.nlp_uk.other

import static org.junit.Assert.assertEquals

//evaluate(new File('CleanText.groovy'))

def cleanText = new CleanText()
def file = new File('CleanTextTest.groovy')

def result = cleanText.cleanUp('просто-\nрово-часового', file, [])
assert result == "просторово-часового\n"
//result = cleanText.cleanUp('двох-\nсторонній', file, [])
//assert result == "двохсторонній\n"

//TODO:
result = cleanText.cleanUp("минулого-сучасного-май-\nбутнього", file, [])
assert result == "минулого-сучасного-майбутнього\n"

//result = cleanText.cleanUp("дитино-\nцентристської", file, [])
//assert result == "дитиноцентристської\n"

//"жінки-\nвченої"
//"скло-\nтермосному"
//"дніпро-\nдонецької"
//"чернігово-\nсіверським"
//"за-\nлізо-рослинне"


//result = cleanText.cleanUp("54                           ISSN 1562-0905 Регіональна економіка 2013, №4", file, [removeMeta: true])
//assert result == "\n"

//result = cleanText.cleanUp("ISSN 1028-9763. Математичні машини і системи, 2011, № 2", file, [removeMeta: true])
//assert result == "\n"



result = cleanText.cleanUp(
"""AAA

ISSN 1028-9763. Математичні машини і системи, 2011, № 2              85

BBB

54                           ISSN 1562-0905 Регіональна економіка 2013, №4

CCC""", file, [modules: 'nanu'])

def expected = "AAA\n\nBBB\n\nCCC"

assertEquals(expected, result)

//assert result == expected




result = cleanText.cleanUp(
"""тивної дії. Адже те громадянське суспільство, що існувало в радянській системі, було
                          «Наука. Релігія. Суспільство» № 2’2012                        41
                                                                       Олександр Поліщук


надзвичайно заангажоване і заідеологізоване. Дана штучно утворена ситуація не визна-
...
у відповідність із певними індивідуальними інтересами і цінностями [2, с. 109].
44                       «Наука. Релігія. Суспільство» № 2’2012
Колективна дія громадянського суспільства у соціальній системі
Щось там ще
               «Наука. Релігія. Суспільство» № 2’2012                                        45
Щось там ще ще""", file, [modules: 'nanu'])

expected = '''тивної дії. Адже те громадянське суспільство, що існувало в радянській системі, було
                                                                       Олександр Поліщук


надзвичайно заангажоване і заідеологізоване. Дана штучно утворена ситуація не визна-
...
у відповідність із певними індивідуальними інтересами і цінностями [2, с. 109].
Колективна дія громадянського суспільства у соціальній системі
Щось там ще
Щось там ще ще'''


//assert result == expected


///"""AAA
// 
//10
//                ISSN 1816-1545 Фізико-математичне моделювання та інформаційні технології
//                                                                      2010, вип. 12, 9-37

def text = """AAA

                                                                                                    89
Василь Кондрат, Ольга Грицина
Рівняння електромагнітотермомеханіки поляризовних неферомагнітних тіл за врахування ...

BBB"""

result = cleanText.cleanUp(text, file, [modules: 'nanu'])

assert result == "AAA\nBBB"
