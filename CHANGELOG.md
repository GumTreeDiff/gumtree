# Changelog of GumTree

## v4.0.0 (Ginkgo)
* New native distribution with zero dependencies apart from JDK 17
* Vastly improved swing client with directory comparison, syntax highlighting, and more
* Update monaco
* Update bootstrap
* Update mergely
* New Acorn based tree generator for JS
* New tree-sitter based tree generator with support for a wide range of languages with both a python and a java implementation.
* Docker image now comes installed with the Acorn and tree-sitter parsers
* Very light docker image with the native distribution
* Benchmark now displays size differences on a boxplot
* Dotdiff displays actions on nodes using colors
* Default build command no longer test generators requiring native tools
* Test reports are uploaded in the CI
* Added citation file
* Simple is now the default matcher
* Added auto matchers which automatically select the best parameters for the input
* Native XML support
* Native YAML support
* Native JSON support
* Fix shortcuts in vanilla web diff view
* Replaced snakehtml by j2html for webdiff
* Totally reworked build system

## v3.0.0 (Ficus)

* Linereader can convert offset to line/column
* Maven packages now include javadoc and source
* New static html diff client (htmldiff) thanks to algomaster99
* Improved API documentation
* Monaco editor diff view now has 100% height 
* Fix missing positions in srcML parser
* Switched from Travis to Github actions for the CI
* Reworked benchmark to use defects4j and test for runtime and size regressions
* New parso-based python 3 compatible parser, replace the old pythonparser
* Docker image is now based upon ubuntu focal LTS
* Update of dependencies (ph-css, monaco-editor, rhino, javaparser, jdt, srcml)
* Integrated monaco native diff editor
* Fix position problem in javaparser generator
* Fix method invocation handling in jdt generator
* Add shortcuts and menubar to all webdiff views except mergely 
* Webdiff directory comparator only displays tree-diff buttons for files where parser are availables
* Webdiff command is compatible with -g option (except from the directory comparator)
* Update vanilla diff viewer to bootstrap v4
* Update mergely diff viewer to newest version of mergely
* TreeInsert and TreeDelete actions added
* Add brand new monaco-based diff viewer
* Improved docker container that can be integrated with git
* Update jdt, rhino and ph-css parsers
* Fix varargs representation in JDT
* Fix index problem in Chawathe's edit script generator
* Theta matchers are now installed by default

## v2.1.2 (Elm)
* New dockerfile to run GumTree
* Python tree generator
* JavaParser tree generator
* Several minor bugfixes

## v2.1.1 (Dogwood)
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
