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
  plot = ggplot(data, aes(x='nm', color='factor(algorithm)', fill='factor(algorithm)')) + geom_bar()
  plot.save(file + "_mv.pdf")
  plot = ggplot(data, aes(x='nu', color='factor(algorithm)', fill='factor(algorithm)')) + geom_bar()
  plot.save(file + "_upd.pdf")
  plot = ggplot(data, aes(x='factor(algorithm)', y='s')) + geom_violin()
  plot.save(file + "_size.pdf")
  data['runtime'] = data.apply (lambda row: statistics.median([row['t'], row['t.1'], row['t.2'], row['t.3'], row['t.4']]), axis = 1)
  plot = ggplot(data, aes(x='factor(algorithm)', y='runtime')) + geom_violin()
  plot.save(file + "_runtime.pdf")

if __name__ == "__main__":
    main()