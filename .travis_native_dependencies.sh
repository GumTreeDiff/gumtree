#!/usr/bin/env sh

# 0) Installing dependencies
sudo apt-get update
sudo apt-get install gdebi-core ocaml libnum-ocaml-dev

# 1) installing srcML
wget http://131.123.42.38/lmcrs/v1.0.0/srcml_1.0.0-1_ubuntu20.04.deb
sudo gdebi srcml_1.0.0-1_ubuntu20.04.deb -n

# 2) installing cgum
git clone --branch v1.0.0 https://github.com/GumTreeDiff/cgum.git --depth 1
cd cgum
make
cd ..

# 3) installing pythonparser
pip3 install parso
git clone https://github.com/GumTreeDiff/pythonparser.git --depth 1