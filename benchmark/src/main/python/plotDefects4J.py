#!/usr/bin/env python3

import sys
import pandas as pd
from plotnine import *

def main():
  file = sys.argv[1]
  plotCsv(file)

def plotCsv(file):
  data = pd.read_csv(file, decimal=",", sep=";")
  plot = ggplot(data, aes(x='factor(algorithm)', y='size')) + geom_violin()
  plot.save(file + "_size.pdf")
  plot = ggplot(data, aes(x='factor(algorithm)', y='runtime')) + geom_violin()
  plot.save(file + "_runtime.pdf")

if __name__ == "__main__":
    main()