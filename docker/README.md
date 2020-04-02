# GumTree dockerfile

## Installation

First you need to build the container. Go to the `docker` folder and compile the image: `docker build . -t jrfaller/gumtree`.

You can also directly pull a version from Docker Hub via the command `docker pull jrfaller/gumtree`.

## Usage

Now you can use GumTree container. You need to:
* bind the "original" folder folder to the `/diff/left` volume of the container
* bind the "modified" folder folder to the `/diff/right` volume of the container,
* bind the `4567` port to be able to use webdiff

A sample command line is `docker run -v /my/original-folder:/diff/left -v /my/modified-folder:/diff/right -p 4567:4567 jrfaller/gumtree webdiff left/ right/`. You can consult the diff at the URL `http://localhost:4567`. Of course, all other GumTree's commands are available.

**Beware, the paths inputs to GumTree's commands are relative to the `/diff` path inside the container.** 

## Git integration

You can easily integrate GumTree's container with Git by adding the following configuration into the `$HOME/.gitconfig` file.
```
[difftool "gumtree-docker"]
	cmd = docker run -v /private/$LOCAL:/diff/left -v /private/$REMOTE:/diff/right -p 4567:4567 jrfaller/gumtree webdiff left/ right/
```

## Debug

If you want to debug GumTree container use the following command line: `docker run -v -v /my/original-folder:/diff/left -v /my/modified-folder:/diff/right -p 4567:4567 --entrypoint "/bin/bash" -it jrfaller/gumtree`.
