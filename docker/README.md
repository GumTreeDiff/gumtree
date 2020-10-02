# GumTree docker image

## Installation

You can directly pull a version of latest GumTree's image from Docker Hub via the command `docker pull gumtreediff/gumtree`.

You can also compile the image on your machine. Go to the root GumTree's folder and compile the image: `docker build . -f docker/Dockerfile -t gumtreediff/gumtree`.

## Usage

To use GumTree's image, You need to:
* bind the "original" folder to the `/diff/left` volume of the container
* bind the "modified" folder to the `/diff/right` volume of the container
* bind the port `4567` of the container to access GumTree's web interface

The classical way to run a GumTree's container is the command `docker run -v /my/original-folder:/diff/left -v /my/modified-folder:/diff/right -p 4567:4567 gumtreediff/gumtree webdiff left/ right/`. You can consult the diff at the URL `http://localhost:4567`. Of course, all other GumTree's commands are available.

**Beware, the paths inputs to GumTree's commands are relative to the `/diff` path inside the container.** 

## Git integration

You can easily integrate GumTree's container with Git by adding the following configuration into the `$HOME/.gitconfig` file.

### Mac OS

```
[difftool "gumtree-docker"]
	cmd = docker run -v /private/$REMOTE:/diff/left -v /private/$LOCAL:/diff/right -p 4567:4567 gumtreediff/gumtree webdiff left/ right/
```
### Linux - Windows

```	
[difftool "gumtree-docker"]
	cmd = docker run -v $REMOTE:/diff/left -v $LOCAL:/diff/right -p 4567:4567 gumtreediff/gumtree webdiff left/ right/
```
### Usage

You can invoke GumTree's from git by running the command `git difftool -d --no-symlinks -t gumtree-docker`. We recommend putting an alias in the alias section of `$HOME/.gitconfig` such as:

```
[alias]
	gd = difftool -d --no-symlinks -t gumtree-docker
```

You can then use the command `git gd`.

## Debug

If you want to debug GumTree's image use the following command line: `docker run -v /my/original-folder:/diff/left -v /my/modified-folder:/diff/right -p 4567:4567 --entrypoint "/bin/bash" -it gumtreediff/gumtree`.
