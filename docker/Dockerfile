FROM ubuntu:focal

# Install all required packages
# Start with a fix to get the openjdk-11-jdk package to be installed on buster
RUN mkdir /usr/share/man/man1/ \
    && apt-get update \
	&& apt-get install -y --no-install-recommends default-jdk wget git gdebi-core \
	    ocaml ocaml-native-compilers camlp4 build-essential m4 zip python3-pip \
	    ca-certificates p11-kit

# Set locale
ENV LANG C.UTF-8

# Install srcML
RUN wget http://131.123.42.38/lmcrs/v1.0.0/srcml_1.0.0-1_ubuntu20.04.deb \
	&& gdebi srcml_1.0.0-1_ubuntu20.04.deb -n

# Install cgum
RUN git clone https://github.com/GumTreeDiff/cgum.git /opt/cgum --depth 1 \
    && make -C /opt/cgum \
	&& ln -s /opt/cgum/cgum /usr/bin/cgum

# Install pythonparser
RUN git clone https://github.com/GumTreeDiff/pythonparser.git /opt/pythonparser --depth 1 \
    && ln -s /opt/pythonparser/pythonparser /usr/bin/pythonparser \
    && pip3 install parso

# Install gumtree
COPY . /opt/gumtree
RUN /opt/gumtree/gradlew -p /opt/gumtree build \
	&& ln -s /opt/gumtree/dist/build/install/gumtree/bin/gumtree /usr/bin/gumtree

# Define volume diff to make available files to diff
RUN mkdir -p /diff/left /diff/right
WORKDIR /diff
VOLUME /diff/left /diff/right

# Expose port 4567 for webdiff
EXPOSE 4567

ENTRYPOINT ["gumtree"]
