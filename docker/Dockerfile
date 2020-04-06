FROM ubuntu:bionic

# installs all required packages and sets locale
RUN apt-get update \
	&& apt-get install -y openjdk-11-jdk wget git gdebi-core ocaml ocaml-native-compilers camlp4 build-essential m4 zip python-pip

ENV LANG C.UTF-8

RUN wget http://131.123.42.38/lmcrs/v1.0.0/srcml_1.0.0-1_ubuntu18.04.deb \
	&& gdebi srcml_1.0.0-1_ubuntu18.04.deb -n \
	&& pip install jsontree asttokens

WORKDIR /opt

# install cgum
RUN git clone https://github.com/GumTreeDiff/cgum.git --depth 1 \
    && make -C /opt/cgum \
	&& ln -s /opt/cgum/cgum /usr/bin/cgum

# install pythonparser
RUN git clone https://github.com/GumTreeDiff/pythonparser.git --depth 1 \
    && ln -s /opt/pythonparser/pythonparser /usr/bin/pythonparser

# install gumtree
COPY . /opt/gumtree
RUN /opt/gumtree/gradlew -p /opt/gumtree build \
	&& ln -s /opt/gumtree/dist/build/install/gumtree/bin/gumtree /usr/bin/gumtree

# define volume diff to make available files to diff
RUN mkdir -p /diff/left /diff/right
WORKDIR /diff
VOLUME /diff/left /diff/right

# expose port 4567 for webdiff
EXPOSE 4567

ENTRYPOINT ["gumtree"]
