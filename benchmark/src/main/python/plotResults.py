#!/usr/bin/env python3

import sys
import statistics
import pandas as pd
from plotnine import *

def main():
  file = sys.argv[1]
  plotCsv(file)

def plotCsv(file):
  data = pd.read_csv(file, decimal=",", sep=";")
  plot = ggplot(data, aes(x='factor(algorithm)', y='size')) + geom_jitter()
  plot.save(file + "_size.pdf")
  data['runtime'] = data.apply (lambda row: statistics.median([row['t1'], row['t2'], row['t3'], row['t4'], row['t5']]), axis = 1)
  plot = ggplot(data, aes(x='factor(algorithm)', y='runtime')) + geom_jitter()
  plot.save(file + "_runtime.pdf")

if __name__ == "__main__":
    main()