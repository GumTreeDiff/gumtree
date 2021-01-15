#!/usr/bin/env python3

import sys
import pandas as pd
from plotnine import *
from scipy.stats import mannwhitneyu

def main():
  ref_file = sys.argv[1]
  file = sys.argv[2]
  testRegression(ref_file, file)

def testRegression(ref_file, file):
  ref_data = pd.read_csv(ref_file, decimal=",", sep=";")
  data = pd.read_csv(ref_file, decimal=",", sep=";")
  ref_algorithms = set(pd.unique(ref_data['algorithm']).tolist())
  algorithms = set(pd.unique(data['algorithm']).tolist())
  common_algorithms = ref_algorithms.intersection(algorithms)
  # Test size regressions
  print("Testing regressions for algorithms " + repr(common_algorithms))
  for algorithm in common_algorithms:
    print("Checking script size regression for algorithm: " + algorithm)
    ref_algorithm_data = ref_data[ref_data['algorithm'] == algorithm]['size']
    algorithm_data = data[data['algorithm'] == algorithm]['size']
    stat, p = mannwhitneyu(ref_algorithm_data, algorithm_data)
    if p < 0.05:
      print("Detected script size regression for algorithm: " + algorithm)
      sys.exit(1)
    else:
      print("No script size regression for algorithm: " + algorithm)
  # Test running time regression
  for algorithm in common_algorithms:
    print("Checking script size regression for algorithm: " + algorithm)
    ref_algorithm_data = ref_data[ref_data['algorithm'] == algorithm]['runtime']
    algorithm_data = data[data['algorithm'] == algorithm]['runtime']
    stat, p = mannwhitneyu(ref_algorithm_data, algorithm_data)
    if p < 0.05:
      print("Detected runtime regression for algorithm: " + algorithm)
      sys.exit(1)
    else:
      print("No runtime regression for algorithm: " + algorithm)

if __name__ == "__main__":
    main()