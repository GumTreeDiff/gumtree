#!/usr/bin/env python3

import sys
import pandas as pd
import plotnine as pn
import statistics
from scipy.stats import mannwhitneyu

pd.options.mode.chained_assignment = None

def main():
  ref_file = sys.argv[1]
  file = sys.argv[2]
  regression_type = sys.argv[3]
  testRegressions(ref_file, file, regression_type)

def testRegressions(ref_file, file, regression_type):
  print(f"Testing regressions for {regression_type}")
  ref_data = pd.read_csv(ref_file, decimal=",", sep=";")
  ref_data['runtime'] = ref_data.apply (lambda row: statistics.median([row['t'], row['t.1'], row['t.2'], row['t.3'], row['t.4']]), axis = 1)
  data = pd.read_csv(file, decimal=",", sep=";")
  data['runtime'] = data.apply (lambda row: statistics.median([row['t'], row['t.1'], row['t.2'], row['t.3'], row['t.4']]), axis = 1)
  ref_algorithms = set(pd.unique(ref_data['algorithm']).tolist())
  algorithms = set(pd.unique(data['algorithm']).tolist())
  common_algorithms = ref_algorithms.intersection(algorithms)
  print("Selected algorithms " + repr(common_algorithms))
  regression = False
  for algorithm in common_algorithms:
    print("Checking " + regression_type + " regressions for algorithm: " + algorithm)
    ref_algorithm_data = ref_data[ref_data['algorithm'] == algorithm]
    algorithm_data = data[data['algorithm'] == algorithm]
    algorithm_data['rel_value'] = algorithm_data[regression_type] - ref_algorithm_data[regression_type]
    plot = pn.ggplot(algorithm_data, pn.aes(0, 'rel_value')) \
        + pn.geom_jitter() \
        + pn.ylim(min(*algorithm_data['rel_value'], -3), max(*algorithm_data['rel_value'], 3)) \
        + pn.geom_text(x = 0, y = max(*algorithm_data['rel_value'], 3), label = "worse", color = "red") \
        + pn.geom_text(x = 0, y = min(*algorithm_data['rel_value'], -3), label = "better", color = "red")
    plot.save(file + "_regression_" + algorithm + "_" + regression_type + ".pdf")
    stat, p = mannwhitneyu(ref_algorithm_data[regression_type], algorithm_data[regression_type])
    if p < 0.05:
      print("Detected " + regression_type + " regressions for algorithm: " + algorithm)
      print(p)
      regression = True
    else:
      print("No " + regression_type + " regressions for algorithm: " + algorithm)
  if regression == True:
     print("Regression detected")
     sys.exit(1)

if __name__ == "__main__":
    main()