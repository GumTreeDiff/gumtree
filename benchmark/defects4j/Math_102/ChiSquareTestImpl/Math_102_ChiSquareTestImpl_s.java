/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.math.stat.inference;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ChiSquaredDistribution;
import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;
import org.apache.commons.math.distribution.DistributionFactory;

/**
 * Implements Chi-Square test statistics defined in the
 * {@link UnknownDistributionChiSquareTest} interface.
 *
 * @version $Revision$ $Date$
 */
public class ChiSquareTestImpl implements UnknownDistributionChiSquareTest {

    /** Distribution used to compute inference statistics. */
    private ChiSquaredDistribution distribution;
  
    /**
     * Construct a ChiSquareTestImpl 
     */
    public ChiSquareTestImpl() {
        this(new ChiSquaredDistributionImpl(1.0));
    }

    /**
     * Create a test instance using the given distribution for computing
     * inference statistics.
     * @param x distribution used to compute inference statistics.
     * @since 1.2
     */
    public ChiSquareTestImpl(ChiSquaredDistribution x) {
        super();
        setDistribution(x);
    }
     /**
     * {@inheritDoc}
     * <p><strong>Note: </strong>This implementation rescales the 
     * <code>expected</code> array if necessary to ensure that the sum of the
     * expected and observed counts are equal.</p>
     * 
     * @param observed array of observed frequency counts
     * @param expected array of expected frequency counts
     * @return chi-square test statistic
     * @throws IllegalArgumentException if preconditions are not met
     * or length is less than 2
     */
    public double chiSquare(double[] expected, long[] observed)
        throws IllegalArgumentException {
        if ((expected.length < 2) || (expected.length != observed.length)) {
            throw new IllegalArgumentException(
                    "observed, expected array lengths incorrect");
        }
        if (!isPositive(expected) || !isNonNegative(observed)) {
            throw new IllegalArgumentException(
                "observed counts must be non-negative and expected counts must be postive");
        }
        double sumSq = 0.0d;
        double dev = 0.0d;
        for (int i = 0; i < observed.length; i++) {
                dev = ((double) observed[i] - expected[i]);
                sumSq += dev * dev / expected[i];
        }
        return sumSq;
    }

    /**
     * {@inheritDoc}
     * <p><strong>Note: </strong>This implementation rescales the 
     * <code>expected</code> array if necessary to ensure that the sum of the
     * expected and observed counts are equal.</p>
     * 
     * @param observed array of observed frequency counts
     * @param expected array of exptected frequency counts
     * @return p-value
     * @throws IllegalArgumentException if preconditions are not met
     * @throws MathException if an error occurs computing the p-value
     */
    public double chiSquareTest(double[] expected, long[] observed)
        throws IllegalArgumentException, MathException {
        distribution.setDegreesOfFreedom(expected.length - 1.0);
        return 1.0 - distribution.cumulativeProbability(
            chiSquare(expected, observed));
    }

    /**
     * {@inheritDoc}
     * <p><strong>Note: </strong>This implementation rescales the 
     * <code>expected</code> array if necessary to ensure that the sum of the
     * expected and observed counts are equal.</p>
     * 
     * @param observed array of observed frequency counts
     * @param expected array of exptected frequency counts
     * @param alpha significance level of the test
     * @return true iff null hypothesis can be rejected with confidence
     * 1 - alpha
     * @throws IllegalArgumentException if preconditions are not met
     * @throws MathException if an error occurs performing the test
     */
    public boolean chiSquareTest(double[] expected, long[] observed, 
            double alpha) throws IllegalArgumentException, MathException {
        if ((alpha <= 0) || (alpha > 0.5)) {
            throw new IllegalArgumentException(
                    "bad significance level: " + alpha);
        }
        return (chiSquareTest(expected, observed) < alpha);
    }
    
    /**
     * @param counts array representation of 2-way table
     * @return chi-square test statistic
     * @throws IllegalArgumentException if preconditions are not met
     */
    public double chiSquare(long[][] counts) throws IllegalArgumentException {
        
        checkArray(counts);
        int nRows = counts.length;
        int nCols = counts[0].length;
        
        // compute row, column and total sums
        double[] rowSum = new double[nRows];
        double[] colSum = new double[nCols];
        double total = 0.0d;
        for (int row = 0; row < nRows; row++) {
            for (int col = 0; col < nCols; col++) {
                rowSum[row] += (double) counts[row][col];
                colSum[col] += (double) counts[row][col];
                total += (double) counts[row][col];
            }
        }
        
        // compute expected counts and chi-square
        double sumSq = 0.0d;
        double expected = 0.0d;
        for (int row = 0; row < nRows; row++) {
            for (int col = 0; col < nCols; col++) {
                expected = (rowSum[row] * colSum[col]) / total;
                sumSq += (((double) counts[row][col] - expected) * 
                        ((double) counts[row][col] - expected)) / expected; 
            }
        } 
        return sumSq;
    }

    /**
     * @param counts array representation of 2-way table
     * @return p-value
     * @throws IllegalArgumentException if preconditions are not met
     * @throws MathException if an error occurs computing the p-value
     */
    public double chiSquareTest(long[][] counts)
    throws IllegalArgumentException, MathException {
        checkArray(counts);
        double df = ((double) counts.length -1) * ((double) counts[0].length - 1);
        distribution.setDegreesOfFreedom(df);
        return 1 - distribution.cumulativeProbability(chiSquare(counts));
    }

    /**
     * @param counts array representation of 2-way table
     * @param alpha significance level of the test
     * @return true iff null hypothesis can be rejected with confidence
     * 1 - alpha
     * @throws IllegalArgumentException if preconditions are not met
     * @throws MathException if an error occurs performing the test
     */
    public boolean chiSquareTest(long[][] counts, double alpha)
    throws IllegalArgumentException, MathException {
        if ((alpha <= 0) || (alpha > 0.5)) {
            throw new IllegalArgumentException("bad significance level: " + alpha);
        }
        return (chiSquareTest(counts) < alpha);
    }
    
    /**
     * @param observed1 array of observed frequency counts of the first data set
     * @param observed2 array of observed frequency counts of the second data set
     * @return chi-square test statistic
     * @throws IllegalArgumentException if preconditions are not met
     */
    public double chiSquareDataSetsComparison(long[] observed1, long[] observed2)
        throws IllegalArgumentException {
        
        // Make sure lengths are same
        if ((observed1.length < 2) || (observed1.length != observed2.length)) {
            throw new IllegalArgumentException(
                    "oberved1, observed2 array lengths incorrect");
        }
        // Ensure non-negative counts
        if (!isNonNegative(observed1) || !isNonNegative(observed2)) {
            throw new IllegalArgumentException(
                "observed counts must be non-negative");
        }
        // Compute and compare count sums
        long countSum1 = 0;
        long countSum2 = 0;
        boolean unequalCounts = false;
        double weight = 0.0;
        for (int i = 0; i < observed1.length; i++) {
            countSum1 += observed1[i];
            countSum2 += observed2[i];   
        }
        // Ensure neither sample is uniformly 0
        if (countSum1 * countSum2 == 0) {
            throw new IllegalArgumentException(
             "observed counts cannot all be 0"); 
        }
        // Compare and compute weight only if different
        unequalCounts = (countSum1 != countSum2);
        if (unequalCounts) {
            weight = Math.sqrt((double) countSum1 / (double) countSum2);
        }
        // Compute ChiSquare statistic
        double sumSq = 0.0d;
        double dev = 0.0d;
        double obs1 = 0.0d;
        double obs2 = 0.0d;
        for (int i = 0; i < observed1.length; i++) {
            if (observed1[i] == 0 && observed2[i] == 0) {
                throw new IllegalArgumentException(
                        "observed counts must not both be zero");
            } else {
                obs1 = (double) observed1[i];
                obs2 = (double) observed2[i];
                if (unequalCounts) { // apply weights
                    dev = obs1/weight - obs2 * weight;
                } else {
                    dev = obs1 - obs2;
                }
                sumSq += (dev * dev) / (obs1 + obs2);
            }
        }
        return sumSq;
    }

    /**
     * @param observed1 array of observed frequency counts of the first data set
     * @param observed2 array of observed frequency counts of the second data set
     * @return p-value
     * @throws IllegalArgumentException if preconditions are not met
     * @throws MathException if an error occurs computing the p-value
     */
    public double chiSquareTestDataSetsComparison(long[] observed1, long[] observed2)
        throws IllegalArgumentException, MathException {
        distribution.setDegreesOfFreedom((double) observed1.length - 1);
        return 1 - distribution.cumulativeProbability(
                chiSquareDataSetsComparison(observed1, observed2));
    }

    /**
     * @param observed1 array of observed frequency counts of the first data set
     * @param observed2 array of observed frequency counts of the second data set
     * @param alpha significance level of the test
     * @return true iff null hypothesis can be rejected with confidence
     * 1 - alpha
     * @throws IllegalArgumentException if preconditions are not met
     * @throws MathException if an error occurs performing the test
     */
    public boolean chiSquareTestDataSetsComparison(long[] observed1, long[] observed2,
            double alpha) throws IllegalArgumentException, MathException {
        if ((alpha <= 0) || (alpha > 0.5)) {
            throw new IllegalArgumentException(
                    "bad significance level: " + alpha);
        }
        return (chiSquareTestDataSetsComparison(observed1, observed2) < alpha);
    }

    /**
     * Checks to make sure that the input long[][] array is rectangular,
     * has at least 2 rows and 2 columns, and has all non-negative entries,
     * throwing IllegalArgumentException if any of these checks fail.
     * 
     * @param in input 2-way table to check
     * @throws IllegalArgumentException if the array is not valid
     */
    private void checkArray(long[][] in) throws IllegalArgumentException {
        
        if (in.length < 2) {
            throw new IllegalArgumentException("Input table must have at least two rows");
        }
        
        if (in[0].length < 2) {
            throw new IllegalArgumentException("Input table must have at least two columns");
        }    
        
        if (!isRectangular(in)) {
            throw new IllegalArgumentException("Input table must be rectangular");
        }
        
        if (!isNonNegative(in)) {
            throw new IllegalArgumentException("All entries in input 2-way table must be non-negative");
        }
        
    }
    
    //---------------------  Protected methods ---------------------------------
    /**
     * Gets a DistributionFactory to use in creating ChiSquaredDistribution instances.
     * @deprecated inject ChiSquaredDistribution instances directly instead of
     *             using a factory.
     */
    protected DistributionFactory getDistributionFactory() {
        return DistributionFactory.newInstance();
    }
    
    //---------------------  Private array methods -- should find a utility home for these
    
    /**
     * Returns true iff input array is rectangular.
     * 
     * @param in array to be tested
     * @return true if the array is rectangular
     * @throws NullPointerException if input array is null
     * @throws ArrayIndexOutOfBoundsException if input array is empty
     */
    private boolean isRectangular(long[][] in) {
        for (int i = 1; i < in.length; i++) {
            if (in[i].length != in[0].length) {
                return false;
            }
        }  
        return true;
    }
    
    /**
     * Returns true iff all entries of the input array are > 0.
     * Returns true if the array is non-null, but empty
     * 
     * @param in array to be tested
     * @return true if all entries of the array are positive
     * @throws NullPointerException if input array is null
     */
    private boolean isPositive(double[] in) {
        for (int i = 0; i < in.length; i ++) {
            if (in[i] <= 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Returns true iff all entries of the input array are >= 0.
     * Returns true if the array is non-null, but empty
     * 
     * @param in array to be tested
     * @return true if all entries of the array are non-negative
     * @throws NullPointerException if input array is null
     */
    private boolean isNonNegative(long[] in) {
        for (int i = 0; i < in.length; i ++) {
            if (in[i] < 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Returns true iff all entries of (all subarrays of) the input array are >= 0.
     * Returns true if the array is non-null, but empty
     * 
     * @param in array to be tested
     * @return true if all entries of the array are non-negative
     * @throws NullPointerException if input array is null
     */
    private boolean isNonNegative(long[][] in) {
        for (int i = 0; i < in.length; i ++) {
            for (int j = 0; j < in[i].length; j++) {
                if (in[i][j] < 0) {
                    return false;
                }
            }
        }
        return true;
    }
 
    /**
     * Modify the distribution used to compute inference statistics.
     * 
     * @param value
     *            the new distribution
     * @since 1.2
     */
    public void setDistribution(ChiSquaredDistribution value) {
        distribution = value;
    }
}
