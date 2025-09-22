FROM ubuntu:noble AS build-stage

ARG TARGETARCH
ARG DEBIAN_FRONTEND=noninteractive
ENV TZ=GMT
ENV LANG=C.UTF-8

# Install all dependencies
RUN apt-get update \
	&& apt-get install -y --no-install-recommends openjdk-17-jdk-headless wget && \
    if [ "${TARGETARCH}" = "arm64" ]; then \
            wget https://github.com/srcML/srcML/releases/download/v1.1.0/srcml_1.1.0-1_ubuntu24.10_arm64.deb && \
            apt install -y --no-install-recommends ./srcml_1.1.0-1_ubuntu24.10_arm64.deb; \
        else \
            wget https://github.com/srcML/srcML/releases/download/v1.1.0/srcml_1.1.0-1_ubuntu24.04_amd64.deb && \
            apt install -y --no-install-recommends ./srcml_1.1.0-1_ubuntu24.04_amd64.deb; \
    fi

# Compile gumtree
COPY . /opt/gumtree
RUN /opt/gumtree/gradlew -PtestExternal -p /opt/gumtree build

FROM ubuntu:noble AS final-stage

ARG TARGETARCH
ARG DEBIAN_FRONTEND=noninteractive
ENV TZ=GMT
ENV LANG=C.UTF-8

# Copy gumtree distribution from build-stage.
COPY --from=build-stage /opt/gumtree/dist/build/install/gumtree/ /opt/gumtree
# Copy srcml package from build-stage.
COPY --from=build-stage /*.deb /opt

# Install all dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends openjdk-17-jre-headless && \
    if [ "${TARGETARCH}" = "arm64" ]; then \
        apt install -y --no-install-recommends /opt/srcml_1.1.0-1_ubuntu24.10_arm64.deb; \
    else \
        apt install -y --no-install-recommends /opt/srcml_1.1.0-1_ubuntu24.04_amd64.deb; \
    fi && \
    rm -rf /var/cache/apt/archives /var/lib/apt/lists/*. /opt/*.deb && \
    mkdir -p /left /right

VOLUME /left /right

# Expose port 4567 for webdiff
EXPOSE 4567

ENTRYPOINT ["/opt/gumtree/bin/gumtree"]