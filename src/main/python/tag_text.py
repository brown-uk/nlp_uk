#!/usr/bin/python3

# This script allows to tag Ukrinian text
# by invoking TagText.groovy that uses LanguageTool API
# JDK > 17 and groovy >= 4.0 (http://www.groovy-lang.org) needs to be installed and in the path
# Usage: tag_text.py <inputfile>

import os
import sys
import subprocess
import threading
import argparse


ENCODING='utf-8'
SCRIPT_PATH=os.path.dirname(__file__) + '/../groovy/ua/net/nlp/tools'

in_txt = None

parser = argparse.ArgumentParser()
parser.add_argument("-v", help="Verbose",  action="store_true")
parser.add_argument("-g", help="Disambiguate and print first token only",  action="store_true")
parser.add_argument("input_file", default=None, type=str, help="Input file")
parser.add_argument("-o", "--output_file", default=None, type=str, help="Output file")
#parser.add_argument("-gr", "--disambiguationRate", default=None, type=str, help="Show a disambiguated token ratings")
#parser.add_argument("-t", "--tokenFormat", default=None, type=str, help="Use <token> format (instead of <tokenReading>)")
#parser.add_argument("-t1", "--singleTokenOnly", default=None, type=str, help="rint only one token per reading (-g is recommended with this option)")

args = parser.parse_args()


with open(args.input_file, encoding=ENCODING) as a_file:
    in_txt = a_file.read()


def print_output(p):

    print("output: ", p.stdout.read().decode(ENCODING))

def print_error(p):

    error_txt = p.stderr.read().decode(ENCODING)
    if error_txt:
        print("stderr: ", error_txt, "\n", file=sys.stderr)


# technically only needed on Windows
my_env = os.environ.copy()
my_env["JAVA_TOOL_OPTIONS"] = "-Dfile.encoding=UTF-8"


groovy_cmd = 'groovy.bat' if sys.platform == "win32" else 'groovy'
cmd = [groovy_cmd, SCRIPT_PATH + '/TagText.groovy', '-i', '-']

if args.g:
    cmd.append('-g')
    cmd.append('-t1')

if args.output_file:
    cmd.append('-o')
    cmd.append(args.output_file)
else:
    cmd.append('-o')
    cmd.append('-')

if args.v:
    print('Running: ' + str(cmd))
else:
    cmd.append('-q')


p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stdin=subprocess.PIPE, stderr=subprocess.PIPE, env=my_env)

threading.Thread(target=print_output, args=(p,)).start()
threading.Thread(target=print_error, args=(p,)).start()


p.stdin.write(in_txt.encode(ENCODING))
p.stdin.close()

