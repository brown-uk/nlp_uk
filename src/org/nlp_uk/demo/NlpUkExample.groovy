package org.nlp_uk.demo

@Grab(group='org.languagetool', module='language-uk', version='3.2')

import java.net.*
import org.languagetool.*
import org.languagetool.rules.*
import org.languagetool.tokenizers.*
import org.languagetool.language.*
import org.languagetool.uk.*


class NlpUkExample {
	JLanguageTool langTool = new JLanguageTool(new Ukrainian())
//		JLanguageTool langTool = new MultiThreadedJLanguageTool(new Ukrainian());
	SRXSentenceTokenizer stokenizer = new SRXSentenceTokenizer(new Ukrainian());
	
	NlpUkExample() {
	}

	def check(text) {
		List<RuleMatch> matches = langTool.check(text)

		for (RuleMatch match : matches) {
			System.out.println("Можлива помилка в рядку " +
					match.getLine() + ", стовпчик " +
					match.getColumn() + ": " + match.getMessage());
			System.out.println("Пропозиція виправлення: " +
					match.getSuggestedReplacements());
		}
	}

	def tokenizeText(String text) {
		List<String> tokenized = stokenizer.tokenize(text);

		def sb = new StringBuilder()
		for (String sent: tokenized) {
			sb.append("=====================>\n");
			sb.append(sent); //.replaceAll(/(.)\n/, '$1|'));
			sb.append("\n<====================\n");
		}
		return sb.toString()
    }
	
	def analyzeText(String text) {
		List<AnalyzedSentence> analyzedSentences = langTool.analyzeText(text);

		def sb = new StringBuilder()
		for (AnalyzedSentence analyzedSentence : analyzedSentences) {
			sb.append(analyzedSentence);
		}
		return sb.toString()
	}
	
	def analyzePageTyzhden() {
		new URL("http://tyzhden.ua/Columns/50/155282").text	
		
	
	}
	
	
	
	static void main(String[] argv) {
		def nlpUk = new NlpUkExample()
		
		
		def textToSplit = 
'''
Це — перше речення. А ось це, друге. А 
це третє.
Їздять дорогами, що побудував І.В. Петренко. Ходять 
пішки на майданчиках по 2 кв. м.
'''.trim()
		
		def tokenized = nlpUk.tokenizeText(textToSplit);
		
		println("Демонстрація розбивання на речення:")
		println(tokenized)

		
		
		def textToAnalyze = 
'''
"Це — перше речення. А ось це, друге."
'''.trim()

		def analyzed = nlpUk.analyzeText(textToAnalyze);
		println("\n\n")
		println("Демонстрація аналізу тексту:")
		println(analyzed)
	
		
				
		def textToCheck = "Їздять по поломаним мостам."
		
		println("\n\n")
		println("Демонстрація перевірки речень:")
		
		nlpUk.check(textToCheck)
		
	}
	
}
