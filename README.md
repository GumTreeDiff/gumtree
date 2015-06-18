GumTree
=======

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

### From downloadable Jar

Download the [gumtree.jar](https://drive.google.com/file/d/0B0S2lIHclUdwOUVEXzVKUjVWM1U/view?usp=sharing) file and that's it! For a more confortable use, do not hesitate to put it in a directory in the path and to create a shell (or bat) script that will create a shortcurt to the command `java -jar gumtree.jar`.

### From source

Note that you need Maven to compile GumTree. You can clone GumTree with the following command: `git clone https://github.com/GumTreeDiff/gumtree.git`. Then go into the cloned folder and run the command `mvn install`. The `gumtree.jar` is generated in the `client/target` folder.

## Usage

Using GumTree is really easy... The classical command is `java -jar gumtree.jar file1.java file2.java`. This command will compute the diff between file1.java and file2.java, and launch a local web server that will show you the output of the diff. This server is accessible via the URL [http://localhost:4754/](http://localhost:4754/). If you want to try the Java swing GUI, you can use the command `java -jar gumtree.jar --output swing file1.java file2.java` instead.

### Git integration

To use GumTree within git you can use the following command : 

`git difftool -x gumtree --d`

You can also set gumtree as the default diff tool by adding this content in `~/.gitconfig` : 

```
[diff]
tool = gumtree

[difftool "gumtree"]
cmd = gumtree $LOCAL $REMOTE
```

and then use `git difftool -d`.

### Mercurial integration
You can also set gumtree as a diff tool by adding this content in `~/.hgrc`: 

```
[extensions]
hgext.extdiff =

[extdiff]
cmd.gumtree =
```
and then use `hg gumtree`.
