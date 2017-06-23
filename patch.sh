#!/bin/bash
if [ ! -d gen.antlr3-smali ]; then
	git clone https://github.com/JesusFreke/smali gen.antlr3-smali
	cp -r smali.patch/* gen.antlr3-smali
	cd !$
	gradle build -x test -x checkstyleMain
fi
gradle build -x test -x checkstyleMain
unzip -o dist/build/distributions/gumtree-20170617-2.1.0-SNAPSHOT.zip
sudo cp -r gumtree-20170617-2.1.0-SNAPSHOT/* /usr/local/
sudo cp gumtree /usr/local/bin
gumtree diff DuplicateVirtualMethods.smali DuplicateVirtualMethods.smali 
