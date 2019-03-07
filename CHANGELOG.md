# Changelog of GumTree

## v2.1.3
* Fix position problem in javaparser generator
* Fix method invocation handling in jdt generator
* Webdiff command is compatible with -g option
* TreeInsert and TreeDelete actions added

## v2.1.2
* New dockerfile to run GumTree
* Python tree generator
* JavaParser tree generator
* Several minor bugfixes

## v2.1.1
* New integration with antlr4 grammars
* Add matlab grammar

## v2.1.0 (Cedar)
* Improve the web diff view
* Integrate mergely to get a text diff from the web view
* Fix actions text formatter
* Add command list in Run client
* Fix bug to view a diff in the web client
* Add benchmark module to check output and performance of algorithms
* Add regression test to check the output of the matching algorithms
* Improve the tree API
* Automatically produce nightlies
* Add new srcML tree generator that can deal with C++, C, C# and Java files
* Add new css tree generator based on ph-css
* Fix no label bug in ruby tree generator
* Add custom options to furnish srcml and cgum paths
* Build script can exclude tests requiring an external tool
* Use sparkjava for the web client instead of NanoHTTPd
* Improve annotations for generators, clients and matchers
* Remove oudated css antlr tree generator
