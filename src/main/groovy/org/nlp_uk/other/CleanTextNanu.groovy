package org.nlp_uk.other


import groovy.transform.Field
import groovy.transform.TupleConstructor
import groovy.transform.CompileStatic
import org.languagetool.tagging.uk.*
import org.languagetool.*
import java.util.regex.Pattern


class CleanTextNanu {

	Pattern SPECIAL_REGEX_CHARS = Pattern.compile(/[{}()\[\].+*?^$\\|]/)
	String pntf1 = /[ ]*((?:[12][0-9]{3} +)?[“"«]?[а-яіїєґА-ЯІЇЄҐ][^\n]*)[ ][0-9]{1,3}$/
	String pntf2 = /^[ ]*[0-9]{1,3}[ ]*([“"«]?[а-яіїєґА-ЯІЇЄҐ][^\n]*)/
	String pntf3 = /(?:[ ]*([А-ЯІЇЄҐ][а-яіїєґ'-]+ (?:[А-ЯІЇЄҐ]\.)(?: ?[А-ЯІЇЄҐ]\.)?(?:, [А-ЯІЇЄҐ][а-яіїєґ'-]+ (?:[А-ЯІЇЄҐ]\.)(?: ?[А-ЯІЇЄҐ]\.)?)*) *)/
	String pntf4 = /(?:[ ]*((?:[А-ЯІЇЄҐ]\.)(?: ?[А-ЯІЇЄҐ]\.)? [А-ЯІЇЄҐ][а-яіїєґ'-]+(?:, (?:[А-ЯІЇЄҐ]\.)(?: ?[А-ЯІЇЄҐ]\.)? [А-ЯІЇЄҐ][а-яіїєґ'-]+)*) *)/
	Pattern pageNumTitleFooter = Pattern.compile(/^(?:/ + pntf1 + /|/ + pntf2 + /|/ + pntf3 + /|/ + pntf4 + /)$/, Pattern.MULTILINE)


	PrintStream out

	CleanTextNanu(PrintStream out) {
		this.out = out
	}


	void println(String txt) {
		out.println(txt)
	}


	String removeMeta(String text, File file, def options) {
		def footers = findFooter(text)

		def footerMatchers = null
		if( footers ) {
//			println "\tfooters: " + footers
			footerMatchers = footers.collect { footer ->
				//            Pattern.compile("^(?:[ ]*(${footer.key})[ ]+[0-9]+\$|^[ ]*[0-9]+[ ]*(${footer.key}))\$", Pattern.MULTILINE)
				def footerKey = SPECIAL_REGEX_CHARS.matcher(footer.key).replaceAll('\\\\$0')
				footerKey = footerKey.replaceAll(/ {8,}/, ' {8,}')
//				footerKey = footerKey.replaceAll(/ {10,}/, ' {10,}')
//				            println"\tregex: " + footerKey
				Pattern.compile("^(?:[ ]*(${footerKey})([ ]+[0-9]+| *)\$|^([ ]*[0-9]+[ ]*| *)(${footerKey}))\$", Pattern.MULTILINE)
			}
			//      println "match: " + footerMatcher.matcher(text).find()

//            println"\tfooterMatchers: " + footerMatchers
		}

		text = removeFooters(text, file, options, footerMatchers)
		text = removeAbstracts(text)
	}

	def removeAbstracts(String text) {
	    return text.replaceAll(/(?si)\n *(abstract|resume|Literatura|(Джерела та|Використана|Цитована) Література|СПИСОК ЛІТЕРАТУРИ)\n.+?(\n\n|\n?$)/, '\n')
	        .replaceAll(/(?si)\n *(abstract|resume|Literatura|(Джерела та|Використана|Цитована) Література|СПИСОК ЛІТЕРАТУРИ)\n.+?(\n\n|\n?$)/, '\n')
	}


    def findNonNullGroup(m) {
        for(int i=1;i<=m.groupCount(); i++) {
            if( m.group(i) )
                return m.group(i)
        }
        assert null, "No group in $m (group count ${m.groupCount()}"
    }

	def findFooter(String text) {
		def m = pageNumTitleFooter.matcher(text)
		def candidates = [:].withDefault { 0 }
		while( m.find() ) {
//			      println "found group: " + m.group()
			def key = findNonNullGroup(m)
			key = key.trim()
			if( ! (key =~ /Таблиця|група|ЗМІСТ №|Рис\.|.*?\.{6,}/) ) {
				key = key.replaceAll(/ {8,}/, '        ')
				candidates[ key ] += 1
			}
		}
//		println "\tcandidates: " + candidates
		return candidates.findAll { it.value > 2 }
	}

	String removeFooters(String text, File file, def options, def footerMatchers) {

		String[] lines = text.split(/\n\r?/)

		List<String> newLines = []

		List<String> suspectWeak = []
		boolean justCut = false

		for(String line: lines) {
			//      println ":: "  + line

			if( line.trim().isEmpty()
			|| line.matches(/   \s{30,}?[0-9]+/) ) {
			    if( ! justCut ) {
				    suspectWeak << line
				    continue
				}

//				justCut = false
				continue
			}
			else {
				justCut = false
			}

			if( line.matches(/([0-9]*\s{10,})?© .*/)
			|| line.matches(/([0-9]*\s{10,})?ISSN [0-9]+.*/)
			|| (footerMatchers && footerMatchers.find{ it.matcher(line).matches() } ) ) {

//                println "\tfound footer: $line"
				suspectWeak.clear()
				justCut = true
				continue
			}

			if( suspectWeak ) {
				//            println "adding weak: $suspectWeak"
				newLines += suspectWeak
			}

			if( justCut ) {
				if( line.matches(/(Рис\. |Таблиця ).*/) ) {
					newLines << "\n"
				}
			}

			//        println "adding: $line"
			newLines << line

			suspectWeak.clear()
			justCut = false
		}

		if( suspectWeak ) {
			//        println "adding weak2: $suspectWeak"
			newLines += suspectWeak
		}

		if( text.endsWith("\n") ) {
			//      println "adding new line"
			newLines << ""
		}

		//    println "=$newLines="
		text = newLines.join("\n")


		//    def m = pageNumFooter.matcher(text)
		//
		//    if( m.find() ) {
		//        println "\tremoving page-num footers..."
		//        println "\t" + m
		//        text = m.replaceAll("\n")
		//    }

		return text
	}


}