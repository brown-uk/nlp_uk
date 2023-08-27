package ua.net.nlp.tools.tag

import org.languagetool.AnalyzedToken
import org.languagetool.AnalyzedTokenReadings
import org.languagetool.JLanguageTool

import groovy.transform.CompileStatic

@CompileStatic
class ModZheleh {
    private final JLanguageTool langTool
    private final Map<String, Set<String>> EXTRA_WORD_MAP = [:]
    
    ModZheleh(JLanguageTool langTool) {
        this.langTool = langTool
        loadExtraWords()
    }
    
    private static String adjustZheleh(String text) {
        // і у сімї
        // подвірє
        text = text.replaceAll(/(?ui)([р])([є])$/, '$1\'я')

        // next are tagged as ":bad" in tagger
//        text = text.replaceAll(/(?ui)ь([сц])(к)/, '$1ь$2')
//        text = text.replaceAll(/(?ui)-(же|ж|би|б)/, ' $1')
    }

    @CompileStatic
    AnalyzedTokenReadings adjustTokens(AnalyzedTokenReadings tokenReadings, AnalyzedTokenReadings[] tokens, int idx) {
        AnalyzedToken origAnalyzedToken = tokenReadings.getReadings().get(0)
        
        boolean syaIsNext = idx < tokens.size()-1 && tokens[idx+1].getToken() == 'ся'

        if( ( tokenReadings.isPosTagUnknown() || syaIsNext )
            && origAnalyzedToken.token =~ /[а-яіїєґА-ЯІЇЄҐ]/ ) {

            String adjustedToken = adjustZheleh(origAnalyzedToken.token)

            if( syaIsNext ) {
                tokenReadings = langTool.getLanguage().getTagger().tag([adjustedToken + 'ся']).get(0)
                // println "trying verb:rev $adjustedToken " + tokenReadings.getReadings()
            }

            if( tokenReadings.isPosTagUnknown() ) {
                tokenReadings = langTool.getLanguage().getTagger().tag([adjustedToken]).get(0)
            }

            if( ! tokenReadings.isPosTagUnknown() ) { 
                return generateTokens(tokenReadings, origAnalyzedToken)
            }
        }

        if( tokenReadings.isPosTagUnknown() ) {
            String cleanToken = tokenReadings.getCleanToken()
            String lowerCleanToken = cleanToken.toLowerCase()
            Set<String> posTags = EXTRA_WORD_MAP[lowerCleanToken]
            if( posTags ) {
                def readings = posTags.collect { posTag ->
                    new AnalyzedToken(origAnalyzedToken.token, posTag, lowerCleanToken)
                }
                return new AnalyzedTokenReadings(readings, tokenReadings.startPos)
            }
            
            // оподаткованє
            // оподатковання
            String adjustedToken = cleanToken
                .replaceFirst(/ьованє/, 'ювання')
                .replaceFirst(/ованє/, 'ування')
                .replaceFirst(/ьова/, 'юва')
                .replaceFirst(/ова/, 'ува')
                
            tokenReadings = langTool.getLanguage().getTagger().tag([adjustedToken]).get(0)
            if( ! tokenReadings.isPosTagUnknown() ) {
                return generateTokens(tokenReadings, origAnalyzedToken)
            }
           
            // прийіздив
            adjustedToken = cleanToken.replaceFirst(/(?ui)йі/, 'ї')
            tokenReadings = langTool.getLanguage().getTagger().tag([adjustedToken]).get(0)
            if( ! tokenReadings.isPosTagUnknown() ) {
                return generateTokens(tokenReadings, origAnalyzedToken)
            }

            // пізнїйше
            adjustedToken = cleanToken.replaceAll(/(?ui)([бвгґджзклмнпрстфхцчшщ])їй/, '$1і')
            tokenReadings = langTool.getLanguage().getTagger().tag([adjustedToken]).get(0)
            if( ! tokenReadings.isPosTagUnknown() ) {
                return generateTokens(tokenReadings, origAnalyzedToken)
            }
            
            // note - this change affects rules below
            // Італїйцї
            adjustedToken = cleanToken.replaceAll(/(?ui)([бвгґджзклмнпрстфхцчшщ])ї/, '$1і')
            tokenReadings = langTool.getLanguage().getTagger().tag([adjustedToken]).get(0)
            if( ! tokenReadings.isPosTagUnknown() ) {
                return generateTokens(tokenReadings, origAnalyzedToken)
            }
            cleanToken = adjustedToken
            
            // літопись
            adjustedToken = cleanToken.replaceFirst(/пись$/, 'пис')
            tokenReadings = langTool.getLanguage().getTagger().tag([adjustedToken]).get(0)
            if( ! tokenReadings.isPosTagUnknown() ) {
                return generateTokens(tokenReadings, origAnalyzedToken)
            }
           
            // галицкій
            adjustedToken = cleanToken.replaceFirst(/(?ui)([сц])(к)/, '$1ь$2')
            tokenReadings = langTool.getLanguage().getTagger().tag([adjustedToken]).get(0)
            if( ! tokenReadings.isPosTagUnknown() ) {
                return generateTokens(tokenReadings, origAnalyzedToken)
            }
           
            // питаннє
            // прикриттє
            adjustedToken = cleanToken.replaceFirst(/н?нє/, 'ння')
                .replaceFirst(/л?лє/, 'лля')
                .replaceFirst(/т?тє/, 'ття')
            tokenReadings = langTool.getLanguage().getTagger().tag([adjustedToken]).get(0)
            if( ! tokenReadings.isPosTagUnknown() ) {
                return generateTokens(tokenReadings, origAnalyzedToken)
            }
            
            // засїданя
            adjustedToken = cleanToken.replaceFirst(/([а])([нлт])([яю])/, '$1$2$2$3')
            tokenReadings = langTool.getLanguage().getTagger().tag([adjustedToken]).get(0)
            if( ! tokenReadings.isPosTagUnknown() ) {
                return generateTokens(tokenReadings, origAnalyzedToken)
            }

            // пірям - пір'ям 
            adjustedToken = cleanToken.replaceAll(/(?ui)([бвпмфр])([юяє])/, '$1\'$2')
            // сьвятити - святити
            adjustedToken = adjustedToken.replaceAll(/(?ui)([сцз])ь([бвпмф])([ія])/, '$1$2$3')
            tokenReadings = langTool.getLanguage().getTagger().tag([adjustedToken]).get(0)
            if( ! tokenReadings.isPosTagUnknown() ) {
                return generateTokens(tokenReadings, origAnalyzedToken)
            }
        }

        return tokenReadings
    }

    @CompileStatic
    private AnalyzedTokenReadings generateTokens(AnalyzedTokenReadings tokenReadings, AnalyzedToken origAnalyzedToken) {
        // put back original word
        for(int i=0; i<tokenReadings.getReadings().size(); i++) {
            AnalyzedToken token = tokenReadings.getReadings().get(i);
            String posTag = token.getPOSTag()
            tokenReadings.getReadings().set(i, new AnalyzedToken(origAnalyzedToken.token, posTag, token.lemma))
        }
        return tokenReadings
    }

    private void loadExtraWords() {
        EXTRA_WORDS.eachLine { line ->
            line = line.replaceFirst(/\h*#.*/, '').trim()
            if( ! line )
                return
                
            try {
                def parts = line.split(/ /)
                def words = parts[0].split(/\|/)

                words.each {
                    EXTRA_WORD_MAP[it] = parts[1].split(/\|/) as Set
                }
            }
            catch(Exception e) {
                System.err.println("Failed to parse extra word: " + line)
            }
        }
    }
    
    
    private static final String EXTRA_WORDS =
"""
из|ізміж|изо|зперед|зпосеред|зпроміж|іс|зь|со|ізпід|зпоза|из-за|з-по-за|зачерез|ізпоміж|де-з prep
єсли|сли|єсли-б|єсли-би|если|еси|ежели conj:subord                                                      # якщо
єсьмо|єсмь|єсь|єсте|єсьм|єст verb:imperf:pres:s:1/2/3//p:1/2/3                                          # є
єще|еще|аще adv
міжь|мїжь|между|міжо|міжтим|поміжь prep
небудь|будьто-би|будьто|будьтоби|будьтеж|будь-щобудь|будь-то-би|де-будь part                            # будь
по-за|по-при|под|спопід|спонад|понадь prep
однакож|ажь|всеж|ож|оже|вжеж|тутже|атакож|якож|якоже|опісляж adv
тамже adv
жь|уж|иже|нехайже|то-жь|хібаж part
хочь|хотьби|хочь-би|хочаби|хочьби part|conj:coord|conj:subord                                          # хоч
ино|инак adv:&pron:def
албо|убо|не-аби part
абим|аби-сьте|аби-сте|аби-в conj:subord
нетільки|только part|adv|conj:subord|conj:coord
а-ле conj:coord|intj                                                                                   # але
длятого|длячого|для-того|адля adv
низше adv:compc
близше adv:compc
дїйстно adv

# что – што|что|чтоб|чтош|чож
# де-що|що-ино|мало-що|що-раз-то|за-що|що-в|що-аж|що-йно|так-що|не-знати-що
# ли – ли|или|што-лї
# ось – ось-що|ось-як|ось-які|ось-якої|ось-який|ось-яке|ось-такі|ось-така|ось-то|ось-якою|ось-такій|ось-таку|ось-такого|ось-такиб|ось-таке
# то(от, те, та) – тоє|про-те|тот|про-то|то-та|томуто|то-то-то|от-іще|от-що|не-то|такото|от-які|от-тутечки|от-тут(1).
# де – денекотрі|денекотрих|денеДе|де-яку|де-якої|де-якими|де-яка|де-чому|де-неде|де-колидесь|де-в-чім|де-в-чому|де-будь
# тогож(20) - tegoż, хтож(11) - któż, чомуж(11) - czemuż, деж(11) - gdzież, тоїж|туж|якіж(4) - jakiż, яж|такіж(2) - takiż, тійже(2) - tejże, йогож(2) - jegoż, |тіж|такимиж|тимже|такогож|такімже|тойже|длячого-ж
"""

}
