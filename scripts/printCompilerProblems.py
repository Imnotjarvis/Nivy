#!/usr/bin/env python3
"""Prints the Kotlin diagnostics in Gradle's problems report.

With allWarningsAsErrors on, a failing compile only logs "warnings found and -Werror specified";
the warnings themselves are in build/reports/problems/problems-report.html.
Usage: scripts/printCompilerProblems.py [report.html]
"""
import html
import re
import sys

path = sys.argv[1] if len(sys.argv) > 1 else "build/reports/problems/problems-report.html"
try:
    text = open(path, encoding="utf-8", errors="replace").read()
except FileNotFoundError:
    print(f"no problems report at {path}")
    sys.exit(0)

for escaped, plain in (('\\"', '"'), ("\\n", "\n"), ("\\u003c", "<"), ("\\u003e", ">"),
                       ("\\u0026", "&"), ("\\u0027", "'"), ("\\/", "/")):
    text = text.replace(escaped, plain)
text = html.unescape(text)

found = []
pattern = r"\[[A-Z][A-Z0-9_]{4,}\][^\"\n<]{0,400}|Location: [^\"\n<]{0,300}"
for match in re.finditer(pattern, text):
    line = match.group(0).strip()
    if line not in found:
        found.append(line)

if found:
    print("\n".join(found))
else:
    # The format changed: show where the compiler problems are so the pattern can be updated.
    at = text.find("Kotlin")
    print(f"no diagnostics matched in {path} ({len(text)} chars); around the first 'Kotlin':")
    print(text[max(at - 200, 0):at + 1500])
