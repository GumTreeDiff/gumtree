FROM ubuntu:xenial

RUN apt-get update \
	&& apt-get install -y software-properties-common python-software-properties \
	&& echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections \
	&& add-apt-repository ppa:webupd8team/java -y \
	&& apt-get update \
	&& apt-get install -y oracle-java8-installer oracle-java8-set-default libarchive13 libcurl3 ocaml ocaml-native-compilers camlp4 git build-essential zip \
	&& wget http://131.123.42.38/lmcrs/beta/srcML-Ubuntu14.04-64.deb \
	&& dpkg -i srcML-Ubuntu14.04-64.deb

WORKDIR /opt
RUN git clone https://github.com/GumTreeDiff/cgum.git --depth 1
WORKDIR /opt/cgum
RUN make \
	&& ln -s /opt/cgum/cgum /usr/bin/cgum

WORKDIR /opt
RUN git clone -b master https://github.com/GumTreeDiff/gumtree.git --depth 1
WORKDIR /opt/gumtree
RUN ./gradlew build \
	&& unzip dist/build/distributions/gumtree.zip \
	&& ln -s /opt/gumtree/gumtree/bin/gumtree /usr/bin/gumtree

RUN mkdir /diff
WORKDIR /diff
VOLUME /diff

EXPOSE 4567

ENTRYPOINT ["gumtree"]
