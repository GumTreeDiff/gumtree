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

package org.apache.commons.math.util;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Some useful additions to the built-in functions in {@link Math}.
 * @version $Revision$ $Date$
 */
public final class MathUtils {

    /** Smallest positive number such that 1 - EPSILON is not numerically equal to 1. */
    public static final double EPSILON = 0x1.0p-53;

    /** Safe minimum, such that 1 / SAFE_MIN does not overflow.
     * <p>In IEEE 754 arithmetic, this is also the smallest normalized
     * number 2<sup>-1022</sup>.</p>
     */
    public static final double SAFE_MIN = 0x1.0p-1022;

    /** -1.0 cast as a byte. */
    private static final byte  NB = (byte)-1;

    /** -1.0 cast as a short. */
    private static final short NS = (short)-1;

    /** 1.0 cast as a byte. */
    private static final byte  PB = (byte)1;

    /** 1.0 cast as a short. */
    private static final short PS = (short)1;

    /** 0.0 cast as a byte. */
    private static final byte  ZB = (byte)0;

    /** 0.0 cast as a short. */
    private static final short ZS = (short)0;

    /** 2 &pi;. */
    private static final double TWO_PI = 2 * Math.PI;

    /**
     * Private Constructor
     */
    private MathUtils() {
        super();
    }

    /**
     * Add two integers, checking for overflow.
     * 
     * @param x an addend
     * @param y an addend
     * @return the sum <code>x+y</code>
     * @throws ArithmeticException if the result can not be represented as an
     *         int
     * @since 1.1
     */
    public static int addAndCheck(int x, int y) {
        long s = (long)x + (long)y;
        if (s < Integer.MIN_VALUE || s > Integer.MAX_VALUE) {
            throw new ArithmeticException("overflow: add");
        }
        return (int)s;
    }

    /**
     * Add two long integers, checking for overflow.
     * 
     * @param a an addend
     * @param b an addend
     * @return the sum <code>a+b</code>
     * @throws ArithmeticException if the result can not be represented as an
     *         long
     * @since 1.2
     */
    public static long addAndCheck(long a, long b) {
        return addAndCheck(a, b, "overflow: add");
    }
    
    /**
     * Add two long integers, checking for overflow.
     * 
     * @param a an addend
     * @param b an addend
     * @param msg the message to use for any thrown exception.
     * @return the sum <code>a+b</code>
     * @throws ArithmeticException if the result can not be represented as an
     *         long
     * @since 1.2
     */
    private static long addAndCheck(long a, long b, String msg) {
        long ret;
        if (a > b) {
            // use symmetry to reduce boundry cases
            ret = addAndCheck(b, a, msg);
        } else {
            // assert a <= b
            
            if (a < 0) {
                if (b < 0) {
                    // check for negative overflow
                    if (Long.MIN_VALUE - b <= a) {
                        ret = a + b;
                    } else {
                        throw new ArithmeticException(msg);
                    }
                } else {
                    // oppisite sign addition is always safe
                    ret = a + b;
                }
            } else {
                // assert a >= 0
                // assert b >= 0

                // check for positive overflow
                if (a <= Long.MAX_VALUE - b) {
                    ret = a + b;
                } else {
                    throw new ArithmeticException(msg);
                }
            }
        }
        return ret;
    }
    
    /**
     * Returns an exact representation of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "<code>n choose k</code>", the number of
     * <code>k</code>-element subsets that can be selected from an
     * <code>n</code>-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>0 <= k <= n </code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * <li> The result is small enough to fit into a <code>long</code>. The
     * largest value of <code>n</code> for which all coefficients are
     * <code> < Long.MAX_VALUE</code> is 66. If the computed value exceeds
     * <code>Long.MAX_VALUE</code> an <code>ArithMeticException</code> is
     * thrown.</li>
     * </ul></p>
     * 
     * @param n the size of the set
     * @param k the size of the subsets to be counted
     * @return <code>n choose k</code>
     * @throws IllegalArgumentException if preconditions are not met.
     * @throws ArithmeticException if the result is too large to be represented
     *         by a long integer.
     */
    public static long binomialCoefficient(final int n, final int k) {
        if (n < k) {
            throw new IllegalArgumentException(
                "must have n >= k for binomial coefficient (n,k)");
        }
        if (n < 0) {
            throw new IllegalArgumentException(
                "must have n >= 0 for binomial coefficient (n,k)");
        }
        if ((n == k) || (k == 0)) {
            return 1;
        }
        if ((k == 1) || (k == n - 1)) {
            return n;
        }

        long result = Math.round(binomialCoefficientDouble(n, k));
        if (result == Long.MAX_VALUE) {
            throw new ArithmeticException(
                "result too large to represent in a long integer");
        }
        return result;
    }

    /**
     * Returns a <code>double</code> representation of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "<code>n choose k</code>", the number of
     * <code>k</code>-element subsets that can be selected from an
     * <code>n</code>-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>0 <= k <= n </code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * <li> The result is small enough to fit into a <code>double</code>. The
     * largest value of <code>n</code> for which all coefficients are <
     * Double.MAX_VALUE is 1029. If the computed value exceeds Double.MAX_VALUE,
     * Double.POSITIVE_INFINITY is returned</li>
     * </ul></p>
     * 
     * @param n the size of the set
     * @param k the size of the subsets to be counted
     * @return <code>n choose k</code>
     * @throws IllegalArgumentException if preconditions are not met.
     */
    public static double binomialCoefficientDouble(final int n, final int k) {
        return Math.floor(Math.exp(binomialCoefficientLog(n, k)) + 0.5);
    }
    
    /**
     * Returns the natural <code>log</code> of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "<code>n choose k</code>", the number of
     * <code>k</code>-element subsets that can be selected from an
     * <code>n</code>-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>0 <= k <= n </code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * </ul></p>
     * 
     * @param n the size of the set
     * @param k the size of the subsets to be counted
     * @return <code>n choose k</code>
     * @throws IllegalArgumentException if preconditions are not met.
     */
    public static double binomialCoefficientLog(final int n, final int k) {
        if (n < k) {
            throw new IllegalArgumentException(
                "must have n >= k for binomial coefficient (n,k)");
        }
        if (n < 0) {
            throw new IllegalArgumentException(
                "must have n >= 0 for binomial coefficient (n,k)");
        }
        if ((n == k) || (k == 0)) {
            return 0;
        }
        if ((k == 1) || (k == n - 1)) {
            return Math.log((double)n);
        }
        double logSum = 0;

        // n!/k!
        for (int i = k + 1; i <= n; i++) {
            logSum += Math.log((double)i);
        }

        // divide by (n-k)!
        for (int i = 2; i <= n - k; i++) {
            logSum -= Math.log((double)i);
        }

        return logSum;
    }
    
    /**
     * Returns the <a href="http://mathworld.wolfram.com/HyperbolicCosine.html">
     * hyperbolic cosine</a> of x.
     * 
     * @param x double value for which to find the hyperbolic cosine
     * @return hyperbolic cosine of x
     */
    public static double cosh(double x) {
        return (Math.exp(x) + Math.exp(-x)) / 2.0;
    }
    
    /**
     * Returns true iff both arguments are NaN or neither is NaN and they are
     * equal
     * 
     * @param x first value
     * @param y second value
     * @return true if the values are equal or both are NaN
     */
    public static boolean equals(double x, double y) {
        return ((Double.isNaN(x) && Double.isNaN(y)) || x == y);
    }

    /**
     * Returns true iff both arguments are null or have same dimensions
     * and all their elements are {@link #equals(double,double) equals}
     * 
     * @param x first array
     * @param y second array
     * @return true if the values are both null or have same dimension
     * and equal elements
     * @since 1.2
     */
    public static boolean equals(double[] x, double[] y) {
        if ((x == null) || (y == null)) {
            return !((x == null) ^ (y == null));
        }
        if (x.length != y.length) {
            return false;
        }
        for (int i = 0; i < x.length; ++i) {
            if (!equals(x[i], y[i])) {
                return false;
            }
        }
        return true;
    }
    
    /** All long-representable factorials */
    private static final long[] factorials = new long[] 
       {1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800, 39916800,
        479001600, 6227020800l, 87178291200l, 1307674368000l, 20922789888000l,
        355687428096000l, 6402373705728000l, 121645100408832000l,
        2432902008176640000l};

    /**
     * Returns n!. Shorthand for <code>n</code> <a
     * href="http://mathworld.wolfram.com/Factorial.html"> Factorial</a>, the
     * product of the numbers <code>1,...,n</code>.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>n >= 0</code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * <li> The result is small enough to fit into a <code>long</code>. The
     * largest value of <code>n</code> for which <code>n!</code> <
     * Long.MAX_VALUE</code> is 20. If the computed value exceeds <code>Long.MAX_VALUE</code>
     * an <code>ArithMeticException </code> is thrown.</li>
     * </ul>
     * </p>
     * 
     * @param n argument
     * @return <code>n!</code>
     * @throws ArithmeticException if the result is too large to be represented
     *         by a long integer.
     * @throws IllegalArgumentException if n < 0
     */
    public static long factorial(final int n) {
        if (n < 0) {
            throw new IllegalArgumentException("must have n >= 0 for n!");
        }
        if (n > 20) {
            throw new ArithmeticException(
                    "factorial value is too large to fit in a long");
        }
        return factorials[n];
    }

    /**
     * Returns n!. Shorthand for <code>n</code> <a
     * href="http://mathworld.wolfram.com/Factorial.html"> Factorial</a>, the
     * product of the numbers <code>1,...,n</code> as a <code>double</code>.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>n >= 0</code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * <li> The result is small enough to fit into a <code>double</code>. The
     * largest value of <code>n</code> for which <code>n!</code> <
     * Double.MAX_VALUE</code> is 170. If the computed value exceeds
     * Double.MAX_VALUE, Double.POSITIVE_INFINITY is returned</li>
     * </ul>
     * </p>
     * 
     * @param n argument
     * @return <code>n!</code>
     * @throws IllegalArgumentException if n < 0
     */
    public static double factorialDouble(final int n) {
        if (n < 0) {
            throw new IllegalArgumentException("must have n >= 0 for n!");
        }
        if (n < 21) {
            return factorial(n);
        }
        return Math.floor(Math.exp(factorialLog(n)) + 0.5);
    }

    /**
     * Returns the natural logarithm of n!.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>n >= 0</code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * </ul></p>
     * 
     * @param n argument
     * @return <code>n!</code>
     * @throws IllegalArgumentException if preconditions are not met.
     */
    public static double factorialLog(final int n) {
        if (n < 0) {
            throw new IllegalArgumentException("must have n > 0 for n!");
        }
        if (n < 21) {
            return Math.log(factorial(n));
        }
        double logSum = 0;
        for (int i = 2; i <= n; i++) {
            logSum += Math.log((double)i);
        }
        return logSum;
    }

    /**
     * <p>
     * Gets the greatest common divisor of the absolute value of two numbers,
     * using the "binary gcd" method which avoids division and modulo
     * operations. See Knuth 4.5.2 algorithm B. This algorithm is due to Josef
     * Stein (1961).
     * </p>
     * 
     * @param u a non-zero number
     * @param v a non-zero number
     * @return the greatest common divisor, never zero
     * @since 1.1
     */
    public static int gcd(int u, int v) {
        if ((u == 0) || (v == 0)) {
            return (Math.abs(u) + Math.abs(v));
        }
        // keep u and v negative, as negative integers range down to
        // -2^31, while positive numbers can only be as large as 2^31-1
        // (i.e. we can't necessarily negate a negative number without
        // overflow)
        /* assert u!=0 && v!=0; */
        if (u > 0) {
            u = -u;
        } // make u negative
        if (v > 0) {
            v = -v;
        } // make v negative
        // B1. [Find power of 2]
        int k = 0;
        while ((u & 1) == 0 && (v & 1) == 0 && k < 31) { // while u and v are
                                                            // both even...
            u /= 2;
            v /= 2;
            k++; // cast out twos.
        }
        if (k == 31) {
            throw new ArithmeticException("overflow: gcd is 2^31");
        }
        // B2. Initialize: u and v have been divided by 2^k and at least
        // one is odd.
        int t = ((u & 1) == 1) ? v : -(u / 2)/* B3 */;
        // t negative: u was odd, v may be even (t replaces v)
        // t positive: u was even, v is odd (t replaces u)
        do {
            /* assert u<0 && v<0; */
            // B4/B3: cast out twos from t.
            while ((t & 1) == 0) { // while t is even..
                t /= 2; // cast out twos
            }
            // B5 [reset max(u,v)]
            if (t > 0) {
                u = -t;
            } else {
                v = t;
            }
            // B6/B3. at this point both u and v should be odd.
            t = (v - u) / 2;
            // |u| larger: t positive (replace u)
            // |v| larger: t negative (replace v)
        } while (t != 0);
        return -u * (1 << k); // gcd is u*2^k
    }

    /**
     * Returns an integer hash code representing the given double value.
     * 
     * @param value the value to be hashed
     * @return the hash code
     */
    public static int hash(double value) {
        return new Double(value).hashCode();
    }

    /**
     * Returns an integer hash code representing the given double array.
     * 
     * @param value the value to be hashed (may be null)
     * @return the hash code
     * @since 1.2
     */
    public static int hash(double[] value) {
        return Arrays.hashCode(value);
    }

    /**
     * For a byte value x, this method returns (byte)(+1) if x >= 0 and
     * (byte)(-1) if x < 0.
     * 
     * @param x the value, a byte
     * @return (byte)(+1) or (byte)(-1), depending on the sign of x
     */
    public static byte indicator(final byte x) {
        return (x >= ZB) ? PB : NB;
    }

    /**
     * For a double precision value x, this method returns +1.0 if x >= 0 and
     * -1.0 if x < 0. Returns <code>NaN</code> if <code>x</code> is
     * <code>NaN</code>.
     * 
     * @param x the value, a double
     * @return +1.0 or -1.0, depending on the sign of x
     */
    public static double indicator(final double x) {
        if (Double.isNaN(x)) {
            return Double.NaN;
        }
        return (x >= 0.0) ? 1.0 : -1.0;
    }

    /**
     * For a float value x, this method returns +1.0F if x >= 0 and -1.0F if x <
     * 0. Returns <code>NaN</code> if <code>x</code> is <code>NaN</code>.
     * 
     * @param x the value, a float
     * @return +1.0F or -1.0F, depending on the sign of x
     */
    public static float indicator(final float x) {
        if (Float.isNaN(x)) {
            return Float.NaN;
        }
        return (x >= 0.0F) ? 1.0F : -1.0F;
    }

    /**
     * For an int value x, this method returns +1 if x >= 0 and -1 if x < 0.
     * 
     * @param x the value, an int
     * @return +1 or -1, depending on the sign of x
     */
    public static int indicator(final int x) {
        return (x >= 0) ? 1 : -1;
    }

    /**
     * For a long value x, this method returns +1L if x >= 0 and -1L if x < 0.
     * 
     * @param x the value, a long
     * @return +1L or -1L, depending on the sign of x
     */
    public static long indicator(final long x) {
        return (x >= 0L) ? 1L : -1L;
    }

    /**
     * For a short value x, this method returns (short)(+1) if x >= 0 and
     * (short)(-1) if x < 0.
     * 
     * @param x the value, a short
     * @return (short)(+1) or (short)(-1), depending on the sign of x
     */
    public static short indicator(final short x) {
        return (x >= ZS) ? PS : NS;
    }

    /**
     * Returns the least common multiple between two integer values.
     * 
     * @param a the first integer value.
     * @param b the second integer value.
     * @return the least common multiple between a and b.
     * @throws ArithmeticException if the lcm is too large to store as an int
     * @since 1.1
     */
    public static int lcm(int a, int b) {
        return Math.abs(mulAndCheck(a / gcd(a, b), b));
    }

    /** 
     * <p>Returns the 
     * <a href="http://mathworld.wolfram.com/Logarithm.html">logarithm</a>
     * for base <code>b</code> of <code>x</code>.
     * </p>
     * <p>Returns <code>NaN<code> if either argument is negative.  If 
     * <code>base</code> is 0 and <code>x</code> is positive, 0 is returned.
     * If <code>base</code> is positive and <code>x</code> is 0, 
     * <code>Double.NEGATIVE_INFINITY</code> is returned.  If both arguments
     * are 0, the result is <code>NaN</code>.</p>
     * 
     * @param base the base of the logarithm, must be greater than 0
     * @param x argument, must be greater than 0
     * @return the value of the logarithm - the number y such that base^y = x.
     * @since 1.2
     */ 
    public static double log(double base, double x) {
        return Math.log(x)/Math.log(base);
    }

    /**
     * Multiply two integers, checking for overflow.
     * 
     * @param x a factor
     * @param y a factor
     * @return the product <code>x*y</code>
     * @throws ArithmeticException if the result can not be represented as an
     *         int
     * @since 1.1
     */
    public static int mulAndCheck(int x, int y) {
        long m = ((long)x) * ((long)y);
        if (m < Integer.MIN_VALUE || m > Integer.MAX_VALUE) {
            throw new ArithmeticException("overflow: mul");
        }
        return (int)m;
    }

    /**
     * Multiply two long integers, checking for overflow.
     * 
     * @param a first value
     * @param b second value
     * @return the product <code>a * b</code>
     * @throws ArithmeticException if the result can not be represented as an
     *         long
     * @since 1.2
     */
    public static long mulAndCheck(long a, long b) {
        long ret;
        String msg = "overflow: multiply";
        if (a > b) {
            // use symmetry to reduce boundry cases
            ret = mulAndCheck(b, a);
        } else {
            if (a < 0) {
                if (b < 0) {
                    // check for positive overflow with negative a, negative b
                    if (a >= Long.MAX_VALUE / b) {
                        ret = a * b;
                    } else {
                        throw new ArithmeticException(msg);
                    }
                } else if (b > 0) {
                    // check for negative overflow with negative a, positive b
                    if (Long.MIN_VALUE / b <= a) {
                        ret = a * b;
                    } else {
                        throw new ArithmeticException(msg);
                        
                    }
                } else {
                    // assert b == 0
                    ret = 0;
                }
            } else if (a > 0) {
                // assert a > 0
                // assert b > 0
                
                // check for positive overflow with positive a, positive b
                if (a <= Long.MAX_VALUE / b) {
                    ret = a * b;
                } else {
                    throw new ArithmeticException(msg);
                }
            } else {
                // assert a == 0
                ret = 0;
            }
        }
        return ret;
    }

    /**
     * Get the next machine representable number after a number, moving
     * in the direction of another number.
     * <p>
     * If <code>direction</code> is greater than or equal to<code>d</code>,
     * the smallest machine representable number strictly greater than
     * <code>d</code> is returned; otherwise the largest representable number
     * strictly less than <code>d</code> is returned.</p>
     * <p>
     * If <code>d</code> is NaN or Infinite, it is returned unchanged.</p>
     * 
     * @param d base number
     * @param direction (the only important thing is whether
     * direction is greater or smaller than d)
     * @return the next machine representable number in the specified direction
     * @since 1.2
     */
    public static double nextAfter(double d, double direction) {

        // handling of some important special cases
        if (Double.isNaN(d) || Double.isInfinite(d)) {
                return d;
        } else if (d == 0) {
                return (direction < 0) ? -Double.MIN_VALUE : Double.MIN_VALUE;
        }
        // special cases MAX_VALUE to infinity and  MIN_VALUE to 0
        // are handled just as normal numbers

        // split the double in raw components
        long bits     = Double.doubleToLongBits(d);
        long sign     = bits & 0x8000000000000000L;
        long exponent = bits & 0x7ff0000000000000L;
        long mantissa = bits & 0x000fffffffffffffL;

        if (d * (direction - d) >= 0) {
                // we should increase the mantissa
                if (mantissa == 0x000fffffffffffffL) {
                        return Double.longBitsToDouble(sign |
                                        (exponent + 0x0010000000000000L));
                } else {
                        return Double.longBitsToDouble(sign |
                                        exponent | (mantissa + 1));
                }
        } else {
                // we should decrease the mantissa
                if (mantissa == 0L) {
                        return Double.longBitsToDouble(sign |
                                        (exponent - 0x0010000000000000L) |
                                        0x000fffffffffffffL);
                } else {
                        return Double.longBitsToDouble(sign |
                                        exponent | (mantissa - 1));
                }
        }

    }

    /**
     * Scale a number by 2<sup>scaleFactor</sup>.
     * <p>If <code>d</code> is 0 or NaN or Infinite, it is returned unchanged.</p>
     * 
     * @param d base number
     * @param scaleFactor power of two by which d sould be multiplied
     * @return d &times; 2<sup>scaleFactor</sup>
     * @since 2.0
     */
    public static double scalb(final double d, final int scaleFactor) {

        // handling of some important special cases
        if ((d == 0) || Double.isNaN(d) || Double.isInfinite(d)) {
            return d;
        }

        // split the double in raw components
        final long bits     = Double.doubleToLongBits(d);
        final long exponent = bits & 0x7ff0000000000000L;
        final long rest     = bits & 0x800fffffffffffffL;

        // shift the exponent
        final long newBits = rest | (exponent + (((long) scaleFactor) << 52));
        return Double.longBitsToDouble(newBits);

    }

    /**
     * Normalize an angle in a 2&pi wide interval around a center value.
     * <p>This method has three main uses:</p>
     * <ul>
     *   <li>normalize an angle between 0 and 2&pi;:<br/>
     *       <code>a = MathUtils.normalizeAngle(a, Math.PI);</code></li>
     *   <li>normalize an angle between -&pi; and +&pi;<br/>
     *       <code>a = MathUtils.normalizeAngle(a, 0.0);</code></li>
     *   <li>compute the angle between two defining angular positions:<br>
     *       <code>angle = MathUtils.normalizeAngle(end, start) - start;</code></li>
     * </ul>
     * <p>Note that due to numerical accuracy and since &pi; cannot be represented
     * exactly, the result interval is <em>closed</em>, it cannot be half-closed
     * as would be more satisfactory in a purely mathematical view.</p>
     * @param a angle to normalize
     * @param center center of the desired 2&pi; interval for the result
     * @return a-2k&pi; with integer k and center-&pi; &lt;= a-2k&pi; &lt;= center+&pi;
     * @since 1.2
     */
     public static double normalizeAngle(double a, double center) {
         return a - TWO_PI * Math.floor((a + Math.PI - center) / TWO_PI);
     }

    /**
     * Round the given value to the specified number of decimal places. The
     * value is rounded using the {@link BigDecimal#ROUND_HALF_UP} method.
     * 
     * @param x the value to round.
     * @param scale the number of digits to the right of the decimal point.
     * @return the rounded value.
     * @since 1.1
     */
    public static double round(double x, int scale) {
        return round(x, scale, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Round the given value to the specified number of decimal places. The
     * value is rounded using the given method which is any method defined in
     * {@link BigDecimal}.
     * 
     * @param x the value to round.
     * @param scale the number of digits to the right of the decimal point.
     * @param roundingMethod the rounding method as defined in
     *        {@link BigDecimal}.
     * @return the rounded value.
     * @since 1.1
     */
    public static double round(double x, int scale, int roundingMethod) {
        try {
            return (new BigDecimal
                   (Double.toString(x))
                   .setScale(scale, roundingMethod))
                   .doubleValue();
        } catch (NumberFormatException ex) {
            if (Double.isInfinite(x)) {
                return x;          
            } else {
                return Double.NaN;
            }
        }
    }

    /**
     * Round the given value to the specified number of decimal places. The
     * value is rounding using the {@link BigDecimal#ROUND_HALF_UP} method.
     * 
     * @param x the value to round.
     * @param scale the number of digits to the right of the decimal point.
     * @return the rounded value.
     * @since 1.1
     */
    public static float round(float x, int scale) {
        return round(x, scale, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Round the given value to the specified number of decimal places. The
     * value is rounded using the given method which is any method defined in
     * {@link BigDecimal}.
     * 
     * @param x the value to round.
     * @param scale the number of digits to the right of the decimal point.
     * @param roundingMethod the rounding method as defined in
     *        {@link BigDecimal}.
     * @return the rounded value.
     * @since 1.1
     */
    public static float round(float x, int scale, int roundingMethod) {
        float sign = indicator(x);
        float factor = (float)Math.pow(10.0f, scale) * sign;
        return (float)roundUnscaled(x * factor, sign, roundingMethod) / factor;
    }

    /**
     * Round the given non-negative, value to the "nearest" integer. Nearest is
     * determined by the rounding method specified. Rounding methods are defined
     * in {@link BigDecimal}.
     * 
     * @param unscaled the value to round.
     * @param sign the sign of the original, scaled value.
     * @param roundingMethod the rounding method as defined in
     *        {@link BigDecimal}.
     * @return the rounded value.
     * @since 1.1
     */
    private static double roundUnscaled(double unscaled, double sign,
        int roundingMethod) {
        switch (roundingMethod) {
        case BigDecimal.ROUND_CEILING :
            if (sign == -1) {
                unscaled = Math.floor(nextAfter(unscaled, Double.NEGATIVE_INFINITY));
            } else {
                unscaled = Math.ceil(nextAfter(unscaled, Double.POSITIVE_INFINITY));
            }
            break;
        case BigDecimal.ROUND_DOWN :
            unscaled = Math.floor(nextAfter(unscaled, Double.NEGATIVE_INFINITY));
            break;
        case BigDecimal.ROUND_FLOOR :
            if (sign == -1) {
                unscaled = Math.ceil(nextAfter(unscaled, Double.POSITIVE_INFINITY));
            } else {
                unscaled = Math.floor(nextAfter(unscaled, Double.NEGATIVE_INFINITY));
            }
            break;
        case BigDecimal.ROUND_HALF_DOWN : {
            unscaled = nextAfter(unscaled, Double.NEGATIVE_INFINITY);
            double fraction = unscaled - Math.floor(unscaled);
            if (fraction > 0.5) {
                unscaled = Math.ceil(unscaled);
            } else {
                unscaled = Math.floor(unscaled);
            }
            break;
        }
        case BigDecimal.ROUND_HALF_EVEN : {
            double fraction = unscaled - Math.floor(unscaled);
            if (fraction > 0.5) {
                unscaled = Math.ceil(unscaled);
            } else if (fraction < 0.5) {
                unscaled = Math.floor(unscaled);
            } else {
                // The following equality test is intentional and needed for rounding purposes
                if (Math.floor(unscaled) / 2.0 == Math.floor(Math
                    .floor(unscaled) / 2.0)) { // even
                    unscaled = Math.floor(unscaled);
                } else { // odd
                    unscaled = Math.ceil(unscaled);
                }
            }
            break;
        }
        case BigDecimal.ROUND_HALF_UP : {
            unscaled = nextAfter(unscaled, Double.POSITIVE_INFINITY);
            double fraction = unscaled - Math.floor(unscaled);
            if (fraction >= 0.5) {
                unscaled = Math.ceil(unscaled);
            } else {
                unscaled = Math.floor(unscaled);
            }
            break;
        }
        case BigDecimal.ROUND_UNNECESSARY :
            if (unscaled != Math.floor(unscaled)) {
                throw new ArithmeticException("Inexact result from rounding");
            }
            break;
        case BigDecimal.ROUND_UP :
            unscaled = Math.ceil(nextAfter(unscaled,  Double.POSITIVE_INFINITY));
            break;
        default :
            throw new IllegalArgumentException("Invalid rounding method.");
        }
        return unscaled;
    }

    /**
     * Returns the <a href="http://mathworld.wolfram.com/Sign.html"> sign</a>
     * for byte value <code>x</code>.
     * <p>
     * For a byte value x, this method returns (byte)(+1) if x > 0, (byte)(0) if
     * x = 0, and (byte)(-1) if x < 0.</p>
     * 
     * @param x the value, a byte
     * @return (byte)(+1), (byte)(0), or (byte)(-1), depending on the sign of x
     */
    public static byte sign(final byte x) {
        return (x == ZB) ? ZB : (x > ZB) ? PB : NB;
    }

    /**
     * Returns the <a href="http://mathworld.wolfram.com/Sign.html"> sign</a>
     * for double precision <code>x</code>.
     * <p>
     * For a double value <code>x</code>, this method returns
     * <code>+1.0</code> if <code>x > 0</code>, <code>0.0</code> if
     * <code>x = 0.0</code>, and <code>-1.0</code> if <code>x < 0</code>.
     * Returns <code>NaN</code> if <code>x</code> is <code>NaN</code>.</p>
     * 
     * @param x the value, a double
     * @return +1.0, 0.0, or -1.0, depending on the sign of x
     */
    public static double sign(final double x) {
        if (Double.isNaN(x)) {
            return Double.NaN;
        }
        return (x == 0.0) ? 0.0 : (x > 0.0) ? 1.0 : -1.0;
    }

    /**
     * Returns the <a href="http://mathworld.wolfram.com/Sign.html"> sign</a>
     * for float value <code>x</code>.
     * <p>
     * For a float value x, this method returns +1.0F if x > 0, 0.0F if x =
     * 0.0F, and -1.0F if x < 0. Returns <code>NaN</code> if <code>x</code>
     * is <code>NaN</code>.</p>
     * 
     * @param x the value, a float
     * @return +1.0F, 0.0F, or -1.0F, depending on the sign of x
     */
    public static float sign(final float x) {
        if (Float.isNaN(x)) {
            return Float.NaN;
        }
        return (x == 0.0F) ? 0.0F : (x > 0.0F) ? 1.0F : -1.0F;
    }

    /**
     * Returns the <a href="http://mathworld.wolfram.com/Sign.html"> sign</a>
     * for int value <code>x</code>.
     * <p>
     * For an int value x, this method returns +1 if x > 0, 0 if x = 0, and -1
     * if x < 0.</p>
     * 
     * @param x the value, an int
     * @return +1, 0, or -1, depending on the sign of x
     */
    public static int sign(final int x) {
        return (x == 0) ? 0 : (x > 0) ? 1 : -1;
    }

    /**
     * Returns the <a href="http://mathworld.wolfram.com/Sign.html"> sign</a>
     * for long value <code>x</code>.
     * <p>
     * For a long value x, this method returns +1L if x > 0, 0L if x = 0, and
     * -1L if x < 0.</p>
     * 
     * @param x the value, a long
     * @return +1L, 0L, or -1L, depending on the sign of x
     */
    public static long sign(final long x) {
        return (x == 0L) ? 0L : (x > 0L) ? 1L : -1L;
    }

    /**
     * Returns the <a href="http://mathworld.wolfram.com/Sign.html"> sign</a>
     * for short value <code>x</code>.
     * <p>
     * For a short value x, this method returns (short)(+1) if x > 0, (short)(0)
     * if x = 0, and (short)(-1) if x < 0.</p>
     * 
     * @param x the value, a short
     * @return (short)(+1), (short)(0), or (short)(-1), depending on the sign of
     *         x
     */
    public static short sign(final short x) {
        return (x == ZS) ? ZS : (x > ZS) ? PS : NS;
    }

    /**
     * Returns the <a href="http://mathworld.wolfram.com/HyperbolicSine.html">
     * hyperbolic sine</a> of x.
     * 
     * @param x double value for which to find the hyperbolic sine
     * @return hyperbolic sine of x
     */
    public static double sinh(double x) {
        return (Math.exp(x) - Math.exp(-x)) / 2.0;
    }

    /**
     * Subtract two integers, checking for overflow.
     * 
     * @param x the minuend
     * @param y the subtrahend
     * @return the difference <code>x-y</code>
     * @throws ArithmeticException if the result can not be represented as an
     *         int
     * @since 1.1
     */
    public static int subAndCheck(int x, int y) {
        long s = (long)x - (long)y;
        if (s < Integer.MIN_VALUE || s > Integer.MAX_VALUE) {
            throw new ArithmeticException("overflow: subtract");
        }
        return (int)s;
    }

    /**
     * Subtract two long integers, checking for overflow.
     * 
     * @param a first value
     * @param b second value
     * @return the difference <code>a-b</code>
     * @throws ArithmeticException if the result can not be represented as an
     *         long
     * @since 1.2
     */
    public static long subAndCheck(long a, long b) {
        long ret;
        String msg = "overflow: subtract";
        if (b == Long.MIN_VALUE) {
            if (a < 0) {
                ret = a - b;
            } else {
                throw new ArithmeticException(msg);
            }
        } else {
            // use additive inverse
            ret = addAndCheck(a, -b, msg);
        }
        return ret;
    }

}
