gumtree
=======

GumTree is a neat tool to visualize differences between source code files.

## Description

Compared to the classic diff tools (such as Meld, KDiff3, etc...), GumTree has two particularties :

* it works on a tree structure rather than a text structure,
* it can detect moved or renamed elements in addition of deleted and inserted elements.

Thanks to these two features, GumTree is able to compute and show kick-ass diffs that will allow you to understand faster what changed between the two code files. GumTree comes with two GUIs (a web based and a Java swing based), and is compatible with several languages: Java, JavaScript, R and C. More languages are coming soon, if you want to help contact [me](www.labri.fr/perso/falleri).

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
