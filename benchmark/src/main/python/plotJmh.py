#!/usr/bin/env python

import sys
import glob
import numpy as np
import pandas as pd
from plotnine import *

def main():
    folder = sys.argv[1]
    files = glob.glob(folder + "*.csv")
    for file in files:
        plotCsv(file, folder)

def plotCsv(file, folder):
    data = pd.read_csv(file, usecols = [0,4,7], decimal=",", header = 0, names = ['Algorithm','Time','Case'], dtype = { 'Algorithm':'category'})
    data['Case'] = data.Case.str.slice(73)
    data['Case'] = pd.Categorical(data['Case'])
    data['Algorithm'] = data.Algorithm.cat.rename_categories({'com.github.gumtree.dist.MatcherAnalyzer.testClassicGumtree':'Classic Gumtree', 'com.github.gumtree.dist.MatcherAnalyzer.testSimpleGumtree':'Simple Gumtree'})
    print(data)
    plot = ggplot(data) + geom_bar(aes(x='Case', y='Time', fill='Algorithm'), stat='identity', position='position_dodge')
    plot.save(file + ".pdf")

if __name__ == "__main__":
    main()

