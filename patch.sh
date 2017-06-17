#!/bin/bash
if [ ! -d gen.antlr3-smali ]; then
	git clone https://github.com/JesusFreke/smali gen.antlr3-smali
	cp -r smali.patch/* gen.antlr3-smali
	cd !$
	gradle build -x test
fi
gradle build -x test 
unzip -o dist/build/distributions/gumtree-20170617-2.1.0-SNAPSHOT.zip
cp -r gumtree-20170617-2.1.0-SNAPSHOT/* /usr/local/
gumtree diff DuplicateVirtualMethods.smali DuplicateVirtualMethods.smali 
