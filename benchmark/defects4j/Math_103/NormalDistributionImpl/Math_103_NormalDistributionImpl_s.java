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

package org.apache.commons.math.distribution;

import java.io.Serializable;

import org.apache.commons.math.MathException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.special.Erf;

/**
 * Default implementation of
 * {@link org.apache.commons.math.distribution.NormalDistribution}.
 *
 * @version $Revision$ $Date$
 */
public class NormalDistributionImpl extends AbstractContinuousDistribution 
        implements NormalDistribution, Serializable {
    
    /** Serializable version identifier */
    private static final long serialVersionUID = 8589540077390120676L;

    /** The mean of this distribution. */
    private double mean = 0;
    
    /** The standard deviation of this distribution. */
    private double standardDeviation = 1;
    
    /**
     * Create a normal distribution using the given mean and standard deviation.
     * @param mean mean for this distribution
     * @param sd standard deviation for this distribution
     */
    public NormalDistributionImpl(double mean, double sd){
        super();
        setMean(mean);
        setStandardDeviation(sd);
    }
    
    /**
     * Creates normal distribution with the mean equal to zero and standard
     * deviation equal to one. 
     */
    public NormalDistributionImpl(){
        this(0.0, 1.0);
    }
    
    /**
     * Access the mean.
     * @return mean for this distribution
     */ 
    public double getMean() {
        return mean;
    }
    
    /**
     * Modify the mean.
     * @param mean for this distribution
     */
    public void setMean(double mean) {
        this.mean = mean;
    }

    /**
     * Access the standard deviation.
     * @return standard deviation for this distribution
     */
    public double getStandardDeviation() {
        return standardDeviation;
    }

    /**
     * Modify the standard deviation.
     * @param sd standard deviation for this distribution
     * @throws IllegalArgumentException if <code>sd</code> is not positive.
     */
    public void setStandardDeviation(double sd) {
        if (sd <= 0.0) {
            throw new IllegalArgumentException(
                "Standard deviation must be positive.");
        }       
        standardDeviation = sd;
    }

    /**
     * For this disbution, X, this method returns P(X &lt; <code>x</code>).
     * @param x the value at which the CDF is evaluated.
     * @return CDF evaluted at <code>x</code>. 
     * @throws MathException if the algorithm fails to converge; unless
     * x is more than 20 standard deviations from the mean, in which case the
     * convergence exception is caught and 0 or 1 is returned.
     */
    public double cumulativeProbability(double x) throws MathException {
            return 0.5 * (1.0 + Erf.erf((x - mean) /
                    (standardDeviation * Math.sqrt(2.0))));
    }
    
    /**
     * For this distribution, X, this method returns the critical point x, such
     * that P(X &lt; x) = <code>p</code>.
     * <p>
     * Returns <code>Double.NEGATIVE_INFINITY</code> for p=0 and 
     * <code>Double.POSITIVE_INFINITY</code> for p=1.
     *
     * @param p the desired probability
     * @return x, such that P(X &lt; x) = <code>p</code>
     * @throws MathException if the inverse cumulative probability can not be
     *         computed due to convergence or other numerical errors.
     * @throws IllegalArgumentException if <code>p</code> is not a valid
     *         probability.
     */
    public double inverseCumulativeProbability(final double p) 
    throws MathException {
        if (p == 0) {
            return Double.NEGATIVE_INFINITY;
        }
        if (p == 1) {
            return Double.POSITIVE_INFINITY;
        }
        return super.inverseCumulativeProbability(p);
    }
    
    /**
     * Access the domain value lower bound, based on <code>p</code>, used to
     * bracket a CDF root.  This method is used by
     * {@link #inverseCumulativeProbability(double)} to find critical values.
     * 
     * @param p the desired probability for the critical value
     * @return domain value lower bound, i.e.
     *         P(X &lt; <i>lower bound</i>) &lt; <code>p</code> 
     */
    protected double getDomainLowerBound(double p) {
        double ret;

        if (p < .5) {
            ret = -Double.MAX_VALUE;
        } else {
            ret = getMean();
        }
        
        return ret;
    }

    /**
     * Access the domain value upper bound, based on <code>p</code>, used to
     * bracket a CDF root.  This method is used by
     * {@link #inverseCumulativeProbability(double)} to find critical values.
     * 
     * @param p the desired probability for the critical value
     * @return domain value upper bound, i.e.
     *         P(X &lt; <i>upper bound</i>) &gt; <code>p</code> 
     */
    protected double getDomainUpperBound(double p) {
        double ret;

        if (p < .5) {
            ret = getMean();
        } else {
            ret = Double.MAX_VALUE;
        }
        
        return ret;
    }

    /**
     * Access the initial domain value, based on <code>p</code>, used to
     * bracket a CDF root.  This method is used by
     * {@link #inverseCumulativeProbability(double)} to find critical values.
     * 
     * @param p the desired probability for the critical value
     * @return initial domain value
     */
    protected double getInitialDomain(double p) {
        double ret;

        if (p < .5) {
            ret = getMean() - getStandardDeviation();
        } else if (p > .5) {
            ret = getMean() + getStandardDeviation();
        } else {
            ret = getMean();
        }
        
        return ret;
    }
}
