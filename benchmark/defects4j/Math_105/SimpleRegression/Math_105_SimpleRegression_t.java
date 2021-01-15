/*
 * Copyright 2003-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.math.stat.regression;
import java.io.Serializable;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.DistributionFactory;
import org.apache.commons.math.distribution.TDistribution;

/**
 * Estimates an ordinary least squares regression model
 * with one independent variable.
 * <p>
 * <code> y = intercept + slope * x  </code>
 * <p>
 * Standard errors for <code>intercept</code> and <code>slope</code> are 
 * available as well as ANOVA, r-square and Pearson's r statistics.
 * <p>
 * Observations (x,y pairs) can be added to the model one at a time or they 
 * can be provided in a 2-dimensional array.  The observations are not stored
 * in memory, so there is no limit to the number of observations that can be
 * added to the model. 
 * <p>
 * <strong>Usage Notes</strong>: <ul>
 * <li> When there are fewer than two observations in the model, or when
 * there is no variation in the x values (i.e. all x values are the same) 
 * all statistics return <code>NaN</code>. At least two observations with
 * different x coordinates are requred to estimate a bivariate regression 
 * model.
 * </li>
 * <li> getters for the statistics always compute values based on the current
 * set of observations -- i.e., you can get statistics, then add more data
 * and get updated statistics without using a new instance.  There is no 
 * "compute" method that updates all statistics.  Each of the getters performs
 * the necessary computations to return the requested statistic.</li>
 * </ul>
 *
 * @version $Revision$ $Date$
 */
public class SimpleRegression implements Serializable {

    /** Serializable version identifier */
    private static final long serialVersionUID = -3004689053607543335L;

    /** sum of x values */
    private double sumX = 0d;

    /** total variation in x (sum of squared deviations from xbar) */
    private double sumXX = 0d;

    /** sum of y values */
    private double sumY = 0d;

    /** total variation in y (sum of squared deviations from ybar) */
    private double sumYY = 0d;

    /** sum of products */
    private double sumXY = 0d;

    /** number of observations */
    private long n = 0;

    /** mean of accumulated x values, used in updating formulas */
    private double xbar = 0;

    /** mean of accumulated y values, used in updating formulas */
    private double ybar = 0;

    // ---------------------Public methods--------------------------------------

    /**
     * Create an empty SimpleRegression instance
     */
    public SimpleRegression() {
        super();
    }
    
    /**
     * Adds the observation (x,y) to the regression data set.
     * <p>
     * Uses updating formulas for means and sums of squares defined in 
     * "Algorithms for Computing the Sample Variance: Analysis and
     * Recommendations", Chan, T.F., Golub, G.H., and LeVeque, R.J. 
     * 1983, American Statistician, vol. 37, pp. 242-247, referenced in
     * Weisberg, S. "Applied Linear Regression". 2nd Ed. 1985
     *
     *
     * @param x independent variable value
     * @param y dependent variable value
     */
    public void addData(double x, double y) {
        if (n == 0) {
            xbar = x;
            ybar = y;
        } else {
            double dx = x - xbar;
            double dy = y - ybar;
            sumXX += dx * dx * (double) n / (double) (n + 1.0);
            sumYY += dy * dy * (double) n / (double) (n + 1.0);
            sumXY += dx * dy * (double) n / (double) (n + 1.0);
            xbar += dx / (double) (n + 1.0);
            ybar += dy / (double) (n + 1.0);
        }
        sumX += x;
        sumY += y;
        n++;
    }

    /**
     * Adds the observations represented by the elements in 
     * <code>data</code>.
     * <p>
     * <code>(data[0][0],data[0][1])</code> will be the first observation, then
     * <code>(data[1][0],data[1][1])</code>, etc. 
     * <p> 
     * This method does not replace data that has already been added.  The
     * observations represented by <code>data</code> are added to the existing
     * dataset.
     * <p> 
     * To replace all data, use <code>clear()</code> before adding the new 
     * data.
     * 
     * @param data array of observations to be added
     */
    public void addData(double[][] data) {
        for (int i = 0; i < data.length; i++) {
            addData(data[i][0], data[i][1]);
        }
    }

    /**
     * Clears all data from the model.
     */
    public void clear() {
        sumX = 0d;
        sumXX = 0d;
        sumY = 0d;
        sumYY = 0d;
        sumXY = 0d;
        n = 0;
    }

    /**
     * Returns the number of observations that have been added to the model.
     *
     * @return n number of observations that have been added.
     */
    public long getN() {
        return n;
    }

    /**
     * Returns the "predicted" <code>y</code> value associated with the 
     * supplied <code>x</code> value,  based on the data that has been
     * added to the model when this method is activated.
     * <p>
     * <code> predict(x) = intercept + slope * x </code>
     * <p>
     * <strong>Preconditions</strong>: <ul>
     * <li>At least two observations (with at least two different x values)
     * must have been added before invoking this method. If this method is 
     * invoked before a model can be estimated, <code>Double,NaN</code> is
     * returned.
     * </li></ul>
     *
     * @param x input <code>x</code> value
     * @return predicted <code>y</code> value
     */
    public double predict(double x) {
        double b1 = getSlope();
        return getIntercept(b1) + b1 * x;
    }

    /**
     * Returns the intercept of the estimated regression line.
     * <p>
     * The least squares estimate of the intercept is computed using the 
     * <a href="http://www.xycoon.com/estimation4.htm">normal equations</a>.
     * The intercept is sometimes denoted b0. 
     * <p>
     * <strong>Preconditions</strong>: <ul>
     * <li>At least two observations (with at least two different x values)
     * must have been added before invoking this method. If this method is 
     * invoked before a model can be estimated, <code>Double,NaN</code> is
     * returned.
     * </li></ul>
     *
     * @return the intercept of the regression line
     */
    public double getIntercept() {
        return getIntercept(getSlope());
    }

    /**
    * Returns the slope of the estimated regression line.  
    * <p>
    * The least squares estimate of the slope is computed using the 
    * <a href="http://www.xycoon.com/estimation4.htm">normal equations</a>.
    * The slope is sometimes denoted b1. 
    * <p>
    * <strong>Preconditions</strong>: <ul>
    * <li>At least two observations (with at least two different x values)
    * must have been added before invoking this method. If this method is 
    * invoked before a model can be estimated, <code>Double.NaN</code> is
    * returned.
    * </li></ul>
    *
    * @return the slope of the regression line
    */
    public double getSlope() {
        if (n < 2) {
            return Double.NaN; //not enough data 
        }
        if (Math.abs(sumXX) < 10 * Double.MIN_VALUE) {
            return Double.NaN; //not enough variation in x
        }
        return sumXY / sumXX;
    }

    /**
     * Returns the <a href="http://www.xycoon.com/SumOfSquares.htm">
     * sum of squared errors</a> (SSE) associated with the regression 
     * model.
     * <p>
     * The sum is computed using the computational formula
     * <p>
     * <code>SSE = SYY - (SXY * SXY / SXX)</code>
     * <p>
     * where <code>SYY</code> is the sum of the squared deviations of the y
     * values about their mean, <code>SXX</code> is similarly defined and
     * <code>SXY</code> is the sum of the products of x and y mean deviations.
     * <p>
     * The sums are accumulated using the updating algorithm referenced in 
     * {@link #addData}.  
     * <p>
     * The return value is constrained to be non-negative - i.e., if due to 
     * rounding errors the computational formula returns a negative result, 
     * 0 is returned.
     * <p>
     * <strong>Preconditions</strong>: <ul>
     * <li>At least two observations (with at least two different x values)
     * must have been added before invoking this method. If this method is 
     * invoked before a model can be estimated, <code>Double,NaN</code> is
     * returned.
     * </li></ul>
     *
     * @return sum of squared errors associated with the regression model
     */
    public double getSumSquaredErrors() {
        return Math.max(0d, sumYY - sumXY * sumXY / sumXX);
    }

    /**
     * Returns the sum of squared deviations of the y values about their mean.
     * <p>
     * This is defined as SSTO 
     * <a href="http://www.xycoon.com/SumOfSquares.htm">here</a>.
     * <p>
     * If <code>n < 2</code>, this returns <code>Double.NaN</code>.
     *
     * @return sum of squared deviations of y values
     */
    public double getTotalSumSquares() {
        if (n < 2) {
            return Double.NaN;
        }
        return sumYY;
    }

    /**
     * Returns the sum of squared deviations of the predicted y values about 
     * their mean (which equals the mean of y).
     * <p>
     * This is usually abbreviated SSR or SSM.  It is defined as SSM 
     * <a href="http://www.xycoon.com/SumOfSquares.htm">here</a>
     * <p>
     * <strong>Preconditions</strong>: <ul>
     * <li>At least two observations (with at least two different x values)
     * must have been added before invoking this method. If this method is 
     * invoked before a model can be estimated, <code>Double.NaN</code> is
     * returned.
     * </li></ul>
     *
     * @return sum of squared deviations of predicted y values
     */
    public double getRegressionSumSquares() {
        return getRegressionSumSquares(getSlope());
    }

    /**
     * Returns the sum of squared errors divided by the degrees of freedom,
     * usually abbreviated MSE. 
     * <p>
     * If there are fewer than <strong>three</strong> data pairs in the model,
     * or if there is no variation in <code>x</code>, this returns 
     * <code>Double.NaN</code>.
     *
     * @return sum of squared deviations of y values
     */
    public double getMeanSquareError() {
        if (n < 3) {
            return Double.NaN;
        }
        return getSumSquaredErrors() / (double) (n - 2);
    }

    /**
     * Returns <a href="http://mathworld.wolfram.com/CorrelationCoefficient.html">
     * Pearson's product moment correlation coefficient</a>,
     * usually denoted r. 
     * <p>
     * <strong>Preconditions</strong>: <ul>
     * <li>At least two observations (with at least two different x values)
     * must have been added before invoking this method. If this method is 
     * invoked before a model can be estimated, <code>Double,NaN</code> is
     * returned.
     * </li></ul>
     *
     * @return Pearson's r
     */
    public double getR() {
        double b1 = getSlope();
        double result = Math.sqrt(getRSquare());
        if (b1 < 0) {
            result = -result;
        }
        return result;
    }

    /** 
     * Returns the <a href="http://www.xycoon.com/coefficient1.htm"> 
     * coefficient of determination</a>,
     * usually denoted r-square. 
     * <p>
     * <strong>Preconditions</strong>: <ul>
     * <li>At least two observations (with at least two different x values)
     * must have been added before invoking this method. If this method is 
     * invoked before a model can be estimated, <code>Double,NaN</code> is
     * returned.
     * </li></ul>
     *
     * @return r-square
     */
    public double getRSquare() {
        double ssto = getTotalSumSquares();
        return (ssto - getSumSquaredErrors()) / ssto;
    }

    /**
     * Returns the <a href="http://www.xycoon.com/standarderrorb0.htm">
     * standard error of the intercept estimate</a>, 
     * usually denoted s(b0). 
     * <p>
     * If there are fewer that <strong>three</strong> observations in the 
     * model, or if there is no variation in x, this returns 
     * <code>Double.NaN</code>.
     *
     * @return standard error associated with intercept estimate
     */
    public double getInterceptStdErr() {
        return Math.sqrt(
            getMeanSquareError() * ((1d / (double) n) + (xbar * xbar) / sumXX));
    }

    /**
     * Returns the <a href="http://www.xycoon.com/standerrorb(1).htm">standard
     * error of the slope estimate</a>,
     * usually denoted s(b1). 
     * <p>
     * If there are fewer that <strong>three</strong> data pairs in the model,
     * or if there is no variation in x, this returns <code>Double.NaN</code>.
     *
     * @return standard error associated with slope estimate
     */
    public double getSlopeStdErr() {
        return Math.sqrt(getMeanSquareError() / sumXX);
    }

    /**
     * Returns the half-width of a 95% confidence interval for the slope
     * estimate.
     * <p>
     * The 95% confidence interval is 
     * <p>
     * <code>(getSlope() - getSlopeConfidenceInterval(), 
     * getSlope() + getSlopeConfidenceInterval())</code>
     * <p>
     * If there are fewer that <strong>three</strong> observations in the 
     * model, or if there is no variation in x, this returns 
     * <code>Double.NaN</code>.
     * <p>
     * <strong>Usage Note</strong>:<br>
     * The validity of this statistic depends on the assumption that the 
     * observations included in the model are drawn from a
     * <a href="http://mathworld.wolfram.com/BivariateNormalDistribution.html">
     * Bivariate Normal Distribution</a>.
     *
     * @return half-width of 95% confidence interval for the slope estimate
     * 
     * @throws MathException if the confidence interval can not be computed.
     */
    public double getSlopeConfidenceInterval() throws MathException {
        return getSlopeConfidenceInterval(0.05d);
    }

    /**
     * Returns the half-width of a (100-100*alpha)% confidence interval for 
     * the slope estimate.
     * <p>
     * The (100-100*alpha)% confidence interval is 
     * <p>
     * <code>(getSlope() - getSlopeConfidenceInterval(), 
     * getSlope() + getSlopeConfidenceInterval())</code>
     * <p>
     * To request, for example, a 99% confidence interval, use 
     * <code>alpha = .01</code>
     * <p>
     * <strong>Usage Note</strong>:<br>
     * The validity of this statistic depends on the assumption that the 
     * observations included in the model are drawn from a
     * <a href="http://mathworld.wolfram.com/BivariateNormalDistribution.html">
     * Bivariate Normal Distribution</a>.
     * <p>
     * <strong> Preconditions:</strong><ul>
     * <li>If there are fewer that <strong>three</strong> observations in the 
     * model, or if there is no variation in x, this returns 
     * <code>Double.NaN</code>. 
     * </li>
     * <li><code>(0 < alpha < 1)</code>; otherwise an 
     * <code>IllegalArgumentException</code> is thrown.
     * </li></ul>    
     *
     * @param alpha the desired significance level 
     * @return half-width of 95% confidence interval for the slope estimate
     * @throws MathException if the confidence interval can not be computed.
     */
    public double getSlopeConfidenceInterval(double alpha)
        throws MathException {
        if (alpha >= 1 || alpha <= 0) {
            throw new IllegalArgumentException();
        }
        return getSlopeStdErr() *
            getTDistribution().inverseCumulativeProbability(1d - alpha / 2d);
    }

    /**
     * Returns the significance level of the slope (equiv) correlation. 
     * <p>
     * Specifically, the returned value is the smallest <code>alpha</code>
     * such that the slope confidence interval with significance level
     * equal to <code>alpha</code> does not include <code>0</code>.
     * On regression output, this is often denoted <code>Prob(|t| > 0)</code>
     * <p>
     * <strong>Usage Note</strong>:<br>
     * The validity of this statistic depends on the assumption that the 
     * observations included in the model are drawn from a
     * <a href="http://mathworld.wolfram.com/BivariateNormalDistribution.html">
     * Bivariate Normal Distribution</a>.
     * <p>
     * If there are fewer that <strong>three</strong> observations in the 
     * model, or if there is no variation in x, this returns 
     * <code>Double.NaN</code>.
     *
     * @return significance level for slope/correlation
     * @throws MathException if the significance level can not be computed.
     */
    public double getSignificance() throws MathException {
        return 2d* (1.0 - getTDistribution().cumulativeProbability(
                    Math.abs(getSlope()) / getSlopeStdErr()));
    }

    // ---------------------Private methods-----------------------------------

    /**
    * Returns the intercept of the estimated regression line, given the slope.
    * <p>
    * Will return <code>NaN</code> if slope is <code>NaN</code>.
    *
    * @param slope current slope
    * @return the intercept of the regression line
    */
    private double getIntercept(double slope) {
        return (sumY - slope * sumX) / ((double) n);
    }

    /**
     * Computes SSR from b1.
     * 
     * @param slope regression slope estimate
     * @return sum of squared deviations of predicted y values
     */
    private double getRegressionSumSquares(double slope) {
        return slope * slope * sumXX;
    }

    /**
     * Uses distribution framework to get a t distribution instance 
     * with df = n - 2
     *
     * @return t distribution with df = n - 2
     */
    private TDistribution getTDistribution() {
        return DistributionFactory.newInstance().createTDistribution(n - 2);
    }
}
