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

package org.apache.commons.math.linear;

import org.apache.commons.math.MathRuntimeException;
import org.apache.commons.math.util.MathUtils;

/**
 * Calculates the compact or truncated Singular Value Decomposition of a matrix.
 * <p>The Singular Value Decomposition of matrix A is a set of three matrices:
 * U, &Sigma; and V such that A = U &times; &Sigma; &times; V<sup>T</sup>.
 * Let A be a m &times; n matrix, then U is a m &times; p orthogonal matrix,
 * &Sigma; is a p &times; p diagonal matrix with positive diagonal elements,
 * V is a n &times; p orthogonal matrix (hence V<sup>T</sup> is a p &times; n
 * orthogonal matrix). The size p depends on the chosen algorithm:
 * <ul>
 *   <li>for full SVD, p would be n, but this is not supported by this implementation,</li>
 *   <li>for compact SVD, p is the rank r of the matrix
 *       (i. e. the number of positive singular values),</li>
 *   <li>for truncated SVD p is min(r, t) where t is user-specified.</li>
 * </ul>
 * </p>
 * <p>
 * Note that since this class computes only the compact or truncated SVD and not
 * the full SVD, the singular values computed are always positive.
 * </p>
 *
 * @version $Revision$ $Date$
 * @since 2.0
 */
public class SingularValueDecompositionImpl implements SingularValueDecomposition {

    /** Number of rows of the initial matrix. */
    private int m;

    /** Number of columns of the initial matrix. */
    private int n;

    /** Transformer to bidiagonal. */
    private BiDiagonalTransformer transformer;

    /** Main diagonal of the bidiagonal matrix. */
    private double[] mainBidiagonal;

    /** Secondary diagonal of the bidiagonal matrix. */
    private double[] secondaryBidiagonal;

    /** Main diagonal of the tridiagonal matrix. */
    private double[] mainTridiagonal;

    /** Secondary diagonal of the tridiagonal matrix. */
    private double[] secondaryTridiagonal;

    /** Eigen decomposition of the tridiagonal matrix. */
    private EigenDecomposition eigenDecomposition;

    /** Singular values. */
    private double[] singularValues;

    /** Cached value of U. */
    private RealMatrix cachedU;

    /** Cached value of U<sup>T</sup>. */
    private RealMatrix cachedUt;

    /** Cached value of S. */
    private RealMatrix cachedS;

    /** Cached value of V. */
    private RealMatrix cachedV;

    /** Cached value of V<sup>T</sup>. */
    private RealMatrix cachedVt;

    /**
     * Calculates the compact Singular Value Decomposition of the given matrix.
     * @param matrix The matrix to decompose.
     * @exception InvalidMatrixException (wrapping a {@link
     * org.apache.commons.math.ConvergenceException} if algorithm fails to converge
     */
    public SingularValueDecompositionImpl(final RealMatrix matrix)
        throws InvalidMatrixException {
        this(matrix, Math.min(matrix.getRowDimension(), matrix.getColumnDimension()));
    }

    /**
     * Calculates the Singular Value Decomposition of the given matrix.
     * @param matrix The matrix to decompose.
     * @param max maximal number of singular values to compute
     * @exception InvalidMatrixException (wrapping a {@link
     * org.apache.commons.math.ConvergenceException} if algorithm fails to converge
     */
    public SingularValueDecompositionImpl(final RealMatrix matrix, final int max)
        throws InvalidMatrixException {

        m = matrix.getRowDimension();
        n = matrix.getColumnDimension();

        cachedU  = null;
        cachedS  = null;
        cachedV  = null;
        cachedVt = null;

        // transform the matrix to bidiagonal
        transformer         = new BiDiagonalTransformer(matrix);
        mainBidiagonal      = transformer.getMainDiagonalRef();
        secondaryBidiagonal = transformer.getSecondaryDiagonalRef();

        // compute Bt.B (if upper diagonal) or B.Bt (if lower diagonal)
        mainTridiagonal      = new double[mainBidiagonal.length];
        secondaryTridiagonal = new double[mainBidiagonal.length - 1];
        double a = mainBidiagonal[0];
        mainTridiagonal[0] = a * a;
        for (int i = 1; i < mainBidiagonal.length; ++i) {
            final double b  = secondaryBidiagonal[i - 1];
            secondaryTridiagonal[i - 1] = a * b;
            a = mainBidiagonal[i];
            mainTridiagonal[i] = a * a + b * b;
        }

        // compute singular values
        eigenDecomposition =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal,
                                       MathUtils.SAFE_MIN);
        final double[] eigenValues = eigenDecomposition.getRealEigenvalues();
        int p = Math.min(max, eigenValues.length);
        while ((p > 0) && (eigenValues[p - 1] <= 0)) {
            --p;
        }
        singularValues = new double[p];
        for (int i = 0; i < p; ++i) {
            singularValues[i] = Math.sqrt(eigenValues[i]);
        }

    }

    /** {@inheritDoc} */
    public RealMatrix getU()
        throws InvalidMatrixException {

        if (cachedU == null) {

            final int p = singularValues.length;
            if (m >= n) {
                // the tridiagonal matrix is Bt.B, where B is upper bidiagonal
                final RealMatrix e =
                    eigenDecomposition.getV().getSubMatrix(0, p - 1, 0, p - 1);
                final double[][] eData = e.getData();
                final double[][] wData = new double[m][p];
                double[] ei1 = eData[0];
                for (int i = 0; i < p - 1; ++i) {
                    // compute W = B.E.S^(-1) where E is the eigenvectors matrix
                    final double mi = mainBidiagonal[i];
                    final double[] ei0 = ei1;
                    final double[] wi  = wData[i];
                        ei1 = eData[i + 1];
                        final double si = secondaryBidiagonal[i];
                        for (int j = 0; j < p; ++j) {
                            wi[j] = (mi * ei0[j] + si * ei1[j]) / singularValues[j];
                        }
                }
                        for (int j = 0; j < p; ++j) {
                            wData[p - 1][j] = ei1[j] * mainBidiagonal[p - 1] / singularValues[j];
                        }

                for (int i = p; i < m; ++i) {
                    wData[i] = new double[p];
                }
                cachedU =
                    transformer.getU().multiply(MatrixUtils.createRealMatrix(wData));
            } else {
                // the tridiagonal matrix is B.Bt, where B is lower bidiagonal
                final RealMatrix e =
                    eigenDecomposition.getV().getSubMatrix(0, m - 1, 0, p - 1);
                cachedU = transformer.getU().multiply(e);
            }

        }

        // return the cached matrix
        return cachedU;

    }

    /** {@inheritDoc} */
    public RealMatrix getUT()
        throws InvalidMatrixException {

        if (cachedUt == null) {
            cachedUt = getU().transpose();
        }

        // return the cached matrix
        return cachedUt;

    }

    /** {@inheritDoc} */
    public RealMatrix getS()
        throws InvalidMatrixException {

        if (cachedS == null) {

            // cache the matrix for subsequent calls
            cachedS = MatrixUtils.createRealDiagonalMatrix(singularValues);

        }
        return cachedS;
    }

    /** {@inheritDoc} */
    public double[] getSingularValues()
        throws InvalidMatrixException {
        return singularValues.clone();
    }

    /** {@inheritDoc} */
    public RealMatrix getV()
        throws InvalidMatrixException {

        if (cachedV == null) {

            final int p = singularValues.length;
            if (m >= n) {
                // the tridiagonal matrix is Bt.B, where B is upper bidiagonal
                final RealMatrix e =
                    eigenDecomposition.getV().getSubMatrix(0, n - 1, 0, p - 1);
                cachedV = transformer.getV().multiply(e);
            } else {
                // the tridiagonal matrix is B.Bt, where B is lower bidiagonal
                // compute W = Bt.E.S^(-1) where E is the eigenvectors matrix
                final RealMatrix e =
                    eigenDecomposition.getV().getSubMatrix(0, p - 1, 0, p - 1);
                final double[][] eData = e.getData();
                final double[][] wData = new double[n][p];
                double[] ei1 = eData[0];
                for (int i = 0; i < p - 1; ++i) {
                    final double mi = mainBidiagonal[i];
                    final double[] ei0 = ei1;
                    final double[] wi  = wData[i];
                        ei1 = eData[i + 1];
                        final double si = secondaryBidiagonal[i];
                        for (int j = 0; j < p; ++j) {
                            wi[j] = (mi * ei0[j] + si * ei1[j]) / singularValues[j];
                        }
                }
                        for (int j = 0; j < p; ++j) {
                            wData[p - 1][j] = ei1[j] * mainBidiagonal[p - 1] / singularValues[j];
                        }
                for (int i = p; i < n; ++i) {
                    wData[i] = new double[p];
                }
                cachedV =
                    transformer.getV().multiply(MatrixUtils.createRealMatrix(wData));
            }

        }

        // return the cached matrix
        return cachedV;

    }

    /** {@inheritDoc} */
    public RealMatrix getVT()
        throws InvalidMatrixException {

        if (cachedVt == null) {
            cachedVt = getV().transpose();
        }

        // return the cached matrix
        return cachedVt;

    }

    /** {@inheritDoc} */
    public RealMatrix getCovariance(final double minSingularValue) {

        // get the number of singular values to consider
        final int p = singularValues.length;
        int dimension = 0;
        while ((dimension < p) && (singularValues[dimension] >= minSingularValue)) {
            ++dimension;
        }

        if (dimension == 0) {
            throw MathRuntimeException.createIllegalArgumentException(
                  "cutoff singular value is {0}, should be at most {1}",
                  minSingularValue, singularValues[0]);
        }

        final double[][] data = new double[dimension][p];
        getVT().walkInOptimizedOrder(new DefaultRealMatrixPreservingVisitor() {
            /** {@inheritDoc} */
            @Override
            public void visit(final int row, final int column, final double value) {
                data[row][column] = value / singularValues[row];
            }
        }, 0, dimension - 1, 0, p - 1);

        RealMatrix jv = new Array2DRowRealMatrix(data, false);
        return jv.transpose().multiply(jv);

    }

    /** {@inheritDoc} */
    public double getNorm()
        throws InvalidMatrixException {
        return singularValues[0];
    }

    /** {@inheritDoc} */
    public double getConditionNumber()
        throws InvalidMatrixException {
        return singularValues[0] / singularValues[singularValues.length - 1];
    }

    /** {@inheritDoc} */
    public int getRank()
        throws IllegalStateException {

        final double threshold = Math.max(m, n) * Math.ulp(singularValues[0]);

        for (int i = singularValues.length - 1; i >= 0; --i) {
           if (singularValues[i] > threshold) {
              return i + 1;
           }
        }
        return 0;

    }

    /** {@inheritDoc} */
    public DecompositionSolver getSolver() {
        return new Solver(singularValues, getUT(), getV(),
                          getRank() == Math.max(m, n));
    }

    /** Specialized solver. */
    private static class Solver implements DecompositionSolver {

        /** Pseudo-inverse of the initial matrix. */
        private final RealMatrix pseudoInverse;

        /** Singularity indicator. */
        private boolean nonSingular;

        /**
         * Build a solver from decomposed matrix.
         * @param singularValues singularValues
         * @param uT U<sup>T</sup> matrix of the decomposition
         * @param v V matrix of the decomposition
         * @param nonSingular singularity indicator
         */
        private Solver(final double[] singularValues, final RealMatrix uT, final RealMatrix v,
                       final boolean nonSingular) {
            double[][] suT      = uT.getData();
            for (int i = 0; i < singularValues.length; ++i) {
                final double a      = 1.0 / singularValues[i];
                final double[] suTi = suT[i];
                for (int j = 0; j < suTi.length; ++j) {
                    suTi[j] *= a;
                }
            }
            pseudoInverse    = v.multiply(new Array2DRowRealMatrix(suT, false));
            this.nonSingular = nonSingular;
        }

        /** Solve the linear equation A &times; X = B in least square sense.
         * <p>The m&times;n matrix A may not be square, the solution X is
         * such that ||A &times; X - B|| is minimal.</p>
         * @param b right-hand side of the equation A &times; X = B
         * @return a vector X that minimizes the two norm of A &times; X - B
         * @exception IllegalArgumentException if matrices dimensions don't match
         */
        public double[] solve(final double[] b)
            throws IllegalArgumentException {
            return pseudoInverse.operate(b);
        }

        /** Solve the linear equation A &times; X = B in least square sense.
         * <p>The m&times;n matrix A may not be square, the solution X is
         * such that ||A &times; X - B|| is minimal.</p>
         * @param b right-hand side of the equation A &times; X = B
         * @return a vector X that minimizes the two norm of A &times; X - B
         * @exception IllegalArgumentException if matrices dimensions don't match
         */
        public RealVector solve(final RealVector b)
            throws IllegalArgumentException {
            return pseudoInverse.operate(b);
        }

        /** Solve the linear equation A &times; X = B in least square sense.
         * <p>The m&times;n matrix A may not be square, the solution X is
         * such that ||A &times; X - B|| is minimal.</p>
         * @param b right-hand side of the equation A &times; X = B
         * @return a matrix X that minimizes the two norm of A &times; X - B
         * @exception IllegalArgumentException if matrices dimensions don't match
         */
        public RealMatrix solve(final RealMatrix b)
            throws IllegalArgumentException {
            return pseudoInverse.multiply(b);
        }

        /**
         * Check if the decomposed matrix is non-singular.
         * @return true if the decomposed matrix is non-singular
         */
        public boolean isNonSingular() {
            return nonSingular;
        }

        /** Get the pseudo-inverse of the decomposed matrix.
         * @return inverse matrix
         */
        public RealMatrix getInverse() {
            return pseudoInverse;
        }

    }

}
