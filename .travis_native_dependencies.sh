#!/usr/bin/env sh
# 1) installing srcML
# start by boost dependency
sudo apt-get update
sudo apt-get install gcc g++ libxml2-dev libxslt1-dev libarchive-dev antlr libantlr-dev libcurl4-openssl-dev libssl-dev
wget https://netix.dl.sourceforge.net/project/boost/boost/1.55.0/boost_1_55_0.tar.gz
tar -xzf boost_1_55_0.tar.gz
cd boost_1_55_0
./bootstrap.sh --without-libraries=atomic,chrono,context,coroutine,exception,graph,graph_parallel,iostreams,locale,log,math,mpi,python,random,serialization,signals,test,timer,wave
sudo ./b2 -d0 link=static cxxflags="-fPIC -static -Wl,--whole-archive" threading=multi install
cd ..
# then srcml itself
wget http://131.123.42.38/lmcrs/beta/srcML-src.tar.gz
tar -xzf srcML-src.tar.gz
cd srcML-src
cmake .
make
cd ..
# 2) installing cgum
sudo apt-get install ocaml ocaml-native-compilers camlp4
git clone --branch v1.0.0 https://github.com/GumTreeDiff/cgum.git --depth 1
cd cgum
make
cd ..
# 3) installing pythonparser
git clone https://github.com/GumTreeDiff/pythonparser.git --depth 1