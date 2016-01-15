package org.nlp_uk.demo

import groovy.util.slurpersupport.NodeChild
import groovy.xml.XmlUtil;
@Grab(group='org.languagetool', module='language-uk', version='3.3-SNAPSHOT')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.2')

import groovyx.net.http.*

import org.languagetool.*
import org.languagetool.rules.*
import org.languagetool.tokenizers.*
import org.languagetool.language.*
import org.languagetool.uk.*


class NlpUkWebPage {
	JLanguageTool langTool = new JLanguageTool(new Ukrainian())
	//		JLanguageTool langTool = new MultiThreadedJLanguageTool(new Ukrainian());
	SRXSentenceTokenizer stokenizer = new SRXSentenceTokenizer(new Ukrainian());

	NlpUkWebPage() {
	}

	def check(text) {
		println("Перевірка тексту:")

		List<RuleMatch> matches = langTool.check(text)

		if( matches.isEmpty() ) {
			println("Помилок не знайдено")
			return
		}

		for (RuleMatch match : matches) {
			println("Можлива помилка в рядку " +
					match.getLine() + ", стовпчик " +
					match.getColumn() + ": " + match.getMessage());
			println("Пропозиція виправлення: " +
					match.getSuggestedReplacements());
		}
	}

	def analyzeText(String text) {
		return langTool.analyzeText(text);
	}

	@Grab('net.sourceforge.nekohtml:nekohtml:1.9.16')
	def getPageTyzhden() {
		def parser = new org.cyberneko.html.parsers.SAXParser()
		
		def url = "http://tyzhden.ua/Economics/155447/PrintView".toURL()
		// to prevent redirect to (missing) mobile page
		def pageText = url.getText(
			requestProperties: ['User-Agent': 'Mozilla/5.0 (X11; Linux i686; rv:10.0) Gecko/20100101 Firefox/10.0'])
		
		def htmlDom = new XmlParser( parser ).parseText( pageText )

		def allP = htmlDom.'**'.P.collect { item ->
			item.text()
		}

		return allP.join("\n\n")
	}



	static void main(String[] argv) {
		def nlpUk = new NlpUkWebPage()

		def text = nlpUk.getPageTyzhden()

		//		println("text: " + text)

		def analyzedSentences = nlpUk.analyzeText(text)
		
//		println(analyzedSentences)

		println(String.format("Знайдено %d речень в тексті", analyzedSentences.size()) )

		def freqMap = [:]
		def posMap = [:]
		for(sentence in analyzedSentences) {
			for(tokenReadings in sentence.getTokens()) {
				for(analyzedToken in tokenReadings) {
					def lemma = analyzedToken.getLemma()
					if( ! lemma )
						continue

					int cnt = freqMap.get(lemma, 0)
					freqMap[lemma] = cnt+1
					posMap[lemma] = analyzedToken.getPOSTag().split(":")[0]
				}
			}
		}

		freqMap = freqMap.sort({ -it.value })

		freqMap.each { lemma, count ->
			println(String.format("лема: %s, частина: %s, вживань: %d", lemma, posMap[lemma], count) )
		}
	}


}
