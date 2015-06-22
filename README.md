GumTree
=======

![Build status](https://travis-ci.org/GumTreeDiff/gumtree.svg?branch=master)

GumTree is a complete framework to deal with source code as trees and compute differences between them. It includes possibilities such as:
* converting a source file into a language-agnostic tree format
* export the produced trees in various formats
* compute the differences between the trees
* export these differences in various formats
* visualize these differences graphically

Compared to classical code differencing tools, it has two important particularities:
* it works on a tree structure rather than a text structure,
* it can detect moved or renamed elements in addition of deleted and inserted elements.

We already deal with a wide range of languages: Java, C, JavaScript and Ruby. More languages are coming soon, if you want to help contact [me](www.labri.fr/perso/falleri).

## Citing GumTree

We are researchers, therefore if you use GumTree in an academic work we would be really glad if you cite our seminal paper using the following bibtex:

```
@inproceedings{DBLP:conf/kbse/FalleriMBMM14,
  author    = {Jean{-}R{\'{e}}my Falleri and
               Flor{\'{e}}al Morandat and
               Xavier Blanc and
               Matias Martinez and
               Martin Monperrus},
  title     = {Fine-grained and accurate source code differencing},
  booktitle = {{ACM/IEEE} International Conference on Automated Software Engineering,
               {ASE} '14, Vasteras, Sweden - September 15 - 19, 2014},
  pages     = {313--324},
  year      = {2014},
  url       = {http://doi.acm.org/10.1145/2642937.2642982},
  doi       = {10.1145/2642937.2642982}
}
```

## Installation

### From source

Note that you need Maven to compile GumTree. You can clone GumTree with the following command: `git clone https://github.com/GumTreeDiff/gumtree.git`. Then go into the cloned folder and run the command `mvn install`. The `gumtree.jar` is generated in the `client/target` folder.

### From maven

Coming soon.

## Usage

The class to run any command line in GumTree is `fr.labri.gumtree.client.Run`. You can check out the help of this class to discover the main commands.

For instance `java fr.labri.gumtree.client.Run webdiff file1.java file2.java` will compute the diff between file1.java and file2.java, and launch a local web server that will show you the output of the diff. This server is accessible via the URL [http://localhost:4754/](http://localhost:4754/).

