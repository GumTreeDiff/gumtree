#!/usr/bin/env python3

import sys
import pandas as pd
from scipy.stats import mannwhitneyu

def main():
  ref_file = sys.argv[1]
  file = sys.argv[2]
  regression_type = sys.argv[3]
  testRegressions(ref_file, file, regression_type)

def testRegressions(ref_file, file, regression_type):
  ref_data = pd.read_csv(ref_file, decimal=",", sep=";")
  data = pd.read_csv(file, decimal=",", sep=";")
  ref_algorithms = set(pd.unique(ref_data['algorithm']).tolist())
  algorithms = set(pd.unique(data['algorithm']).tolist())
  common_algorithms = ref_algorithms.intersection(algorithms)
  print("Selected algorithms " + repr(common_algorithms))
  for algorithm in common_algorithms:
    print("Checking " + regression_type + " regressions for algorithm: " + algorithm)
    ref_algorithm_data = ref_data[ref_data['algorithm'] == algorithm][regression_type]
    algorithm_data = data[data['algorithm'] == algorithm][regression_type]
    stat, p = mannwhitneyu(ref_algorithm_data, algorithm_data)
    if p < 0.05:
      print("Detected " + regression_type + " regressions for algorithm: " + algorithm)
      print(p)
      sys.exit(1)
    else:
      print("No " + regression_type + " regressions for algorithm: " + algorithm)

if __name__ == "__main__":
    main()