FROM ubuntu:noble

ARG DEBIAN_FRONTEND=noninteractive
ENV TZ=GMT

# Install all required packages
RUN apt-get update \
	&& apt-get install -y --no-install-recommends openjdk-17-jdk wget git gdebi-core \
	    build-essential ocaml libnum-ocaml-dev \
        python3-dev nodejs npm python3-venv \
	    tzdata ca-certificates p11-kit

RUN python3 -m venv /opt/venv

ENV PATH="/opt/venv/bin:$PATH"

RUN pip install pandas plotnine scipy jupyter

# Set locale
ENV LANG=C.UTF-8

ARG TARGETARCH

# Install srcML
RUN if [ "${TARGETARCH}" = "arm64" ]; then \
        wget https://github.com/srcML/srcML/releases/download/v1.1.0/srcml_1.1.0-1_ubuntu24.10_arm64.deb && \
        gdebi srcml_1.1.0-1_ubuntu24.10_arm64.deb -n; \
    else \
        wget https://github.com/srcML/srcML/releases/download/v1.1.0/srcml_1.1.0-1_ubuntu24.04_amd64.deb && \
        gdebi srcml_1.1.0-1_ubuntu24.04_amd64.deb -n; \
    fi

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
RUN git clone --recurse-submodules --shallow-submodules https://github.com/GumTreeDiff/tree-sitter-parser.git /opt/tree-sitter-parser --depth 1 \
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
