FROM ubuntu:focal

ARG DEBIAN_FRONTEND=noninteractive
ENV TZ=GMT

# Install all required packages
RUN apt-get update \
	&& apt-get install -y --no-install-recommends openjdk-17-jdk wget git gdebi-core \
	    build-essential ocaml libnum-ocaml-dev python3-pip python3-dev nodejs npm \
	    tzdata ca-certificates p11-kit

RUN pip3 install pandas plotnine scipy

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

# Install jsparser
RUN git clone https://github.com/GumTreeDiff/jsparser.git /opt/jsparser --depth 1 \
    && ln -s /opt/jsparser/jsparser /usr/bin/jsparser \
    && npm --prefix /opt/jsparser/ install /opt/jsparser/

# Install tree-sitter-parser
RUN git clone --recurse-submodules https://github.com/GumTreeDiff/tree-sitter-parser.git /opt/tree-sitter-parser --depth 1 \
    && ln -s /opt/tree-sitter-parser/tree-sitter-parser.py /usr/bin/tree-sitter-parser.py \
    && pip3 install tree_sitter pyyaml

# Install gumtree
COPY . /opt/gumtree
RUN /opt/gumtree/gradlew -PtestNative -p /opt/gumtree build \
	&& ln -s /opt/gumtree/dist/build/install/gumtree/bin/gumtree /usr/bin/gumtree

# Define volume diff to make available files to diff
RUN mkdir -p /diff/left /diff/right
WORKDIR /diff
VOLUME /diff/left /diff/right

# Expose port 4567 for webdiff
EXPOSE 4567

ENTRYPOINT ["gumtree"]
