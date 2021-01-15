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
package org.apache.commons.math.analysis;


import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;

/**
 * Implements the <a href="http://mathworld.wolfram.com/BrentsMethod.html">
 * Brent algorithm</a> for  finding zeros of real univariate functions.
 * <p>
 * The function should be continuous but not necessarily smooth.</p>
 *  
 * @version $Revision:670469 $ $Date:2008-06-23 10:01:38 +0200 (lun., 23 juin 2008) $
 */
public class BrentSolver extends UnivariateRealSolverImpl {
    
    /** Serializable version identifier */
    private static final long serialVersionUID = -2136672307739067002L;

    /**
     * Construct a solver for the given function.
     * 
     * @param f function to solve.
     */
    public BrentSolver(UnivariateRealFunction f) {
        super(f, 100, 1E-6);
    }

    /**
     * Find a zero in the given interval with an initial guess.
     * <p>Throws <code>IllegalArgumentException</code> if the values of the
     * function at the three points have the same sign (note that it is
     * allowed to have endpoints with the same sign if the initial point has
     * opposite sign function-wise).</p>
     * 
     * @param min the lower bound for the interval.
     * @param max the upper bound for the interval.
     * @param initial the start value to use (must be set to min if no
     * initial point is known).
     * @return the value where the function is zero
     * @throws MaxIterationsExceededException the maximum iteration count
     * is exceeded 
     * @throws FunctionEvaluationException if an error occurs evaluating
     *  the function
     * @throws IllegalArgumentException if initial is not between min and max
     * (even if it <em>is</em> a root)
     */
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        if (((initial - min) * (max -initial)) < 0) {
            throw new IllegalArgumentException("Initial guess is not in search" +
                      " interval." + "  Initial: " + initial +
                      "  Endpoints: [" + min + "," + max + "]");
        }

        // return the initial guess if it is good enough
        double yInitial = f.value(initial);
        if (Math.abs(yInitial) <= functionValueAccuracy) {
            setResult(initial, 0);
            return result;
        }

        // return the first endpoint if it is good enough
        double yMin = f.value(min);
        if (Math.abs(yMin) <= functionValueAccuracy) {
            setResult(yMin, 0);
            return result;
        }

        // reduce interval if min and initial bracket the root
        if (yInitial * yMin < 0) {
            return solve(min, yMin, initial, yInitial, min, yMin);
        }

        // return the second endpoint if it is good enough
        double yMax = f.value(max);
        if (Math.abs(yMax) <= functionValueAccuracy) {
            setResult(yMax, 0);
            return result;
        }

        // reduce interval if initial and max bracket the root
        if (yInitial * yMax < 0) {
            return solve(initial, yInitial, max, yMax, initial, yInitial);
        }

        // full Brent algorithm starting with provided initial guess
        return solve(min, yMin, max, yMax, initial, yInitial);

    }
    
    /**
     * Find a zero in the given interval.
     * <p>
     * Requires that the values of the function at the endpoints have opposite
     * signs. An <code>IllegalArgumentException</code> is thrown if this is not
     * the case.</p>
     * 
     * @param min the lower bound for the interval.
     * @param max the upper bound for the interval.
     * @return the value where the function is zero
     * @throws MaxIterationsExceededException if the maximum iteration count is exceeded
     * @throws FunctionEvaluationException if an error occurs evaluating the
     * function 
     * @throws IllegalArgumentException if min is not less than max or the
     * signs of the values of the function at the endpoints are not opposites
     */
    public double solve(double min, double max) throws MaxIterationsExceededException, 
        FunctionEvaluationException {
        
        clearResult();
        verifyInterval(min, max);
        
        double ret = Double.NaN;
        
        double yMin = f.value(min);
        double yMax = f.value(max);
        
        // Verify bracketing
        double sign = yMin * yMax;
        if (sign >= 0) {
            // check if either value is close to a zero
                // neither value is close to zero and min and max do not bracket root.
                throw new IllegalArgumentException
                ("Function values at endpoints do not have different signs." +
                        "  Endpoints: [" + min + "," + max + "]" + 
                        "  Values: [" + yMin + "," + yMax + "]");
        } else {
            // solve using only the first endpoint as initial guess
            ret = solve(min, yMin, max, yMax, min, yMin);
            // either min or max is a root
        }

        return ret;
    }
        
    /**
     * Find a zero starting search according to the three provided points.
     * @param x0 old approximation for the root
     * @param y0 function value at the approximation for the root
     * @param x1 last calculated approximation for the root
     * @param y1 function value at the last calculated approximation
     * for the root
     * @param x2 bracket point (must be set to x0 if no bracket point is
     * known, this will force starting with linear interpolation)
     * @param y2 function value at the bracket point.
     * @return the value where the function is zero
     * @throws MaxIterationsExceededException if the maximum iteration count
     * is exceeded
     * @throws FunctionEvaluationException if an error occurs evaluating
     * the function 
     */
    private double solve(double x0, double y0,
                         double x1, double y1,
                         double x2, double y2)
    throws MaxIterationsExceededException, FunctionEvaluationException {

        double delta = x1 - x0;
        double oldDelta = delta;

        int i = 0;
        while (i < maximalIterationCount) {
            if (Math.abs(y2) < Math.abs(y1)) {
                // use the bracket point if is better than last approximation
                x0 = x1;
                x1 = x2;
                x2 = x0;
                y0 = y1;
                y1 = y2;
                y2 = y0;
            }
            if (Math.abs(y1) <= functionValueAccuracy) {
                // Avoid division by very small values. Assume
                // the iteration has converged (the problem may
                // still be ill conditioned)
                setResult(x1, i);
                return result;
            }
            double dx = (x2 - x1);
            double tolerance =
                Math.max(relativeAccuracy * Math.abs(x1), absoluteAccuracy);
            if (Math.abs(dx) <= tolerance) {
                setResult(x1, i);
                return result;
            }
            if ((Math.abs(oldDelta) < tolerance) ||
                    (Math.abs(y0) <= Math.abs(y1))) {
                // Force bisection.
                delta = 0.5 * dx;
                oldDelta = delta;
            } else {
                double r3 = y1 / y0;
                double p;
                double p1;
                // the equality test (x0 == x2) is intentional,
                // it is part of the original Brent's method,
                // it should NOT be replaced by proximity test
                if (x0 == x2) {
                    // Linear interpolation.
                    p = dx * r3;
                    p1 = 1.0 - r3;
                } else {
                    // Inverse quadratic interpolation.
                    double r1 = y0 / y2;
                    double r2 = y1 / y2;
                    p = r3 * (dx * r1 * (r1 - r2) - (x1 - x0) * (r2 - 1.0));
                    p1 = (r1 - 1.0) * (r2 - 1.0) * (r3 - 1.0);
                }
                if (p > 0.0) {
                    p1 = -p1;
                } else {
                    p = -p;
                }
                if (2.0 * p >= 1.5 * dx * p1 - Math.abs(tolerance * p1) ||
                        p >= Math.abs(0.5 * oldDelta * p1)) {
                    // Inverse quadratic interpolation gives a value
                    // in the wrong direction, or progress is slow.
                    // Fall back to bisection.
                    delta = 0.5 * dx;
                    oldDelta = delta;
                } else {
                    oldDelta = delta;
                    delta = p / p1;
                }
            }
            // Save old X1, Y1 
            x0 = x1;
            y0 = y1;
            // Compute new X1, Y1
            if (Math.abs(delta) > tolerance) {
                x1 = x1 + delta;
            } else if (dx > 0.0) {
                x1 = x1 + 0.5 * tolerance;
            } else if (dx <= 0.0) {
                x1 = x1 - 0.5 * tolerance;
            }
            y1 = f.value(x1);
            if ((y1 > 0) == (y2 > 0)) {
                x2 = x0;
                y2 = y0;
                delta = x1 - x0;
                oldDelta = delta;
            }
            i++;
        }
        throw new MaxIterationsExceededException(maximalIterationCount);
    }
}
