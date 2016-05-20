#!/usr/bin/python3

# This script allows to tokenize Ukrinian text by sentences or words
# by invoking TokenizeText.groovy that uses LanguageTool API
# groovy (http://www.groovy-lang.org) needs to be installed and in the path

import sys
import subprocess
import threading

ENCODING='utf-8'


if len(sys.argv) > 1:
    with open(sys.argv[1], encoding=ENCODING) as a_file:
        in_txt = a_file.read()
else:
    print("Using sample text...", file=sys.stderr)
    in_txt = 'Ми ходили туди-сюди.'





def print_output(p):

#    error_txt = p.stderr.read().decode(ENCODING)
#    if error_txt:
#        print("stderr: ", error_txt, "\n", file=sys.stderr)

    print("output: ", p.stdout.read().decode(ENCODING))



cmd = ['groovy', 'TokenizeText.groovy', '-i', '-', '-o', '-', '-w', '-q']
p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stdin=subprocess.PIPE, stderr=subprocess.PIPE)

threading.Thread(target=print_output, args=(p,)).start()



p.stdin.write(in_txt.encode(ENCODING))
p.stdin.close()

