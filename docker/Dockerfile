FROM ubuntu:bionic

# installs all required packages
RUN apt-get update \
	&& apt-get install -y openjdk-11-jdk wget git gdebi-core ocaml ocaml-native-compilers camlp4 build-essential m4 zip python-pip

RUN wget http://131.123.42.38/lmcrs/v1.0.0/srcml_1.0.0-1_ubuntu18.04.deb \
	&& gdebi srcml_1.0.0-1_ubuntu18.04.deb -n \
	&& pip install jsontree asttokens

# install cgum
WORKDIR /opt
RUN git clone https://github.com/GumTreeDiff/cgum.git --depth 1
WORKDIR /opt/cgum
RUN make \
	&& ln -s /opt/cgum/cgum /usr/bin/cgum

# install pythonparser
WORKDIR /opt
RUN git clone https://github.com/GumTreeDiff/pythonparser.git --depth 1
WORKDIR /opt/pythonparser
RUN ln -s /opt/pythonparser/pythonparser /usr/bin/pythonparser

# install gumtree
WORKDIR /opt
RUN git clone -b develop https://github.com/GumTreeDiff/gumtree.git --depth 1
WORKDIR /opt/gumtree
RUN export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8 \
    && ./gradlew build \
	&& ln -s /opt/gumtree/dist/build/install/gumtree/bin/gumtree /usr/bin/gumtree

# define volume diff to make available files to diff
RUN mkdir -p /diff/left
RUN mkdir -p /diff/right
WORKDIR /diff
VOLUME /diff/left
VOLUME /diff/right

# expose port 4567 for webdiff
EXPOSE 4567

ENTRYPOINT ["gumtree"]
