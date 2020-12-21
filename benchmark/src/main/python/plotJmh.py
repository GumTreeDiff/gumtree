#!/usr/bin/env python3

import sys
import glob
import datetime
import numpy as np
import pandas as pd
from plotnine import *

def main():
    folder = sys.argv[1]
    files = glob.glob(folder + "*.csv")
    dataframes = []
    for file in files:
        dataframes.append(loadFile(file, folder))
    data = pd.concat(dataframes)
    data.sort_values(by=['Date'], inplace=True)
    data['Date'] = pd.Categorical(data['Date'], ordered=True)
    print(data)
    genPlot(data[data['Algorithm'] == 'Simple Gumtree'], "simple")
    genPlot(data[data['Algorithm'] == 'Classic Gumtree'], "classic")

def genPlot(algorithmData, name):
    plot = ggplot(algorithmData) + geom_bar(aes(x='Case', y='Runtime', fill='Date', order='Date'), stat='identity', position='position_dodge')
    plot.save(name + "_results.pdf")

def loadFile(file, folder):
    dateStr = file[file.find('_') + 1:file.find('_') + 15]
    # date = datetime.datetime.strptime(dateStr,'%Y%m%d%H%M%S')
    data = pd.read_csv(file, usecols = [0,4,7], decimal=",", header = 0, names = ['Algorithm','Runtime','Case'], dtype = { 'Algorithm':'category'})
    data['Case'] = data.Case.str.slice(data.Case.str.rfind('/')[0] + 1)
    data['Case'] = pd.Categorical(data['Case'])
    data['Algorithm'] = data.Algorithm.cat.rename_categories({'com.github.gumtree.dist.MatcherAnalyzer.testClassicGumtree':'Classic Gumtree', 'com.github.gumtree.dist.MatcherAnalyzer.testSimpleGumtree':'Simple Gumtree'})
    data['Date'] = dateStr
    return data

if __name__ == "__main__":
    main()

