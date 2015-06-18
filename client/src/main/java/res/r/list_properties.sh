#!/bin/sh

find . -name '*.java' | xargs grep -o 'System.getProperty.*'|
grep -v '^./samples' | grep -o 'fr/labri.gumtree.*'|
sed 's/System.getProperty("\([^"]*\)".*/\1/' | tr / . |
sed 's/\(.*\).java:\(.*\)/"\2 (\1)",/' | sort -u
