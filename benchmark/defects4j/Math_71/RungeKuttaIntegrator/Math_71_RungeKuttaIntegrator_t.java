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

package org.apache.commons.math.ode.nonstiff;


import org.apache.commons.math.ode.AbstractIntegrator;
import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math.ode.IntegratorException;
import org.apache.commons.math.ode.events.CombinedEventsManager;
import org.apache.commons.math.ode.sampling.AbstractStepInterpolator;
import org.apache.commons.math.ode.sampling.DummyStepInterpolator;
import org.apache.commons.math.ode.sampling.StepHandler;

/**
 * This class implements the common part of all fixed step Runge-Kutta
 * integrators for Ordinary Differential Equations.
 *
 * <p>These methods are explicit Runge-Kutta methods, their Butcher
 * arrays are as follows :
 * <pre>
 *    0  |
 *   c2  | a21
 *   c3  | a31  a32
 *   ... |        ...
 *   cs  | as1  as2  ...  ass-1
 *       |--------------------------
 *       |  b1   b2  ...   bs-1  bs
 * </pre>
 * </p>
 *
 * @see EulerIntegrator
 * @see ClassicalRungeKuttaIntegrator
 * @see GillIntegrator
 * @see MidpointIntegrator
 * @version $Revision$ $Date$
 * @since 1.2
 */

public abstract class RungeKuttaIntegrator extends AbstractIntegrator {

    /** Time steps from Butcher array (without the first zero). */
    private final double[] c;

    /** Internal weights from Butcher array (without the first empty row). */
    private final double[][] a;

    /** External weights for the high order method from Butcher array. */
    private final double[] b;

    /** Prototype of the step interpolator. */
    private final RungeKuttaStepInterpolator prototype;

    /** Integration step. */
    private final double step;

  /** Simple constructor.
   * Build a Runge-Kutta integrator with the given
   * step. The default step handler does nothing.
   * @param name name of the method
   * @param c time steps from Butcher array (without the first zero)
   * @param a internal weights from Butcher array (without the first empty row)
   * @param b propagation weights for the high order method from Butcher array
   * @param prototype prototype of the step interpolator to use
   * @param step integration step
   */
  protected RungeKuttaIntegrator(final String name,
                                 final double[] c, final double[][] a, final double[] b,
                                 final RungeKuttaStepInterpolator prototype,
                                 final double step) {
    super(name);
    this.c          = c;
    this.a          = a;
    this.b          = b;
    this.prototype  = prototype;
    this.step       = Math.abs(step);
  }

  /** {@inheritDoc} */
  public double integrate(final FirstOrderDifferentialEquations equations,
                          final double t0, final double[] y0,
                          final double t, final double[] y)
  throws DerivativeException, IntegratorException {

    sanityChecks(equations, t0, y0, t, y);
    setEquations(equations);
    resetEvaluations();
    final boolean forward = t > t0;

    // create some internal working arrays
    final int stages = c.length + 1;
    if (y != y0) {
      System.arraycopy(y0, 0, y, 0, y0.length);
    }
    final double[][] yDotK = new double[stages][];
    for (int i = 0; i < stages; ++i) {
      yDotK [i] = new double[y0.length];
    }
    final double[] yTmp = new double[y0.length];

    // set up an interpolator sharing the integrator arrays
    AbstractStepInterpolator interpolator;
    if (requiresDenseOutput() || (! eventsHandlersManager.isEmpty())) {
      final RungeKuttaStepInterpolator rki = (RungeKuttaStepInterpolator) prototype.copy();
      rki.reinitialize(this, yTmp, yDotK, forward);
      interpolator = rki;
    } else {
      interpolator = new DummyStepInterpolator(yTmp, yDotK[stages - 1], forward);
    }
    interpolator.storeTime(t0);

    // set up integration control objects
    stepStart = t0;
    stepSize  = forward ? step : -step;
    for (StepHandler handler : stepHandlers) {
        handler.reset();
    }
    CombinedEventsManager manager = addEndTimeChecker(t0, t, eventsHandlersManager);
    boolean lastStep = false;

    // main integration loop
    while (!lastStep) {

      interpolator.shift();

      for (boolean loop = true; loop;) {

        // first stage
        computeDerivatives(stepStart, y, yDotK[0]);

        // next stages
        for (int k = 1; k < stages; ++k) {

          for (int j = 0; j < y0.length; ++j) {
            double sum = a[k-1][0] * yDotK[0][j];
            for (int l = 1; l < k; ++l) {
              sum += a[k-1][l] * yDotK[l][j];
            }
            yTmp[j] = y[j] + stepSize * sum;
          }

          computeDerivatives(stepStart + c[k-1] * stepSize, yTmp, yDotK[k]);

        }

        // estimate the state at the end of the step
        for (int j = 0; j < y0.length; ++j) {
          double sum    = b[0] * yDotK[0][j];
          for (int l = 1; l < stages; ++l) {
            sum    += b[l] * yDotK[l][j];
          }
          yTmp[j] = y[j] + stepSize * sum;
        }

        // discrete events handling
        interpolator.storeTime(stepStart + stepSize);
        if (manager.evaluateStep(interpolator)) {
            final double dt = manager.getEventTime() - stepStart;
            if (Math.abs(dt) <= Math.ulp(stepStart)) {
                // we cannot simply truncate the step, reject the current computation
                // and let the loop compute another state with the truncated step.
                // it is so small (much probably exactly 0 due to limited accuracy)
                // that the code above would fail handling it.
                // So we set up an artificial 0 size step by copying states
                interpolator.storeTime(stepStart);
                System.arraycopy(y, 0, yTmp, 0, y0.length);
                stepSize = 0;
                loop     = false;
            } else {
                // reject the step to match exactly the next switch time
                stepSize = dt;
            }
        } else {
          loop = false;
        }

      }

      // the step has been accepted
      final double nextStep = stepStart + stepSize;
      System.arraycopy(yTmp, 0, y, 0, y0.length);
      manager.stepAccepted(nextStep, y);
      lastStep = manager.stop();

      // provide the step data to the step handler
      interpolator.storeTime(nextStep);
      for (StepHandler handler : stepHandlers) {
          handler.handleStep(interpolator, lastStep);
      }
      stepStart = nextStep;

      if (manager.reset(stepStart, y) && ! lastStep) {
        // some events handler has triggered changes that
        // invalidate the derivatives, we need to recompute them
        computeDerivatives(stepStart, y, yDotK[0]);
      }

      // make sure step size is set to default before next step
      stepSize = forward ? step : -step;

    }

    final double stopTime = stepStart;
    stepStart = Double.NaN;
    stepSize  = Double.NaN;
    return stopTime;

  }

}
