#!/usr/bin/python3

# This script allows to tag Ukrinian text
# by invoking TagText.groovy that uses LanguageTool API
# groovy (http://www.groovy-lang.org) needs to be installed and in the path
# Usage: tag_text.py <inputfile>

import os
import sys
import subprocess
import threading

ENCODING='utf-8'
SCRIPT_PATH=os.path.dirname(__file__) + '/../groovy/org/nlp_uk/tools'

in_txt = None

for arg in sys.argv[1:]:
    if not arg.startswith('-'):
        with open(arg, encoding=ENCODING) as a_file:
            in_txt = a_file.read()
        break

if not in_txt:
    print("Usage: " + sys.argv[0] + " [-f] <inputfile>", file=sys.stderr)
    sys.exit(1)



def print_output(p):

#    error_txt = p.stderr.read().decode(ENCODING)
#    if error_txt:
#        print("stderr: ", error_txt, "\n", file=sys.stderr)

    print("output: ", p.stdout.read().decode(ENCODING))

# technically only needed on Windows
my_env = os.environ.copy()
my_env["JAVA_TOOL_OPTIONS"] = "-Dfile.encoding=UTF-8"


groovy_cmd = 'groovy.bat' if sys.platform == "win32" else 'groovy'
cmd = [groovy_cmd, SCRIPT_PATH + '/TagText.groovy', '-i', '-', '-o', '-', '-q']
if '-f' in sys.argv:
    cmd.append('-f')

p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stdin=subprocess.PIPE, stderr=subprocess.PIPE, env=my_env)

threading.Thread(target=print_output, args=(p,)).start()



p.stdin.write(in_txt.encode(ENCODING))
p.stdin.close()

