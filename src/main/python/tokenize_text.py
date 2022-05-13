#!/usr/bin/python3

# This script allows to tokenize Ukrinian text by sentences or words
# by invoking TokenizeText.groovy that uses LanguageTool API
# groovy >= 3.0 (http://www.groovy-lang.org) needs to be installed and in the path
# Usage: tokenize_text.py <inputfile>

import os
import sys
import subprocess
import threading
import argparse


SCRIPT_PATH=os.path.dirname(__file__) + '/../groovy/ua/net/nlp/tools'
ENCODING='utf-8'


in_txt = None

parser = argparse.ArgumentParser()
parser.add_argument("-v", help="Verbose",  action="store_true")
parser.add_argument("input_file", default=None, type=str, help="Input file")
parser.add_argument("-o", "--output_file", default=None, type=str, help="Output file")

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
cmd = [groovy_cmd, SCRIPT_PATH + '/TokenizeText.groovy', '-i', '-', '-w', '-u']


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

