package ua.net.nlp.tools

public enum OutputFormat { txt, xml, json, vertical, conllu
    
    String getExtension() {
        return this == vertical ? "vertical.txt"
             : this == conllu ? "conllu.txt"
                 : this.name()
    }
    
}
