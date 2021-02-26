#!/usr/bin/env python3

import sys
import pandas as pd
from scipy.stats import mannwhitneyu

def main():
  ref_file = sys.argv[1]
  file = sys.argv[2]
  difference_type = sys.argv[3]
  displayDifferences(ref_file, file, difference_type)

def displayDifferences(ref_file, file, difference_type):
  ref_data = pd.read_csv(ref_file, decimal=",", sep=";")
  data = pd.read_csv(file, decimal=",", sep=";")
  ref_algorithms = set(pd.unique(ref_data['algorithm']).tolist())
  algorithms = set(pd.unique(data['algorithm']).tolist())
  common_algorithms = ref_algorithms.intersection(algorithms)
  common_cases = set(pd.unique(ref_data['case']).tolist())
  print("Selected algorithms " + repr(common_algorithms))
  for algorithm in common_algorithms:
    for case in common_cases:
      ref_value = ref_data[(ref_data['algorithm'] == algorithm) & (ref_data['case'] == case)][difference_type].item()
      actual_value = data[(data['algorithm'] == algorithm) & (data['case'] == case)][difference_type].item()
      if (ref_value != actual_value):
        print("Detected " + difference_type + " difference for algorithm: " + algorithm + " on case: " + case)
        print(ref_value)
        print(actual_value)

if __name__ == "__main__":
    main()