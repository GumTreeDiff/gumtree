#!/usr/bin/env python3

import sys
import statistics
import pandas as pd
from plotnine import *

def main():
  file = sys.argv[1]
  plotCsv(file)

def plotCsv(file):
  print("Plotting results")
  data = pd.read_csv(file, decimal=",", sep=";")
  plot = ggplot(data, aes(x='factor(algorithm)', y='nm', fill='factor(algorithm)')) + geom_violin(draw_quantiles=[0.25, 0.5, 0.75], show_legend=False) + scale_y_continuous(trans="log1p", breaks = [0, 1, 5, 10, 20, 50, 100, 1000]) + ylab("Number of move actions") + xlab("Algorithm") + scale_fill_manual(values=cb_palette()) + theme_light()
  plot.save(file + "_mv.pdf")
  plot = ggplot(data, aes(x='factor(algorithm)', y='nu', fill='factor(algorithm)')) + geom_violin(draw_quantiles=[0.25, 0.5, 0.75], show_legend=False) + scale_y_continuous(trans="log1p", breaks = [0, 1, 5, 10, 20, 50, 100, 1000]) + ylab("Number of update actions") + xlab("Algorithm") + scale_fill_manual(values=cb_palette()) + theme_light()
  plot.save(file + "_upd.pdf")
  plot = ggplot(data, aes(x='factor(algorithm)', y='s', fill='factor(algorithm)')) + geom_violin(draw_quantiles=[0.25, 0.5, 0.75], show_legend=False) + scale_y_continuous(trans="log1p", breaks = [0, 1, 5, 10, 20, 50, 100, 1000]) + ylab("Number of actions") + xlab("Algorithm") + scale_fill_manual(values=cb_palette()) + theme_light()
  plot.save(file + "_size.pdf")
  data['runtime'] = data.apply (lambda row: statistics.median([row['t'], row['t.1'], row['t.2'], row['t.3'], row['t.4']]), axis = 1)
  plot = ggplot(data, aes(x='factor(algorithm)', y='runtime / 1_000_000_000', fill='factor(algorithm)')) + geom_violin(draw_quantiles=[0.25, 0.5, 0.75], show_legend=False) + scale_y_continuous(trans="log10", breaks = [0, 0.5, 1, 10, 100, 1000]) + ylab("Runtime (s)") + xlab("Algorithm") + scale_fill_manual(values=cb_palette()) + theme_light()
  plot.save(file + "_runtime.pdf")

def cb_palette():
  return ["#E69F00", "#56B4E9", "#009E73", "#F0E442", "#0072B2", "#D55E00", "#CC79A7"]

if __name__ == "__main__":
    main()