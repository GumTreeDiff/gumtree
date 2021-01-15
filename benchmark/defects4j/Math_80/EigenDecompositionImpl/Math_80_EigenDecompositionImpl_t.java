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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math.MathRuntimeException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.util.MathUtils;

/**
 * Calculates the eigen decomposition of a <strong>symmetric</strong> matrix.
 * <p>The eigen decomposition of matrix A is a set of two matrices:
 * V and D such that A = V D V<sup>T</sup>. A, V and D are all m &times; m
 * matrices.</p>
 * <p>As of 2.0, this class supports only <strong>symmetric</strong> matrices,
 * and hence computes only real realEigenvalues. This implies the D matrix returned by
 * {@link #getD()} is always diagonal and the imaginary values returned {@link
 * #getImagEigenvalue(int)} and {@link #getImagEigenvalues()} are always null.</p>
 * <p>When called with a {@link RealMatrix} argument, this implementation only uses
 * the upper part of the matrix, the part below the diagonal is not accessed at all.</p>
 * <p>Eigenvalues are computed as soon as the matrix is decomposed, but eigenvectors
 * are computed only when required, i.e. only when one of the {@link #getEigenvector(int)},
 * {@link #getV()}, {@link #getVT()}, {@link #getSolver()} methods is called.</p>
 * <p>This implementation is based on Inderjit Singh Dhillon thesis
 * <a href="http://www.cs.utexas.edu/users/inderjit/public_papers/thesis.pdf">A
 * New O(n<sup>2</sup>) Algorithm for the Symmetric Tridiagonal Eigenvalue/Eigenvector
 * Problem</a>, on Beresford N. Parlett and Osni A. Marques paper <a
 * href="http://www.netlib.org/lapack/lawnspdf/lawn155.pdf">An Implementation of the
 * dqds Algorithm (Positive Case)</a> and on the corresponding LAPACK routines (DLARRE,
 * DLASQ2, DLAZQ3, DLAZQ4, DLASQ5 and DLASQ6).</p>
 * <p>The authors of the original fortran version are:
 *   <ul>
 *     <li>Beresford Parlett, University of California, Berkeley, USA</li>
 *     <li>Jim Demmel, University of California, Berkeley, USA</li>
 *     <li>Inderjit Dhillon, University of Texas, Austin, USA</li>
 *     <li>Osni Marques, LBNL/NERSC, USA</li>
 *     <li>Christof Voemel, University of California, Berkeley, USA</li>
 *   </ul>
 * </p>
 * @version $Revision$ $Date$
 * @since 2.0
 */
public class EigenDecompositionImpl implements EigenDecomposition {

    /** Tolerance. */
    private static final double TOLERANCE = 100 * MathUtils.EPSILON;

    /** Squared tolerance. */
    private static final double TOLERANCE_2 = TOLERANCE * TOLERANCE;

    /** Split tolerance. */
    private double splitTolerance;

    /** Main diagonal of the tridiagonal matrix. */
    private double[] main;

    /** Secondary diagonal of the tridiagonal matrix. */
    private double[] secondary;

    /** Squared secondary diagonal of the tridiagonal matrix. */
    private double[] squaredSecondary;

    /** Transformer to tridiagonal (may be null if matrix is already tridiagonal). */
    private TriDiagonalTransformer transformer;

    /** Lower bound of spectra. */
    private double lowerSpectra;

    /** Upper bound of spectra. */
    private double upperSpectra;

    /** Minimum pivot in the Sturm sequence. */
    private double minPivot;

    /** Current shift. */
    private double sigma;

    /** Low part of the current shift. */
    private double sigmaLow;

    /** Shift increment to apply. */
    private double tau;

    /** Work array for all decomposition algorithms. */
    private double[] work;

    /** Shift within qd array for ping-pong implementation. */
    private int pingPong;

    /** Max value of diagonal elements in current segment. */
    private double qMax;

    /** Min value of off-diagonal elements in current segment. */
    private double eMin;

    /** Type of the last dqds shift. */
    private int    tType;

    /** Minimal value on current state of the diagonal. */
    private double dMin;

    /** Minimal value on current state of the diagonal, excluding last element. */
    private double dMin1;

    /** Minimal value on current state of the diagonal, excluding last two elements. */
    private double dMin2;

    /** Last value on current state of the diagonal. */
    private double dN;

    /** Last but one value on current state of the diagonal. */
    private double dN1;

    /** Last but two on current state of the diagonal. */
    private double dN2;

    /** Shift ratio with respect to dMin used when tType == 6. */
    private double g;

    /** Real part of the realEigenvalues. */
    private double[] realEigenvalues;

    /** Imaginary part of the realEigenvalues. */
    private double[] imagEigenvalues;

    /** Eigenvectors. */
    private ArrayRealVector[] eigenvectors;

    /** Cached value of V. */
    private RealMatrix cachedV;

    /** Cached value of D. */
    private RealMatrix cachedD;

    /** Cached value of Vt. */
    private RealMatrix cachedVt;

    /**
     * Calculates the eigen decomposition of the given symmetric matrix.
     * @param matrix The <strong>symmetric</strong> matrix to decompose.
     * @param splitTolerance tolerance on the off-diagonal elements relative to the
     * geometric mean to split the tridiagonal matrix (a suggested value is
     * {@link MathUtils#SAFE_MIN})
     * @exception InvalidMatrixException (wrapping a {@link
     * org.apache.commons.math.ConvergenceException} if algorithm fails to converge
     */
    public EigenDecompositionImpl(final RealMatrix matrix,
                                  final double splitTolerance)
        throws InvalidMatrixException {
        if (isSymmetric(matrix)) {
            this.splitTolerance = splitTolerance;
            transformToTridiagonal(matrix);
            decompose();
        } else {
            // as of 2.0, non-symmetric matrices (i.e. complex eigenvalues) are NOT supported
            // see issue https://issues.apache.org/jira/browse/MATH-235
            throw new InvalidMatrixException("eigen decomposition of assymetric matrices not supported yet");
        }
    }

    /**
     * Calculates the eigen decomposition of the given tridiagonal symmetric matrix.
     * @param main the main diagonal of the matrix (will be copied)
     * @param secondary the secondary diagonal of the matrix (will be copied)
     * @param splitTolerance tolerance on the off-diagonal elements relative to the
     * geometric mean to split the tridiagonal matrix (a suggested value is
     * {@link MathUtils#SAFE_MIN})
     * @exception InvalidMatrixException (wrapping a {@link
     * org.apache.commons.math.ConvergenceException} if algorithm fails to converge
     */
    public EigenDecompositionImpl(final double[] main, double[] secondary,
            final double splitTolerance)
        throws InvalidMatrixException {

        this.main      = main.clone();
        this.secondary = secondary.clone();
        transformer    = null;

        // pre-compute some elements
        squaredSecondary = new double[secondary.length];
        for (int i = 0; i < squaredSecondary.length; ++i) {
            final double s = secondary[i];
            squaredSecondary[i] = s * s;
        }

        this.splitTolerance = splitTolerance;
        decompose();

    }

    /**
     * Check if a matrix is symmetric.
     * @param matrix matrix to check
     * @return true if matrix is symmetric
     */
    private boolean isSymmetric(final RealMatrix matrix) {
        final int rows    = matrix.getRowDimension();
        final int columns = matrix.getColumnDimension();
        final double eps  = 10 * rows * columns * MathUtils.EPSILON;
        for (int i = 0; i < rows; ++i) {
            for (int j = i + 1; j < columns; ++j) {
                final double mij = matrix.getEntry(i, j);
                final double mji = matrix.getEntry(j, i);
                if (Math.abs(mij - mji) > (Math.max(Math.abs(mij), Math.abs(mji)) * eps)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Decompose a tridiagonal symmetric matrix.
     * @exception InvalidMatrixException (wrapping a {@link
     * org.apache.commons.math.ConvergenceException} if algorithm fails to converge
     */
    private void decompose() {

        cachedV  = null;
        cachedD  = null;
        cachedVt = null;
        work     = new double[6 * main.length];

        // compute the Gershgorin circles
        computeGershgorinCircles();

        // find all the realEigenvalues
        findEigenvalues();

        // we will search for eigenvectors only if required
        eigenvectors = null;

    }

    /** {@inheritDoc} */
    public RealMatrix getV()
        throws InvalidMatrixException {

        if (cachedV == null) {

            if (eigenvectors == null) {
                findEigenVectors();
            }

            final int m = eigenvectors.length;
            cachedV = MatrixUtils.createRealMatrix(m, m);
            for (int k = 0; k < m; ++k) {
                cachedV.setColumnVector(k, eigenvectors[k]);
            }

        }

        // return the cached matrix
        return cachedV;

    }

    /** {@inheritDoc} */
    public RealMatrix getD()
        throws InvalidMatrixException {
        if (cachedD == null) {
            // cache the matrix for subsequent calls
            cachedD = MatrixUtils.createRealDiagonalMatrix(realEigenvalues);
        }
        return cachedD;
    }

    /** {@inheritDoc} */
    public RealMatrix getVT()
        throws InvalidMatrixException {

        if (cachedVt == null) {

            if (eigenvectors == null) {
                findEigenVectors();
            }

            final int m = eigenvectors.length;
            cachedVt = MatrixUtils.createRealMatrix(m, m);
            for (int k = 0; k < m; ++k) {
                cachedVt.setRowVector(k, eigenvectors[k]);
            }

        }

        // return the cached matrix
        return cachedVt;

    }

    /** {@inheritDoc} */
    public double[] getRealEigenvalues()
        throws InvalidMatrixException {
        return realEigenvalues.clone();
    }

    /** {@inheritDoc} */
    public double getRealEigenvalue(final int i)
        throws InvalidMatrixException, ArrayIndexOutOfBoundsException {
        return realEigenvalues[i];
    }

    /** {@inheritDoc} */
    public double[] getImagEigenvalues()
        throws InvalidMatrixException {
        return imagEigenvalues.clone();
    }

    /** {@inheritDoc} */
    public double getImagEigenvalue(final int i)
        throws InvalidMatrixException, ArrayIndexOutOfBoundsException {
        return imagEigenvalues[i];
    }

    /** {@inheritDoc} */
    public RealVector getEigenvector(final int i)
        throws InvalidMatrixException, ArrayIndexOutOfBoundsException {
        if (eigenvectors == null) {
            findEigenVectors();
        }
        return eigenvectors[i].copy();
    }

    /**
     * Return the determinant of the matrix
     * @return determinant of the matrix
     */
    public double getDeterminant() {
        double determinant = 1;
        for (double lambda : realEigenvalues) {
            determinant *= lambda;
        }
        return determinant;
    }

    /** {@inheritDoc} */
    public DecompositionSolver getSolver() {
        if (eigenvectors == null) {
            findEigenVectors();
        }
        return new Solver(realEigenvalues, imagEigenvalues, eigenvectors);
    }

    /** Specialized solver. */
    private static class Solver implements DecompositionSolver {

        /** Real part of the realEigenvalues. */
        private double[] realEigenvalues;

        /** Imaginary part of the realEigenvalues. */
        private double[] imagEigenvalues;

        /** Eigenvectors. */
        private final ArrayRealVector[] eigenvectors;

        /**
         * Build a solver from decomposed matrix.
         * @param realEigenvalues real parts of the eigenvalues
         * @param imagEigenvalues imaginary parts of the eigenvalues
         * @param eigenvectors eigenvectors
         */
        private Solver(final double[] realEigenvalues, final double[] imagEigenvalues,
                       final ArrayRealVector[] eigenvectors) {
            this.realEigenvalues = realEigenvalues;
            this.imagEigenvalues = imagEigenvalues;
            this.eigenvectors    = eigenvectors;
        }

        /** Solve the linear equation A &times; X = B for symmetric matrices A.
         * <p>This method only find exact linear solutions, i.e. solutions for
         * which ||A &times; X - B|| is exactly 0.</p>
         * @param b right-hand side of the equation A &times; X = B
         * @return a vector X that minimizes the two norm of A &times; X - B
         * @exception IllegalArgumentException if matrices dimensions don't match
         * @exception InvalidMatrixException if decomposed matrix is singular
         */
        public double[] solve(final double[] b)
            throws IllegalArgumentException, InvalidMatrixException {

            if (!isNonSingular()) {
                throw new SingularMatrixException();
            }

            final int m = realEigenvalues.length;
            if (b.length != m) {
                throw MathRuntimeException.createIllegalArgumentException(
                        "vector length mismatch: got {0} but expected {1}",
                        b.length, m);
            }

            final double[] bp = new double[m];
            for (int i = 0; i < m; ++i) {
                final ArrayRealVector v = eigenvectors[i];
                final double[] vData = v.getDataRef();
                final double s = v.dotProduct(b) / realEigenvalues[i];
                for (int j = 0; j < m; ++j) {
                    bp[j] += s * vData[j];
                }
            }

            return bp;

        }

        /** Solve the linear equation A &times; X = B for symmetric matrices A.
         * <p>This method only find exact linear solutions, i.e. solutions for
         * which ||A &times; X - B|| is exactly 0.</p>
         * @param b right-hand side of the equation A &times; X = B
         * @return a vector X that minimizes the two norm of A &times; X - B
         * @exception IllegalArgumentException if matrices dimensions don't match
         * @exception InvalidMatrixException if decomposed matrix is singular
         */
        public RealVector solve(final RealVector b)
            throws IllegalArgumentException, InvalidMatrixException {

            if (!isNonSingular()) {
                throw new SingularMatrixException();
            }

            final int m = realEigenvalues.length;
            if (b.getDimension() != m) {
                throw MathRuntimeException.createIllegalArgumentException(
                        "vector length mismatch: got {0} but expected {1}",
                        b.getDimension(), m);
            }

            final double[] bp = new double[m];
            for (int i = 0; i < m; ++i) {
                final ArrayRealVector v = eigenvectors[i];
                final double[] vData = v.getDataRef();
                final double s = v.dotProduct(b) / realEigenvalues[i];
                for (int j = 0; j < m; ++j) {
                    bp[j] += s * vData[j];
                }
            }

            return new ArrayRealVector(bp, false);

        }

        /** Solve the linear equation A &times; X = B for symmetric matrices A.
         * <p>This method only find exact linear solutions, i.e. solutions for
         * which ||A &times; X - B|| is exactly 0.</p>
         * @param b right-hand side of the equation A &times; X = B
         * @return a matrix X that minimizes the two norm of A &times; X - B
         * @exception IllegalArgumentException if matrices dimensions don't match
         * @exception InvalidMatrixException if decomposed matrix is singular
         */
        public RealMatrix solve(final RealMatrix b)
            throws IllegalArgumentException, InvalidMatrixException {

            if (!isNonSingular()) {
                throw new SingularMatrixException();
            }

            final int m = realEigenvalues.length;
            if (b.getRowDimension() != m) {
                throw MathRuntimeException.createIllegalArgumentException(
                        "dimensions mismatch: got {0}x{1} but expected {2}x{3}",
                        b.getRowDimension(), b.getColumnDimension(), m, "n");
            }

            final int nColB = b.getColumnDimension();
            final double[][] bp = new double[m][nColB];
            for (int k = 0; k < nColB; ++k) {
                for (int i = 0; i < m; ++i) {
                    final ArrayRealVector v = eigenvectors[i];
                    final double[] vData = v.getDataRef();
                    double s = 0;
                    for (int j = 0; j < m; ++j) {
                        s += v.getEntry(j) * b.getEntry(j, k);
                    }
                    s /= realEigenvalues[i];
                    for (int j = 0; j < m; ++j) {
                        bp[j][k] += s * vData[j];
                    }
                }
            }

            return MatrixUtils.createRealMatrix(bp);

        }

        /**
         * Check if the decomposed matrix is non-singular.
         * @return true if the decomposed matrix is non-singular
         */
        public boolean isNonSingular() {
            for (int i = 0; i < realEigenvalues.length; ++i) {
                if ((realEigenvalues[i] == 0) && (imagEigenvalues[i] == 0)) {
                    return false;
                }
            }
            return true;
        }

        /** Get the inverse of the decomposed matrix.
         * @return inverse matrix
         * @throws InvalidMatrixException if decomposed matrix is singular
         */
        public RealMatrix getInverse()
            throws InvalidMatrixException {

            if (!isNonSingular()) {
                throw new SingularMatrixException();
            }

            final int m = realEigenvalues.length;
            final double[][] invData = new double[m][m];

            for (int i = 0; i < m; ++i) {
                final double[] invI = invData[i];
                for (int j = 0; j < m; ++j) {
                    double invIJ = 0;
                    for (int k = 0; k < m; ++k) {
                        final double[] vK = eigenvectors[k].getDataRef();
                        invIJ += vK[i] * vK[j] / realEigenvalues[k];
                    }
                    invI[j] = invIJ;
                }
            }
            return MatrixUtils.createRealMatrix(invData);

        }

    }

    /**
     * Transform matrix to tridiagonal.
     * @param matrix matrix to transform
     */
    private void transformToTridiagonal(final RealMatrix matrix) {

        // transform the matrix to tridiagonal
        transformer = new TriDiagonalTransformer(matrix);
        main      = transformer.getMainDiagonalRef();
        secondary = transformer.getSecondaryDiagonalRef();

        // pre-compute some elements
        squaredSecondary = new double[secondary.length];
        for (int i = 0; i < squaredSecondary.length; ++i) {
            final double s = secondary[i];
            squaredSecondary[i] = s * s;
        }

    }

    /**
     * Compute the Gershgorin circles for all rows.
     */
    private void computeGershgorinCircles() {

        final int m     = main.length;
        final int lowerStart = 4 * m;
        final int upperStart = 5 * m;
        lowerSpectra = Double.POSITIVE_INFINITY;
        upperSpectra = Double.NEGATIVE_INFINITY;
        double eMax = 0;

        double eCurrent = 0;
        for (int i = 0; i < m - 1; ++i) {

            final double dCurrent = main[i];
            final double ePrevious = eCurrent;
            eCurrent = Math.abs(secondary[i]);
            eMax = Math.max(eMax, eCurrent);
            final double radius = ePrevious + eCurrent;

            final double lower = dCurrent - radius;
            work[lowerStart + i] = lower;
            lowerSpectra = Math.min(lowerSpectra, lower);

            final double upper = dCurrent + radius;
            work[upperStart + i] = upper;
            upperSpectra = Math.max(upperSpectra, upper);

        }

        final double dCurrent = main[m - 1];
        final double lower = dCurrent - eCurrent;
        work[lowerStart + m - 1] = lower;
        lowerSpectra = Math.min(lowerSpectra, lower);
        final double upper = dCurrent + eCurrent;
        work[upperStart + m - 1] = upper;
        upperSpectra = Math.max(upperSpectra, upper);
        minPivot = MathUtils.SAFE_MIN * Math.max(1.0, eMax * eMax);

    }

    /**
     * Find the realEigenvalues.
     * @exception InvalidMatrixException if a block cannot be diagonalized
     */
    private void findEigenvalues()
        throws InvalidMatrixException {

        // compute splitting points
        List<Integer> splitIndices = computeSplits();

        // find realEigenvalues in each block
        realEigenvalues = new double[main.length];
        imagEigenvalues = new double[main.length];
        int begin = 0;
        for (final int end : splitIndices) {
            final int n = end - begin;
            switch (n) {

            case 1:
                // apply dedicated method for dimension 1
                process1RowBlock(begin);
                break;

            case 2:
                // apply dedicated method for dimension 2
                process2RowsBlock(begin);
                break;

            case 3:
                // apply dedicated method for dimension 3
                process3RowsBlock(begin);
                break;

            default:

                // choose an initial shift for LDL<sup>T</sup> decomposition
                final double[] range       = eigenvaluesRange(begin, n);
                final double oneFourth     = 0.25 * (3 * range[0] + range[1]);
                final int oneFourthCount   = countEigenValues(oneFourth, begin, n);
                final double threeFourth   = 0.25 * (range[0] + 3 * range[1]);
                final int threeFourthCount = countEigenValues(threeFourth, begin, n);
                final boolean chooseLeft   = (oneFourthCount - 1) >= (n - threeFourthCount);
                final double lambda        = chooseLeft ? range[0] : range[1];

                tau = (range[1] - range[0]) * MathUtils.EPSILON * n + 2 * minPivot;

                // decompose T-&lambda;I as LDL<sup>T</sup>
                ldlTDecomposition(lambda, begin, n);

                // apply general dqd/dqds method
                processGeneralBlock(n);

                // extract realEigenvalues
                if (chooseLeft) {
                    for (int i = 0; i < n; ++i) {
                        realEigenvalues[begin + i] = lambda + work[4 * i];
                    }
                } else {
                    for (int i = 0; i < n; ++i) {
                        realEigenvalues[begin + i] = lambda - work[4 * i];
                    }
                }

            }
            begin = end;
        }

        // sort the realEigenvalues in decreasing order
        Arrays.sort(realEigenvalues);
        int j = realEigenvalues.length - 1;
        for (int i = 0; i < j; ++i) {
            final double tmp = realEigenvalues[i];
            realEigenvalues[i] = realEigenvalues[j];
            realEigenvalues[j] = tmp;
            --j;
        }

    }

    /**
     * Compute splitting points.
     * @return list of indices after matrix can be split
     */
    private List<Integer> computeSplits() {

        final List<Integer> list = new ArrayList<Integer>();

        // splitting preserving relative accuracy
        double absDCurrent = Math.abs(main[0]);
        for (int i = 0; i < secondary.length; ++i) {
            final double absDPrevious = absDCurrent;
            absDCurrent = Math.abs(main[i + 1]);
            final double max = splitTolerance * Math.sqrt(absDPrevious * absDCurrent);
            if (Math.abs(secondary[i]) <= max) {
                list.add(i + 1);
                secondary[i] = 0;
                squaredSecondary[i] = 0;
            }
        }

        list.add(secondary.length + 1);
        return list;

    }

    /**
     * Find eigenvalue in a block with 1 row.
     * <p>In low dimensions, we simply solve the characteristic polynomial.</p>
     * @param index index of the first row of the block
     */
    private void process1RowBlock(final int index) {
        realEigenvalues[index] = main[index];
    }

    /**
     * Find realEigenvalues in a block with 2 rows.
     * <p>In low dimensions, we simply solve the characteristic polynomial.</p>
     * @param index index of the first row of the block
     * @exception InvalidMatrixException if characteristic polynomial cannot be solved
     */
    private void process2RowsBlock(final int index)
        throws InvalidMatrixException {

        // the characteristic polynomial is
        // X^2 - (q0 + q1) X + q0 q1 - e1^2
        final double q0   = main[index];
        final double q1   = main[index + 1];
        final double e12  = squaredSecondary[index];

        final double s     = q0 + q1;
        final double p     = q0 * q1 - e12;
        final double delta = s * s - 4 * p;
        if (delta < 0) {
            throw new InvalidMatrixException("cannot solve degree {0} equation", 2);
        }

        final double largestRoot = 0.5 * (s + Math.sqrt(delta));
        realEigenvalues[index]     = largestRoot;
        realEigenvalues[index + 1] = p / largestRoot;

    }

    /**
     * Find realEigenvalues in a block with 3 rows.
     * <p>In low dimensions, we simply solve the characteristic polynomial.</p>
     * @param index index of the first row of the block
     * @exception InvalidMatrixException if diagonal elements are not positive
     */
    private void process3RowsBlock(final int index)
        throws InvalidMatrixException {

        // the characteristic polynomial is
        // X^3 - (q0 + q1 + q2) X^2 + (q0 q1 + q0 q2 + q1 q2 - e1^2 - e2^2) X + q0 e2^2 + q2 e1^2 - q0 q1 q2
        final double q0       = main[index];
        final double q1       = main[index + 1];
        final double q2       = main[index + 2];
        final double e12      = squaredSecondary[index];
        final double q1q2Me22 = q1 * q2 - squaredSecondary[index + 1];

        // compute coefficients of the cubic equation as: x^3 + b x^2 + c x + d = 0
        final double b        = -(q0 + q1 + q2);
        final double c        = q0 * q1 + q0 * q2 + q1q2Me22 - e12;
        final double d        = q2 * e12 - q0 * q1q2Me22;

        // solve cubic equation
        final double b2       = b * b;
        final double q        = (3 * c - b2) / 9;
        final double r        = ((9 * c - 2 * b2) * b - 27 * d) / 54;
        final double delta    = q * q * q + r * r;
        if (delta >= 0) {
            // in fact, there are solutions to the equation, but in the context
            // of symmetric realEigenvalues problem, there should be three distinct
            // real roots, so we throw an error if this condition is not met
            throw new InvalidMatrixException("cannot solve degree {0} equation", 3);
        }
        final double sqrtMq = Math.sqrt(-q);
        final double theta  = Math.acos(r / (-q * sqrtMq));
        final double alpha  = 2 * sqrtMq;
        final double beta   = b / 3;

        double z0 = alpha * Math.cos(theta / 3) - beta;
        double z1 = alpha * Math.cos((theta + 2 * Math.PI) / 3) - beta;
        double z2 = alpha * Math.cos((theta + 4 * Math.PI) / 3) - beta;
        if (z0 < z1) {
            final double t = z0;
            z0 = z1;
            z1 = t;
        }
        if (z1 < z2) {
            final double t = z1;
            z1 = z2;
            z2 = t;
        }
        if (z0 < z1) {
            final double t = z0;
            z0 = z1;
            z1 = t;
        }
        realEigenvalues[index]     = z0;
        realEigenvalues[index + 1] = z1;
        realEigenvalues[index + 2] = z2;

    }

    /**
     * Find realEigenvalues using dqd/dqds algorithms.
     * <p>This implementation is based on Beresford N. Parlett
     * and Osni A. Marques paper <a
     * href="http://www.netlib.org/lapack/lawnspdf/lawn155.pdf">An
     * Implementation of the dqds Algorithm (Positive Case)</a> and on the
     * corresponding LAPACK routine DLASQ2.</p>
     * @param n number of rows of the block
     * @exception InvalidMatrixException if block cannot be diagonalized
     * after 30 * n iterations
     */
    private void processGeneralBlock(final int n)
        throws InvalidMatrixException {

        // check decomposed matrix data range
        double sumOffDiag = 0;
        for (int i = 0; i < n - 1; ++i) {
            final int fourI = 4 * i;
            final double ei = work[fourI + 2];
            sumOffDiag += ei;
        }

        if (sumOffDiag == 0) {
            // matrix is already diagonal
            return;
        }

        // initial checks for splits (see Parlett & Marques section 3.3)
        flipIfWarranted(n, 2);

        // two iterations with Li's test for initial splits
        initialSplits(n);

        // initialize parameters used by goodStep
        tType = 0;
        dMin1 = 0;
        dMin2 = 0;
        dN    = 0;
        dN1   = 0;
        dN2   = 0;
        tau   = 0;

        // process split segments
        int i0 = 0;
        int n0 = n;
        while (n0 > 0) {

            // retrieve shift that was temporarily stored as a negative off-diagonal element
            sigma    = (n0 == n) ? 0 : -work[4 * n0 - 2];
            sigmaLow = 0;

            // find start of a new split segment to process
            double offDiagMin = (i0 == n0) ? 0 : work[4 * n0 - 6];
            double offDiagMax = 0;
            double diagMax    = work[4 * n0 - 4];
            double diagMin    = diagMax;
            i0 = 0;
            for (int i = 4 * (n0 - 2); i >= 0; i -= 4) {
                if (work[i + 2] <= 0) {
                    i0 = 1 + i / 4;
                    break;
                }
                if (diagMin >= 4 * offDiagMax) {
                    diagMin    = Math.min(diagMin, work[i + 4]);
                    offDiagMax = Math.max(offDiagMax, work[i + 2]);
                }
                diagMax    = Math.max(diagMax, work[i] + work[i + 2]);
                offDiagMin = Math.min(offDiagMin, work[i + 2]);
            }
            work[4 * n0 - 2] = offDiagMin;

            // lower bound of Gershgorin disk
            dMin = -Math.max(0, diagMin - 2 * Math.sqrt(diagMin * offDiagMax));

            pingPong = 0;
            int maxIter = 30 * (n0 - i0);
            for (int k = 0; i0 < n0; ++k) {
                if (k >= maxIter) {
                    throw new InvalidMatrixException(new MaxIterationsExceededException(maxIter));
                }

                // perform one step
                n0 = goodStep(i0, n0);
                pingPong = 1 - pingPong;

                // check for new splits after "ping" steps
                // when the last elements of qd array are very small
                if ((pingPong == 0) && (n0 - i0 > 3) &&
                    (work[4 * n0 - 1] <= TOLERANCE_2 * diagMax) &&
                    (work[4 * n0 - 2] <= TOLERANCE_2 * sigma)) {
                    int split  = i0 - 1;
                    diagMax    = work[4 * i0];
                    offDiagMin = work[4 * i0 + 2];
                    double previousEMin = work[4 * i0 + 3];
                    for (int i = 4 * i0; i < 4 * n0 - 16; i += 4) {
                        if ((work[i + 3] <= TOLERANCE_2 * work[i]) ||
                            (work[i + 2] <= TOLERANCE_2 * sigma)) {
                            // insert a split
                            work[i + 2]  = -sigma;
                            split        = i / 4;
                            diagMax      = 0;
                            offDiagMin   = work[i + 6];
                            previousEMin = work[i + 7];
                        } else {
                            diagMax      = Math.max(diagMax, work[i + 4]);
                            offDiagMin   = Math.min(offDiagMin, work[i + 2]);
                            previousEMin = Math.min(previousEMin, work[i + 3]);
                        }
                    }
                    work[4 * n0 - 2] = offDiagMin;
                    work[4 * n0 - 1] = previousEMin;
                    i0 = split + 1;
                }
            }

        }

    }

    /**
     * Perform two iterations with Li's tests for initial splits.
     * @param n number of rows of the matrix to process
     */
    private void initialSplits(final int n) {

        pingPong = 0;
        for (int k = 0; k < 2; ++k) {

            // apply Li's reverse test
            double d = work[4 * (n - 1) + pingPong];
            for (int i = 4 * (n - 2) + pingPong; i >= 0; i -= 4) {
                if (work[i + 2] <= TOLERANCE_2 * d) {
                    work[i + 2] = -0.0;
                    d = work[i];
                } else {
                    d *= work[i] / (d + work[i + 2]);
                }
            }

            // apply dqd plus Li's forward test.
            d = work[pingPong];
            for (int i = 2 + pingPong; i < 4 * n - 2; i += 4) {
                final int j = i - 2 * pingPong - 1;
                work[j] = d + work[i];
                if (work[i] <= TOLERANCE_2 * d) {
                    work[i]     = -0.0;
                    work[j]     = d;
                    work[j + 2] = 0.0;
                    d = work[i + 2];
                } else if ((MathUtils.SAFE_MIN * work[i + 2] < work[j]) &&
                           (MathUtils.SAFE_MIN * work[j] < work[i + 2])) {
                    final double tmp = work[i + 2] / work[j];
                    work[j + 2] = work[i] * tmp;
                    d *= tmp;
                } else {
                    work[j + 2] = work[i + 2] * (work[i] / work[j]);
                    d *= work[i + 2] / work[j];
               }
            }
            work[4 * n - 3 - pingPong] = d;

            // from ping to pong
            pingPong = 1 - pingPong;

        }

    }

    /**
     * Perform one "good" dqd/dqds step.
     * <p>This implementation is based on Beresford N. Parlett
     * and Osni A. Marques paper <a
     * href="http://www.netlib.org/lapack/lawnspdf/lawn155.pdf">An
     * Implementation of the dqds Algorithm (Positive Case)</a> and on the
     * corresponding LAPACK routine DLAZQ3.</p>
     * @param start start index
     * @param end end index
     * @return new end (maybe deflated)
     */
    private int goodStep(final int start, final int end) {

        g = 0.0;

        // step 1: accepting realEigenvalues
        int deflatedEnd = end;
        for (boolean deflating = true; deflating;) {

            if (start >= deflatedEnd) {
                // the array has been completely deflated
                return deflatedEnd;
            }

            final int k = 4 * deflatedEnd + pingPong - 1;

            if ((start == deflatedEnd - 1) ||
                ((start != deflatedEnd - 2) &&
                 ((work[k - 5] <= TOLERANCE_2 * (sigma + work[k - 3])) ||
                  (work[k - 2 * pingPong - 4] <= TOLERANCE_2 * work[k - 7])))) {

                // one eigenvalue found, deflate array
                work[4 * deflatedEnd - 4] = sigma + work[4 * deflatedEnd - 4 + pingPong];
                deflatedEnd -= 1;

            } else if ((start == deflatedEnd - 2) ||
                (work[k - 9] <= TOLERANCE_2 * sigma) ||
                (work[k - 2 * pingPong - 8] <= TOLERANCE_2 * work[k - 11])) {

                // two realEigenvalues found, deflate array
                if (work[k - 3] > work[k - 7]) {
                    final double tmp = work[k - 3];
                    work[k - 3] = work[k - 7];
                    work[k - 7] = tmp;
                }

                if (work[k - 5] > TOLERANCE_2 * work[k - 3]) {
                    double t = 0.5 * ((work[k - 7] - work[k - 3]) + work[k - 5]);
                    double s = work[k - 3] * (work[k - 5] / t);
                    if (s <= t) {
                        s = work[k - 3] * work[k - 5] / (t * (1 + Math.sqrt(1 + s / t)));
                    } else {
                        s = work[k - 3] * work[k - 5] / (t + Math.sqrt(t * (t + s)));
                    }
                    t = work[k - 7] + (s + work[k - 5]);
                    work[k - 3] *= work[k - 7] / t;
                    work[k - 7]  = t;
                }
                work[4 * deflatedEnd - 8] = sigma + work[k - 7];
                work[4 * deflatedEnd - 4] = sigma + work[k - 3];
                deflatedEnd -= 2;
            } else {

                // no more realEigenvalues found, we need to iterate
                deflating = false;

            }

        }

        final int l = 4 * deflatedEnd + pingPong - 1;

        // step 2: flip array if needed
        if ((dMin <= 0) || (deflatedEnd < end)) {
            if (flipIfWarranted(deflatedEnd, 1)) {
                dMin2 = Math.min(dMin2, work[l - 1]);
                work[l - 1] =
                    Math.min(work[l - 1],
                             Math.min(work[3 + pingPong], work[7 + pingPong]));
                work[l - 2 * pingPong] =
                    Math.min(work[l - 2 * pingPong],
                             Math.min(work[6 + pingPong], work[6 + pingPong]));
                qMax  = Math.max(qMax, Math.max(work[3 + pingPong], work[7 + pingPong]));
                dMin  = -0.0;
            }
        }

        if ((dMin < 0) ||
            (MathUtils.SAFE_MIN * qMax < Math.min(work[l - 1],
                                                  Math.min(work[l - 9],
                                                           dMin2 + work[l - 2 * pingPong])))) {
            // step 3: choose a shift
            computeShiftIncrement(start, deflatedEnd, end - deflatedEnd);

            // step 4a: dqds
            for (boolean loop = true; loop;) {

                // perform one dqds step with the chosen shift
                dqds(start, deflatedEnd);

                // check result of the dqds step
                if ((dMin >= 0) && (dMin1 > 0)) {
                    // the shift was good
                    updateSigma(tau);
                    return deflatedEnd;
                } else if ((dMin < 0.0) &&
                           (dMin1 > 0.0) &&
                           (work[4 * deflatedEnd - 5 - pingPong] < TOLERANCE * (sigma + dN1)) &&
                           (Math.abs(dN) < TOLERANCE * sigma)) {
                   // convergence hidden by negative DN.
                    work[4 * deflatedEnd - 3 - pingPong] = 0.0;
                    dMin = 0.0;
                    updateSigma(tau);
                    return deflatedEnd;
                } else if (dMin < 0.0) {
                    // tau too big. Select new tau and try again.
                    if (tType < -22) {
                        // failed twice. Play it safe.
                        tau = 0.0;
                    } else if (dMin1 > 0.0) {
                        // late failure. Gives excellent shift.
                        tau = (tau + dMin) * (1.0 - 2.0 * MathUtils.EPSILON);
                        tType -= 11;
                    } else {
                        // early failure. Divide by 4.
                        tau *= 0.25;
                        tType -= 12;
                    }
                } else if (Double.isNaN(dMin)) {
                    tau = 0.0;
                } else {
                    // possible underflow. Play it safe.
                    loop = false;
                }
            }

        }

        // perform a dqd step (i.e. no shift)
        dqd(start, deflatedEnd);

        return deflatedEnd;

    }

    /**
     * Flip qd array if warranted.
     * @param n number of rows in the block
     * @param step within the array (1 for flipping all elements, 2 for flipping
     * only every other element)
     * @return true if qd array was flipped
     */
    private boolean flipIfWarranted(final int n, final int step) {
        if (1.5 * work[pingPong] < work[4 * (n - 1) + pingPong]) {
            // flip array
            int j = 4 * (n - 1);
            for (int i = 0; i < j; i += 4) {
                for (int k = 0; k < 4; k += step) {
                    final double tmp = work[i + k];
                    work[i + k] = work[j - k];
                    work[j - k] = tmp;
                }
                j -= 4;
            }
            return true;
        }
        return false;
    }

    /**
     * Compute an interval containing all realEigenvalues of a block.
     * @param index index of the first row of the block
     * @param n number of rows of the block
     * @return an interval containing the realEigenvalues
     */
    private double[] eigenvaluesRange(final int index, final int n) {

        // find the bounds of the spectra of the local block
        final int lowerStart = 4 * main.length;
        final int upperStart = 5 * main.length;
        double lower = Double.POSITIVE_INFINITY;
        double upper = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; ++i) {
            lower = Math.min(lower, work[lowerStart + index +i]);
            upper = Math.max(upper, work[upperStart + index +i]);
        }

        // set thresholds
        final double tNorm = Math.max(Math.abs(lower), Math.abs(upper));
        final double relativeTolerance = Math.sqrt(MathUtils.EPSILON);
        final double absoluteTolerance = 4 * minPivot;
        final int maxIter =
            2 + (int) ((Math.log(tNorm + minPivot) - Math.log(minPivot)) / Math.log(2.0));
        final double margin = 2 * (tNorm * MathUtils.EPSILON * n + 2 * minPivot);

        // search lower eigenvalue
        double left  = lower - margin;
        double right = upper + margin;
        for (int i = 0; i < maxIter; ++i) {

            final double range = right - left;
            if ((range < absoluteTolerance) ||
                (range < relativeTolerance * Math.max(Math.abs(left), Math.abs(right)))) {
                // search has converged
                break;
            }

            final double middle = 0.5 * (left + right);
            if (countEigenValues(middle, index, n) >= 1) {
                right = middle;
            } else {
                left = middle;
            }

        }
        lower = Math.max(lower, left - 100 * MathUtils.EPSILON * Math.abs(left));

        // search upper eigenvalue
        left  = lower - margin;
        right = upper + margin;
        for (int i = 0; i < maxIter; ++i) {

            final double range = right - left;
            if ((range < absoluteTolerance) ||
                (range < relativeTolerance * Math.max(Math.abs(left), Math.abs(right)))) {
                // search has converged
                break;
            }

            final double middle = 0.5 * (left + right);
            if (countEigenValues(middle, index, n) >= n) {
                right = middle;
            } else {
                left = middle;
            }

        }
        upper = Math.min(upper, right + 100 * MathUtils.EPSILON * Math.abs(right));

        return new double[] { lower, upper };

    }

    /**
     * Count the number of realEigenvalues below a point.
     * @param t value below which we must count the number of realEigenvalues
     * @param index index of the first row of the block
     * @param n number of rows of the block
     * @return number of realEigenvalues smaller than t
     */
    private int countEigenValues(final double t, final int index, final int n) {
        double ratio = main[index] - t;
        int count = (ratio > 0) ? 0 : 1;
        for (int i = 1; i < n; ++i) {
            ratio = main[index + i] - squaredSecondary[index + i - 1] / ratio - t;
            if (ratio <= 0) {
                ++count;
            }
        }
        return count;
    }

    /**
     * Decompose the shifted tridiagonal matrix T-&lambda;I as LDL<sup>T</sup>.
     * <p>A shifted symmetric tridiagonal matrix T can be decomposed as
     * LDL<sup>T</sup> where L is a lower bidiagonal matrix with unit diagonal
     * and D is a diagonal matrix. This method is an implementation of
     * algorithm 4.4.7 from Dhillon's thesis.</p>
     * @param lambda shift to add to the matrix before decomposing it
     * to ensure it is positive definite
     * @param index index of the first row of the block
     * @param n number of rows of the block
     */
    private void ldlTDecomposition(final double lambda, final int index, final int n) {
        double di = main[index] - lambda;
        work[0] = Math.abs(di);
        for (int i = 1; i < n; ++i) {
            final int    fourI = 4 * i;
            final double eiM1  = secondary[index + i - 1];
            final double ratio = eiM1 / di;
            work[fourI - 2] = ratio * ratio * Math.abs(di);
            di = (main[index + i] - lambda) - eiM1 * ratio;
            work[fourI] = Math.abs(di);
        }
    }

    /**
     * Perform a dqds step, using current shift increment.
     * <p>This implementation is a translation of the LAPACK routine DLASQ5.</p>
     * @param start start index
     * @param end end index
     */
    private void dqds(final int start, final int end) {

        eMin = work[4 * start + pingPong + 4];
        double d = work[4 * start + pingPong] - tau;
        dMin = d;
        dMin1 = -work[4 * start + pingPong];

        if (pingPong == 0) {
            for (int j4 = 4 * start + 3; j4 <= 4 * (end - 3); j4 += 4) {
                work[j4 - 2] = d + work[j4 - 1];
                final double tmp = work[j4 + 1] / work[j4 - 2];
                d = d * tmp - tau;
                dMin = Math.min(dMin, d);
                work[j4] = work[j4 - 1] * tmp;
                eMin = Math.min(work[j4], eMin);
            }
        } else {
            for (int j4 = 4 * start + 3; j4 <= 4 * (end - 3); j4 += 4) {
                work[j4 - 3] = d + work[j4];
                final double tmp = work[j4 + 2] / work[j4 - 3];
                d = d * tmp - tau;
                dMin = Math.min(dMin, d);
                work[j4 - 1] = work[j4] * tmp;
                eMin = Math.min(work[j4 - 1], eMin);
            }
        }

        // unroll last two steps.
        dN2 = d;
        dMin2 = dMin;
        int j4 = 4 * (end - 2) - pingPong - 1;
        int j4p2 = j4 + 2 * pingPong - 1;
        work[j4 - 2] = dN2 + work[j4p2];
        work[j4] = work[j4p2 + 2] * (work[j4p2] / work[j4 - 2]);
        dN1 = work[j4p2 + 2] * (dN2 / work[j4 - 2]) - tau;
        dMin = Math.min(dMin, dN1);

        dMin1 = dMin;
        j4 = j4 + 4;
        j4p2 = j4 + 2 * pingPong - 1;
        work[j4 - 2] = dN1 + work[j4p2];
        work[j4] = work[j4p2 + 2] * (work[j4p2] / work[j4 - 2]);
        dN = work[j4p2 + 2] * (dN1 / work[j4 - 2]) - tau;
        dMin = Math.min(dMin, dN);

        work[j4 + 2] = dN;
        work[4 * end - pingPong - 1] = eMin;

    }


    /**
     * Perform a dqd step.
     * <p>This implementation is a translation of the LAPACK routine DLASQ6.</p>
     * @param start start index
     * @param end end index
     */
    private void dqd(final int start, final int end) {

        eMin = work[4 * start + pingPong + 4];
        double d = work[4 * start + pingPong];
        dMin = d;

        if (pingPong == 0) {
            for (int j4 = 4 * start + 3; j4 < 4 * (end - 3); j4 += 4) {
                work[j4 - 2] = d + work[j4 - 1];
                if (work[j4 - 2] == 0.0) {
                    work[j4] = 0.0;
                    d = work[j4 + 1];
                    dMin = d;
                    eMin = 0.0;
                } else if ((MathUtils.SAFE_MIN * work[j4 + 1] < work[j4 - 2]) &&
                           (MathUtils.SAFE_MIN * work[j4 - 2] < work[j4 + 1])) {
                    final double tmp = work[j4 + 1] / work[j4 - 2];
                    work[j4] = work[j4 - 1] * tmp;
                    d *= tmp;
                } else {
                    work[j4] = work[j4 + 1] * (work[j4 - 1] / work[j4 - 2]);
                    d *= work[j4 + 1] / work[j4 - 2];
                }
                dMin = Math.min(dMin, d);
                eMin = Math.min(eMin, work[j4]);
            }
        } else {
            for (int j4 = 4 * start + 3; j4 < 4 * (end - 3); j4 += 4) {
                work[j4 - 3] = d + work[j4];
                if (work[j4 - 3] == 0.0) {
                    work[j4 - 1] = 0.0;
                    d = work[j4 + 2];
                    dMin = d;
                    eMin = 0.0;
                } else if ((MathUtils.SAFE_MIN * work[j4 + 2] < work[j4 - 3]) &&
                           (MathUtils.SAFE_MIN * work[j4 - 3] < work[j4 + 2])) {
                    final double tmp = work[j4 + 2] / work[j4 - 3];
                    work[j4 - 1] = work[j4] * tmp;
                    d *= tmp;
                } else {
                    work[j4 - 1] = work[j4 + 2] * (work[j4] / work[j4 - 3]);
                    d *= work[j4 + 2] / work[j4 - 3];
                }
                dMin = Math.min(dMin, d);
                eMin = Math.min(eMin, work[j4 - 1]);
            }
        }

        // Unroll last two steps
        dN2   = d;
        dMin2 = dMin;
        int j4 = 4 * (end - 2) - pingPong - 1;
        int j4p2 = j4 + 2 * pingPong - 1;
        work[j4 - 2] = dN2 + work[j4p2];
        if (work[j4 - 2] == 0.0) {
            work[j4] = 0.0;
            dN1  = work[j4p2 + 2];
            dMin = dN1;
            eMin = 0.0;
        } else if ((MathUtils.SAFE_MIN * work[j4p2 + 2] < work[j4 - 2]) &&
                   (MathUtils.SAFE_MIN * work[j4 - 2] < work[j4p2 + 2])) {
            final double tmp = work[j4p2 + 2] / work[j4 - 2];
            work[j4] = work[j4p2] * tmp;
            dN1 = dN2 * tmp;
        } else {
            work[j4] = work[j4p2 + 2] * (work[j4p2] / work[j4 - 2]);
            dN1 = work[j4p2 + 2] * (dN2 / work[j4 - 2]);
        }
        dMin = Math.min(dMin, dN1);

        dMin1 = dMin;
        j4 = j4 + 4;
        j4p2 = j4 + 2 * pingPong - 1;
        work[j4 - 2] = dN1 + work[j4p2];
        if (work[j4 - 2] == 0.0) {
            work[j4] = 0.0;
            dN   = work[j4p2 + 2];
            dMin = dN;
            eMin = 0.0;
        } else if ((MathUtils.SAFE_MIN * work[j4p2 + 2] < work[j4 - 2]) &&
                   (MathUtils.SAFE_MIN * work[j4 - 2] < work[j4p2 + 2])) {
            final double tmp = work[j4p2 + 2] / work[j4 - 2];
            work[j4] = work[j4p2] * tmp;
            dN = dN1 * tmp;
        } else {
            work[j4] = work[j4p2 + 2] * (work[j4p2] / work[j4 - 2]);
            dN = work[j4p2 + 2] * (dN1 / work[j4 - 2]);
        }
        dMin = Math.min(dMin, dN);

        work[j4 + 2] = dN;
        work[4 * end - pingPong - 1] = eMin;

    }

    /**
     * Compute the shift increment as an estimate of the smallest eigenvalue.
     * <p>This implementation is a translation of the LAPACK routine DLAZQ4.</p>
     * @param start start index
     * @param end end index
     * @param deflated number of realEigenvalues just deflated
     */
    private void computeShiftIncrement(final int start, final int end, final int deflated) {

        final double cnst1 = 0.563;
        final double cnst2 = 1.010;
        final double cnst3 = 1.05;

        // a negative dMin forces the shift to take that absolute value
        // tType records the type of shift.
        if (dMin <= 0.0) {
            tau = -dMin;
            tType = -1;
            return;
        }

        int nn = 4 * end + pingPong - 1;
        switch (deflated) {

        case 0 : // no realEigenvalues deflated.
            if (dMin == dN || dMin == dN1) {

                double b1 = Math.sqrt(work[nn - 3]) * Math.sqrt(work[nn - 5]);
                double b2 = Math.sqrt(work[nn - 7]) * Math.sqrt(work[nn - 9]);
                double a2 = work[nn - 7] + work[nn - 5];

                if (dMin == dN && dMin1 == dN1) {
                    // cases 2 and 3.
                    final double gap2 = dMin2 - a2 - dMin2 * 0.25;
                    final double gap1 = a2 - dN - ((gap2 > 0.0 && gap2 > b2) ? (b2 / gap2) * b2 : (b1 + b2));
                    if (gap1 > 0.0 && gap1 > b1) {
                        tau   = Math.max(dN - (b1 / gap1) * b1, 0.5 * dMin);
                        tType = -2;
                    } else {
                        double s = 0.0;
                        if (dN > b1) {
                            s = dN - b1;
                        }
                        if (a2 > (b1 + b2)) {
                            s = Math.min(s, a2 - (b1 + b2));
                        }
                        tau   = Math.max(s, 0.333 * dMin);
                        tType = -3;
                    }
                } else {
                    // case 4.
                    tType = -4;
                    double s = 0.25 * dMin;
                    double gam;
                    int np;
                    if (dMin == dN) {
                        gam = dN;
                        a2 = 0.0;
                        if (work[nn - 5]  >  work[nn - 7]) {
                            return;
                        }
                        b2 = work[nn - 5] / work[nn - 7];
                        np = nn - 9;
                    } else {
                        np = nn - 2 * pingPong;
                        b2 = work[np - 2];
                        gam = dN1;
                        if (work[np - 4]  >  work[np - 2]) {
                            return;
                        }
                        a2 = work[np - 4] / work[np - 2];
                        if (work[nn - 9]  >  work[nn - 11]) {
                            return;
                        }
                        b2 = work[nn - 9] / work[nn - 11];
                        np = nn - 13;
                    }

                    // approximate contribution to norm squared from i < nn-1.
                    a2 = a2 + b2;
                    for (int i4 = np; i4 >= 4 * start + 2 + pingPong; i4 -= 4) {
                        if(b2 == 0.0) {
                            break;
                        }
                        b1 = b2;
                        if (work[i4]  >  work[i4 - 2]) {
                            return;
                        }
                        b2 = b2 * (work[i4] / work[i4 - 2]);
                        a2 = a2 + b2;
                        if (100 * Math.max(b2, b1) < a2 || cnst1 < a2) {
                            break;
                        }
                    }
                    a2 = cnst3 * a2;

                    // rayleigh quotient residual bound.
                    if (a2 < cnst1) {
                        s = gam * (1 - Math.sqrt(a2)) / (1 + a2);
                    }
                    tau = s;

                }
            } else if (dMin == dN2) {

                // case 5.
                tType = -5;
                double s = 0.25 * dMin;

                // compute contribution to norm squared from i > nn-2.
                final int np = nn - 2 * pingPong;
                double b1 = work[np - 2];
                double b2 = work[np - 6];
                final double gam = dN2;
                if (work[np - 8] > b2 || work[np - 4] > b1) {
                    return;
                }
                double a2 = (work[np - 8] / b2) * (1 + work[np - 4] / b1);

                // approximate contribution to norm squared from i < nn-2.
                if (end - start > 3) {
                    b2 = work[nn - 13] / work[nn - 15];
                    a2 = a2 + b2;
                    for (int i4 = nn - 17; i4 >= 4 * start + 2 + pingPong; i4 -= 4) {
                        if (b2 == 0.0) {
                            break;
                        }
                        b1 = b2;
                        if (work[i4]  >  work[i4 - 2]) {
                            return;
                        }
                        b2 = b2 * (work[i4] / work[i4 - 2]);
                        a2 = a2 + b2;
                        if (100 * Math.max(b2, b1) < a2 || cnst1 < a2)  {
                            break;
                        }
                    }
                    a2 = cnst3 * a2;
                }

                if (a2 < cnst1) {
                    tau = gam * (1 - Math.sqrt(a2)) / (1 + a2);
                } else {
                    tau = s;
                }

            } else {

                // case 6, no information to guide us.
                if (tType == -6) {
                    g += 0.333 * (1 - g);
                } else if (tType == -18) {
                    g = 0.25 * 0.333;
                } else {
                    g = 0.25;
                }
                tau   = g * dMin;
                tType = -6;

            }
            break;

        case 1 : // one eigenvalue just deflated. use dMin1, dN1 for dMin and dN.
            if (dMin1 == dN1 && dMin2 == dN2) {

                // cases 7 and 8.
                tType = -7;
                double s = 0.333 * dMin1;
                if (work[nn - 5] > work[nn - 7]) {
                    return;
                }
                double b1 = work[nn - 5] / work[nn - 7];
                double b2 = b1;
                if (b2 != 0.0) {
                    for (int i4 = 4 * end - 10 + pingPong; i4 >= 4 * start + 2 + pingPong; i4 -= 4) {
                        final double oldB1 = b1;
                        if (work[i4] > work[i4 - 2]) {
                            return;
                        }
                        b1 = b1 * (work[i4] / work[i4 - 2]);
                        b2 = b2 + b1;
                        if (100 * Math.max(b1, oldB1) < b2) {
                            break;
                        }
                    }
                }
                b2 = Math.sqrt(cnst3 * b2);
                final double a2 = dMin1 / (1 + b2 * b2);
                final double gap2 = 0.5 * dMin2 - a2;
                if (gap2 > 0.0 && gap2 > b2 * a2) {
                    tau = Math.max(s, a2 * (1 - cnst2 * a2 * (b2 / gap2) * b2));
                } else {
                    tau = Math.max(s, a2 * (1 - cnst2 * b2));
                    tType = -8;
                }
            } else {

                // case 9.
                tau = 0.25 * dMin1;
                if (dMin1 == dN1) {
                    tau = 0.5 * dMin1;
                }
                tType = -9;
            }
            break;

        case 2 : // two realEigenvalues deflated. use dMin2, dN2 for dMin and dN.

            // cases 10 and 11.
            if (dMin2 == dN2 && 2 * work[nn - 5] < work[nn - 7]) {
                tType = -10;
                final double s = 0.333 * dMin2;
                if (work[nn - 5] > work[nn - 7]) {
                    return;
                }
                double b1 = work[nn - 5] / work[nn - 7];
                double b2 = b1;
                if (b2 != 0.0){
                    for (int i4 = 4 * end - 9 + pingPong; i4 >= 4 * start + 2 + pingPong; i4 -= 4) {
                        if (work[i4] > work[i4 - 2]) {
                            return;
                        }
                        b1 *= work[i4] / work[i4 - 2];
                        b2 += b1;
                        if (100 * b1 < b2) {
                            break;
                        }
                    }
                }
                b2 = Math.sqrt(cnst3 * b2);
                final double a2 = dMin2 / (1 + b2 * b2);
                final double gap2 = work[nn - 7] + work[nn - 9] -
                Math.sqrt(work[nn - 11]) * Math.sqrt(work[nn - 9]) - a2;
                if (gap2 > 0.0 && gap2 > b2 * a2) {
                    tau = Math.max(s, a2 * (1 - cnst2 * a2 * (b2 / gap2) * b2));
                } else {
                    tau = Math.max(s, a2 * (1 - cnst2 * b2));
                }
            } else {
                tau   = 0.25 * dMin2;
                tType = -11;
            }
            break;

        default : // case 12, more than two realEigenvalues deflated. no information.
            tau   = 0.0;
            tType = -12;
        }

    }

    /**
     * Update sigma.
     * @param shift shift to apply to sigma
     */
    private void updateSigma(final double shift) {
        // BEWARE: do NOT attempt to simplify the following statements
        // the expressions below take care to accumulate the part of sigma
        // that does not fit within a double variable into sigmaLow
        if (shift < sigma) {
            sigmaLow += shift;
            final double t = sigma + sigmaLow;
            sigmaLow -= t - sigma;
            sigma = t;
        } else {
            final double t = sigma + shift;
            sigmaLow += sigma - (t - shift);
            sigma = t;
        }
    }

    /**
     * Find eigenvectors.
     */
    private void findEigenVectors() {

        final int m = main.length;
        eigenvectors = new ArrayRealVector[m];

        // perform an initial non-shifted LDLt decomposition
        final double[] d = new double[m];
        final double[] l = new double[m - 1];
        // avoid zero divide on indefinite matrix
        final double mu = realEigenvalues[m-1] <= 0 && realEigenvalues[0] > 0 ? 0.5-realEigenvalues[m-1] : 0;
        double di = main[0]+mu;
        d[0] = di;
        for (int i = 1; i < m; ++i) {
            final double eiM1  = secondary[i - 1];
            final double ratio = eiM1 / di;
            di       = main[i] - eiM1 * ratio + mu;
            l[i - 1] = ratio;
            d[i]     = di;
        }

        // compute eigenvectors
        for (int i = 0; i < m; ++i) {
            eigenvectors[i] = findEigenvector(realEigenvalues[i]+mu, d, l);
        }

    }

    /**
     * Find an eigenvector corresponding to an eigenvalue, using bidiagonals.
     * <p>This method corresponds to algorithm X from Dhillon's thesis.</p>
     *
     * @param eigenvalue eigenvalue for which eigenvector is desired
     * @param d diagonal elements of the initial non-shifted D matrix
     * @param l off-diagonal elements of the initial non-shifted L matrix
     * @return an eigenvector
     */
    private ArrayRealVector findEigenvector(final double eigenvalue,
                                           final double[] d, final double[] l) {

        // compute the LDLt and UDUt decompositions of the
        // perfectly shifted tridiagonal matrix
        final int m = main.length;
        stationaryQuotientDifferenceWithShift(d, l, eigenvalue);
        progressiveQuotientDifferenceWithShift(d, l, eigenvalue);

        // select the twist index leading to
        // the least diagonal element in the twisted factorization
        int r = m - 1;
        double minG = Math.abs(work[6 * r] + work[6 * r + 3] + eigenvalue);
        int sixI = 0;
        for (int i = 0; i < m - 1; ++i) {
            final double absG = Math.abs(work[sixI] + d[i] * work[sixI + 9] / work[sixI + 10]);
            if (absG < minG) {
                r = i;
                minG = absG;
            }
            sixI += 6;
        }

        // solve the singular system by ignoring the equation
        // at twist index and propagating upwards and downwards
        double[] eigenvector = new double[m];
        double n2 = 1;
        eigenvector[r] = 1;
        double z = 1;
        for (int i = r - 1; i >= 0; --i) {
            z *= -work[6 * i + 2];
            eigenvector[i] = z;
            n2 += z * z;
        }
        z = 1;
        for (int i = r + 1; i < m; ++i) {
            z *= -work[6 * i - 1];
            eigenvector[i] = z;
            n2 += z * z;
        }

        // normalize vector
        final double inv = 1.0 / Math.sqrt(n2);
        for (int i = 0; i < m; ++i) {
            eigenvector[i] *= inv;
        }

        return (transformer == null) ?
               new ArrayRealVector(eigenvector, false) :
               new ArrayRealVector(transformer.getQ().operate(eigenvector), false);

    }

    /**
     * Decompose matrix LDL<sup>T</sup> - &lambda; I as
     * L<sub>+</sub>D<sub>+</sub>L<sub>+</sub><sup>T</sup>.
     * <p>This method corresponds to algorithm 4.4.3 (dstqds) from Dhillon's thesis.</p>
     * @param d diagonal elements of D,
     * @param l off-diagonal elements of L
     * @param lambda shift to apply
     */
    private void stationaryQuotientDifferenceWithShift(final double[] d, final double[] l,
                                                       final double lambda) {
        final int nM1 = d.length - 1;
        double si = -lambda;
        int sixI = 0;
        for (int i = 0; i < nM1; ++i) {
            final double di   = d[i];
            final double li   = l[i];
            final double diP1 = di + si;
            final double liP1 = li * di / diP1;
            work[sixI]        = si;
            work[sixI + 1]    = diP1;
            work[sixI + 2]    = liP1;
            si = li * liP1 * si - lambda;
            sixI += 6;
        }
        work[6 * nM1 + 1] = d[nM1] + si;
        work[6 * nM1]     = si;
    }

    /**
     * Decompose matrix LDL<sup>T</sup> - &lambda; I as
     * U<sub>-</sub>D<sub>-</sub>U<sub>-</sub><sup>T</sup>.
     * <p>This method corresponds to algorithm 4.4.5 (dqds) from Dhillon's thesis.</p>
     * @param d diagonal elements of D
     * @param l off-diagonal elements of L
     * @param lambda shift to apply
     */
    private void progressiveQuotientDifferenceWithShift(final double[] d, final double[] l,
                                                        final double lambda) {
        final int nM1 = d.length - 1;
        double pi = d[nM1] - lambda;
        int sixI = 6 * (nM1 - 1);
        for (int i = nM1 - 1; i >= 0; --i) {
            final double di   = d[i];
            final double li   = l[i];
            final double diP1 = di * li * li + pi;
            final double t    = di / diP1;
            work[sixI +  9]   = pi;
            work[sixI + 10]   = diP1;
            work[sixI +  5]   = li * t;
            pi = pi * t - lambda;
            sixI -= 6;
        }
        work[3] = pi;
        work[4] = pi;
    }

}
