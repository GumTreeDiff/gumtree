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

package org.apache.commons.math.optimization.univariate;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.MathIllegalStateException;
import org.apache.commons.math.exception.ConvergenceException;
import org.apache.commons.math.exception.util.LocalizedFormats;
import org.apache.commons.math.random.RandomGenerator;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.ConvergenceChecker;
import org.apache.commons.math.util.FastMath;

/**
 * Special implementation of the {@link UnivariateRealOptimizer} interface
 * adding multi-start features to an existing optimizer.
 *
 * This class wraps a classical optimizer to use it several times in
 * turn with different starting points in order to avoid being trapped
 * into a local extremum when looking for a global one.
 *
 * @param <FUNC> Type of the objective function to be optimized.
 *
 * @version $Revision$ $Date$
 * @since 3.0
 */
public class MultiStartUnivariateRealOptimizer<FUNC extends UnivariateRealFunction>
    implements BaseUnivariateRealOptimizer<FUNC> {
    /** Underlying classical optimizer. */
    private final BaseUnivariateRealOptimizer<FUNC> optimizer;
    /** Maximal number of evaluations allowed. */
    private int maxEvaluations;
    /** Number of evaluations already performed for all starts. */
    private int totalEvaluations;
    /** Number of starts to go. */
    private int starts;
    /** Random generator for multi-start. */
    private RandomGenerator generator;
    /** Found optima. */
    private UnivariateRealPointValuePair[] optima;

    /**
     * Create a multi-start optimizer from a single-start optimizer.
     *
     * @param optimizer Single-start optimizer to wrap.
     * @param starts Number of starts to perform (including the
     * first one), multi-start is disabled if value is less than or
     * equal to 1.
     * @param generator Random generator to use for restarts.
     */
    public MultiStartUnivariateRealOptimizer(final BaseUnivariateRealOptimizer<FUNC> optimizer,
                                             final int starts,
                                             final RandomGenerator generator) {
        this.optimizer = optimizer;
        this.starts = starts;
        this.generator = generator;
    }

    /**
     * {@inheritDoc}
     */
    public void setConvergenceChecker(ConvergenceChecker<UnivariateRealPointValuePair> checker) {
        optimizer.setConvergenceChecker(checker);
    }

    /**
     * {@inheritDoc}
     */
    public ConvergenceChecker<UnivariateRealPointValuePair> getConvergenceChecker() {
        return optimizer.getConvergenceChecker();
    }

    /** {@inheritDoc} */
    public int getMaxEvaluations() {
        return maxEvaluations;
    }

    /** {@inheritDoc} */
    public int getEvaluations() {
        return totalEvaluations;
    }

    /** {@inheritDoc} */
    public void setMaxEvaluations(int maxEvaluations) {
        this.maxEvaluations = maxEvaluations;
        optimizer.setMaxEvaluations(maxEvaluations);
    }

    /**
     * Get all the optima found during the last call to {@link
     * #optimize(FUNC,GoalType,double,double) optimize}.
     * The optimizer stores all the optima found during a set of
     * restarts. The {@link #optimize(FUNC,GoalType,double,double) optimize}
     * method returns the best point only. This method returns all the points
     * found at the end of each starts, including the best one already
     * returned by the {@link #optimize(FUNC,GoalType,double,double) optimize}
     * method.
     * <br/>
     * The returned array as one element for each start as specified
     * in the constructor. It is ordered with the results from the
     * runs that did converge first, sorted from best to worst
     * objective value (i.e in ascending order if minimizing and in
     * descending order if maximizing), followed by {@code null} elements
     * corresponding to the runs that did not converge. This means all
     * elements will be {@code null} if the {@link
     * #optimize(FUNC,GoalType,double,double) optimize} method did throw a
     * {@link ConvergenceException}). This also means that if the first
     * element is not {@code null}, it is the best point found across all
     * starts.
     *
     * @return an array containing the optima.
     * @throws MathIllegalStateException if {@link
     * #optimize(FUNC,GoalType,double,double) optimize} has not been called.
     */
    public UnivariateRealPointValuePair[] getOptima() {
        if (optima == null) {
            throw new MathIllegalStateException(LocalizedFormats.NO_OPTIMUM_COMPUTED_YET);
        }
        return optima.clone();
    }

    /** {@inheritDoc} */
    public UnivariateRealPointValuePair optimize(final FUNC f,
                                                 final GoalType goal,
                                                 final double min, final double max)
        throws FunctionEvaluationException {
        return optimize(f, goal, min, max, min + 0.5 * (max - min));
    }

    /** {@inheritDoc} */
    public UnivariateRealPointValuePair optimize(final FUNC f, final GoalType goal,
                                                 final double min, final double max,
                                                 final double startValue)
        throws FunctionEvaluationException {
        optima = new UnivariateRealPointValuePair[starts];
        totalEvaluations = 0;

        // Multi-start loop.
        for (int i = 0; i < starts; ++i) {
            try {
                final double s = (i == 0) ? startValue : min + generator.nextDouble() * (max - min);
                optima[i] = optimizer.optimize(f, goal, min, max, s);
            } catch (FunctionEvaluationException fee) {
                optima[i] = null;
            } catch (ConvergenceException ce) {
                optima[i] = null;
            }

            final int usedEvaluations = optimizer.getEvaluations();
            optimizer.setMaxEvaluations(optimizer.getMaxEvaluations() - usedEvaluations);
            totalEvaluations += usedEvaluations;
        }

        sortPairs(goal);

        if (optima[0] == null) {
            throw new ConvergenceException(LocalizedFormats.NO_CONVERGENCE_WITH_ANY_START_POINT,
                                           starts);
        }

        // Return the point with the best objective function value.
        return optima[0];
    }

    /**
     * Sort the optima from best to worst, followed by {@code null} elements.
     *
     * @param goal Goal type.
     */
    private void sortPairs(final GoalType goal) {
        Arrays.sort(optima, new Comparator<UnivariateRealPointValuePair>() {
                public int compare(final UnivariateRealPointValuePair o1,
                                   final UnivariateRealPointValuePair o2) {
                    if (o1 == null) {
                        return (o2 == null) ? 0 : 1;
                    } else if (o2 == null) {
                        return -1;
                    }
                    final double v1 = o1.getValue();
                    final double v2 = o2.getValue();
                    return (goal == GoalType.MINIMIZE) ?
                        Double.compare(v1, v2) : Double.compare(v2, v1);
                }
            });
    }
}
