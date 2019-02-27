# GumTree dockerfile

## Installation

First you need to build the container. Go to the `docker` folder and compile the image: `docker build . -t gumtree`.

## Usage

Now you can use GumTree container. You need to:
* bind a `/diff` volume where you files of interest are located,
* bind the `4567` port to be able to use webdiff

A sample command line is `docker run -v /my/folder:/diff -p 4567:4567 gumtree webdiff left.rb right.rb`. You can consult the diff at the URL `http://localhost:4567`. Of course, all other GumTree's commands are available.

## Debug

If you wan to debug GumTree container use the following command line: `docker run -v /my/folder:/diff -p 4567:4567 --entrypoint "/bin/bash" -it gumtree`.
