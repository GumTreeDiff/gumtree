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
package org.apache.commons.math.special;

import java.io.Serializable;

import org.apache.commons.math.MathException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.util.ContinuedFraction;

/**
 * This is a utility class that provides computation methods related to the
 * Gamma family of functions.
 *
 * @version $Revision$ $Date$
 */
public class Gamma implements Serializable {
    
    /** Serializable version identifier */
    private static final long serialVersionUID = -6587513359895466954L;

    /** Maximum allowed numerical error. */
    private static final double DEFAULT_EPSILON = 10e-15;

    /** Lanczos coefficients */
    private static double[] lanczos =
    {
        0.99999999999999709182,
        57.156235665862923517,
        -59.597960355475491248,
        14.136097974741747174,
        -0.49191381609762019978,
        .33994649984811888699e-4,
        .46523628927048575665e-4,
        -.98374475304879564677e-4,
        .15808870322491248884e-3,
        -.21026444172410488319e-3,
        .21743961811521264320e-3,
        -.16431810653676389022e-3,
        .84418223983852743293e-4,
        -.26190838401581408670e-4,
        .36899182659531622704e-5,
    };

    /** Avoid repeated computation of log of 2 PI in logGamma */
    private static final double HALF_LOG_2_PI = 0.5 * Math.log(2.0 * Math.PI);

    
    /**
     * Default constructor.  Prohibit instantiation.
     */
    private Gamma() {
        super();
    }

    /**
     * Returns the natural logarithm of the gamma function &#915;(x).
     *
     * The implementation of this method is based on:
     * <ul>
     * <li><a href="http://mathworld.wolfram.com/GammaFunction.html">
     * Gamma Function</a>, equation (28).</li>
     * <li><a href="http://mathworld.wolfram.com/LanczosApproximation.html">
     * Lanczos Approximation</a>, equations (1) through (5).</li>
     * <li><a href="http://my.fit.edu/~gabdo/gamma.txt">Paul Godfrey, A note on
     * the computation of the convergent Lanczos complex Gamma approximation
     * </a></li>
     * </ul>
     * 
     * @param x the value.
     * @return log(&#915;(x))
     */
    public static double logGamma(double x) {
        double ret;

        if (Double.isNaN(x) || (x <= 0.0)) {
            ret = Double.NaN;
        } else {
            double g = 607.0 / 128.0;
            
            double sum = 0.0;
            for (int i = lanczos.length - 1; i > 0; --i) {
                sum = sum + (lanczos[i] / (x + i));
            }
            sum = sum + lanczos[0];

            double tmp = x + g + .5;
            ret = ((x + .5) * Math.log(tmp)) - tmp +
                HALF_LOG_2_PI + Math.log(sum / x);
        }

        return ret;
    }

    /**
     * Returns the regularized gamma function P(a, x).
     * 
     * @param a the a parameter.
     * @param x the value.
     * @return the regularized gamma function P(a, x)
     * @throws MathException if the algorithm fails to converge.
     */
    public static double regularizedGammaP(double a, double x)
        throws MathException
    {
        return regularizedGammaP(a, x, DEFAULT_EPSILON, Integer.MAX_VALUE);
    }
        
        
    /**
     * Returns the regularized gamma function P(a, x).
     * 
     * The implementation of this method is based on:
     * <ul>
     * <li>
     * <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
     * Regularized Gamma Function</a>, equation (1).</li>
     * <li>
     * <a href="http://mathworld.wolfram.com/IncompleteGammaFunction.html">
     * Incomplete Gamma Function</a>, equation (4).</li>
     * <li>
     * <a href="http://mathworld.wolfram.com/ConfluentHypergeometricFunctionoftheFirstKind.html">
     * Confluent Hypergeometric Function of the First Kind</a>, equation (1).
     * </li>
     * </ul>
     * 
     * @param a the a parameter.
     * @param x the value.
     * @param epsilon When the absolute value of the nth item in the
     *                series is less than epsilon the approximation ceases
     *                to calculate further elements in the series.
     * @param maxIterations Maximum number of "iterations" to complete. 
     * @return the regularized gamma function P(a, x)
     * @throws MathException if the algorithm fails to converge.
     */
    public static double regularizedGammaP(double a, 
                                           double x, 
                                           double epsilon, 
                                           int maxIterations) 
        throws MathException
    {
        double ret;

        if (Double.isNaN(a) || Double.isNaN(x) || (a <= 0.0) || (x < 0.0)) {
            ret = Double.NaN;
        } else if (x == 0.0) {
            ret = 0.0;
        } else if (a >= 1.0 && x > a) {
            // use regularizedGammaQ because it should converge faster in this
            // case.
            ret = 1.0 - regularizedGammaQ(a, x, epsilon, maxIterations);
        } else {
            // calculate series
            double n = 0.0; // current element index
            double an = 1.0 / a; // n-th element in the series
            double sum = an; // partial sum
            while (Math.abs(an) > epsilon && n < maxIterations) {
                // compute next element in the series
                n = n + 1.0;
                an = an * (x / (a + n));

                // update partial sum
                sum = sum + an;
            }
            if (n >= maxIterations) {
                throw new MaxIterationsExceededException(maxIterations);
            } else {
                ret = Math.exp(-x + (a * Math.log(x)) - logGamma(a)) * sum;
            }
        }

        return ret;
    }
    
    /**
     * Returns the regularized gamma function Q(a, x) = 1 - P(a, x).
     * 
     * @param a the a parameter.
     * @param x the value.
     * @return the regularized gamma function Q(a, x)
     * @throws MathException if the algorithm fails to converge.
     */
    public static double regularizedGammaQ(double a, double x)
        throws MathException
    {
        return regularizedGammaQ(a, x, DEFAULT_EPSILON, Integer.MAX_VALUE);
    }
    
    /**
     * Returns the regularized gamma function Q(a, x) = 1 - P(a, x).
     * 
     * The implementation of this method is based on:
     * <ul>
     * <li>
     * <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
     * Regularized Gamma Function</a>, equation (1).</li>
     * <li>
     * <a href="    http://functions.wolfram.com/GammaBetaErf/GammaRegularized/10/0003/">
     * Regularized incomplete gamma function: Continued fraction representations  (formula 06.08.10.0003)</a></li>
     * </ul>
     * 
     * @param a the a parameter.
     * @param x the value.
     * @param epsilon When the absolute value of the nth item in the
     *                series is less than epsilon the approximation ceases
     *                to calculate further elements in the series.
     * @param maxIterations Maximum number of "iterations" to complete. 
     * @return the regularized gamma function P(a, x)
     * @throws MathException if the algorithm fails to converge.
     */
    public static double regularizedGammaQ(final double a, 
                                           double x, 
                                           double epsilon, 
                                           int maxIterations) 
        throws MathException
    {
        double ret;

        if (Double.isNaN(a) || Double.isNaN(x) || (a <= 0.0) || (x < 0.0)) {
            ret = Double.NaN;
        } else if (x == 0.0) {
            ret = 1.0;
        } else if (x < a || a < 1.0) {
            // use regularizedGammaP because it should converge faster in this
            // case.
            ret = 1.0 - regularizedGammaP(a, x, epsilon, maxIterations);
        } else {
            // create continued fraction
            ContinuedFraction cf = new ContinuedFraction() {

                private static final long serialVersionUID = 5378525034886164398L;

                protected double getA(int n, double x) {
                    return ((2.0 * n) + 1.0) - a + x;
                }

                protected double getB(int n, double x) {
                    return n * (a - n);
                }
            };
            
            ret = 1.0 / cf.evaluate(x, epsilon, maxIterations);
            ret = Math.exp(-x + (a * Math.log(x)) - logGamma(a)) * ret;
        }

        return ret;
    }
}
