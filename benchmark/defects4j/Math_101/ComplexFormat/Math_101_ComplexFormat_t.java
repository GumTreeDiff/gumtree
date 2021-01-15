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

package org.apache.commons.math.complex;

import java.io.Serializable;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

/**
 * Formats a Complex number in cartesian format "Re(c) + Im(c)i".  'i' can
 * be replaced with 'j', and the number format for both real and imaginary parts
 * can be configured.
 *
 * @author Apache Software Foundation
 * @version $Revision$ $Date$
 */
public class ComplexFormat extends Format implements Serializable {
    
    /** Serializable version identifier */
    private static final long serialVersionUID = -6337346779577272306L;
    
    /** The default imaginary character. */
    private static final String DEFAULT_IMAGINARY_CHARACTER = "i";
    
    /** The notation used to signify the imaginary part of the complex number. */
    private String imaginaryCharacter;
    
    /** The format used for the imaginary part. */
    private NumberFormat imaginaryFormat;

    /** The format used for the real part. */
    private NumberFormat realFormat;
    
    /**
     * Create an instance with the default imaginary character, 'i', and the
     * default number format for both real and imaginary parts.
     */
    public ComplexFormat() {
        this(DEFAULT_IMAGINARY_CHARACTER, getDefaultNumberFormat());
    }

    /**
     * Create an instance with a custom number format for both real and
     * imaginary parts.
     * @param format the custom format for both real and imaginary parts.
     */
    public ComplexFormat(NumberFormat format) {
        this(DEFAULT_IMAGINARY_CHARACTER, format);
    }
    
    /**
     * Create an instance with a custom number format for the real part and a
     * custom number format for the imaginary part.
     * @param realFormat the custom format for the real part.
     * @param imaginaryFormat the custom format for the imaginary part.
     */
    public ComplexFormat(NumberFormat realFormat,
            NumberFormat imaginaryFormat) {
        this(DEFAULT_IMAGINARY_CHARACTER, realFormat, imaginaryFormat);
    }
    
    /**
     * Create an instance with a custom imaginary character, and the default
     * number format for both real and imaginary parts.
     * @param imaginaryCharacter The custom imaginary character.
     */
    public ComplexFormat(String imaginaryCharacter) {
        this(imaginaryCharacter, getDefaultNumberFormat());
    }
    
    /**
     * Create an instance with a custom imaginary character, and a custom number
     * format for both real and imaginary parts.
     * @param imaginaryCharacter The custom imaginary character.
     * @param format the custom format for both real and imaginary parts.
     */
    public ComplexFormat(String imaginaryCharacter, NumberFormat format) {
        this(imaginaryCharacter, format, (NumberFormat)format.clone());
    }
    
    /**
     * Create an instance with a custom imaginary character, a custom number
     * format for the real part, and a custom number format for the imaginary
     * part.
     * @param imaginaryCharacter The custom imaginary character.
     * @param realFormat the custom format for the real part.
     * @param imaginaryFormat the custom format for the imaginary part.
     */
    public ComplexFormat(String imaginaryCharacter, NumberFormat realFormat,
            NumberFormat imaginaryFormat) {
        super();
        setImaginaryCharacter(imaginaryCharacter);
        setImaginaryFormat(imaginaryFormat);
        setRealFormat(realFormat);
    }

    /**
     * This static method calls formatComplex() on a default instance of
     * ComplexFormat.
     *
     * @param c Complex object to format
     * @return A formatted number in the form "Re(c) + Im(c)i"
     */
    public static String formatComplex( Complex c ) {
        return getInstance().format( c );
    }
    
    /**
     * Formats a {@link Complex} object to produce a string.
     *
     * @param complex the object to format.
     * @param toAppendTo where the text is to be appended
     * @param pos On input: an alignment field, if desired. On output: the
     *            offsets of the alignment field
     * @return the value passed in as toAppendTo.
     */
    public StringBuffer format(Complex complex, StringBuffer toAppendTo,
            FieldPosition pos) {
        
        pos.setBeginIndex(0);
        pos.setEndIndex(0);

        // format real
        double re = complex.getReal();
        formatDouble(re, getRealFormat(), toAppendTo, pos);
        
        // format sign and imaginary
        double im = complex.getImaginary();
        if (im < 0.0) {
            toAppendTo.append(" - ");
            formatDouble(-im, getImaginaryFormat(), toAppendTo, pos);
            toAppendTo.append(getImaginaryCharacter());
        } else if (im > 0.0 || Double.isNaN(im)) {
            toAppendTo.append(" + ");
            formatDouble(im, getImaginaryFormat(), toAppendTo, pos);
            toAppendTo.append(getImaginaryCharacter());
        }
        
        return toAppendTo;
    }
    
    /**
     * Formats a object to produce a string.  <code>obj</code> must be either a 
     * {@link Complex} object or a {@link Number} object.  Any other type of
     * object will result in an {@link IllegalArgumentException} being thrown.
     *
     * @param obj the object to format.
     * @param toAppendTo where the text is to be appended
     * @param pos On input: an alignment field, if desired. On output: the
     *            offsets of the alignment field
     * @return the value passed in as toAppendTo.
     * @see java.text.Format#format(java.lang.Object, java.lang.StringBuffer, java.text.FieldPosition)
     * @throws IllegalArgumentException is <code>obj</code> is not a valid type.
     */
    public StringBuffer format(Object obj, StringBuffer toAppendTo,
            FieldPosition pos) {
        
        StringBuffer ret = null;
        
        if (obj instanceof Complex) {
            ret = format( (Complex)obj, toAppendTo, pos);
        } else if (obj instanceof Number) {
            ret = format( new Complex(((Number)obj).doubleValue(), 0.0),
                toAppendTo, pos);
        } else { 
            throw new IllegalArgumentException(
                "Cannot format given Object as a Date");
        }
        
        return ret;
    }

    /**
     * Formats a double value to produce a string.  In general, the value is
     * formatted using the formatting rules of <code>format</code>.  There are
     * three exceptions to this:
     * <ol>
     * <li>NaN is formatted as '(NaN)'</li>
     * <li>Positive infinity is formatted as '(Infinity)'</li>
     * <li>Negative infinity is formatted as '(-Infinity)'</li>
     * </ol>
     *
     * @param value the double to format.
     * @param format the format used.
     * @param toAppendTo where the text is to be appended
     * @param pos On input: an alignment field, if desired. On output: the
     *            offsets of the alignment field
     * @return the value passed in as toAppendTo.
     */
    private StringBuffer formatDouble(double value, NumberFormat format,
            StringBuffer toAppendTo, FieldPosition pos) {
        if( Double.isNaN(value) || Double.isInfinite(value) ) {
            toAppendTo.append('(');
            toAppendTo.append(value);
            toAppendTo.append(')');
        } else {
            format.format(value, toAppendTo, pos);
        }
        return toAppendTo;
    }
    
    /**
     * Get the set of locales for which complex formats are available.  This
     * is the same set as the {@link NumberFormat} set. 
     * @return available complex format locales.
     */
    public static Locale[] getAvailableLocales() {
        return NumberFormat.getAvailableLocales();
    }
    
    /**
     * Create a default number format.  The default number format is based on
     * {@link NumberFormat#getInstance()} with the only customizing is the
     * maximum number of fraction digits, which is set to 2.  
     * @return the default number format.
     */
    private static NumberFormat getDefaultNumberFormat() {
        return getDefaultNumberFormat(Locale.getDefault());
    }
    
    /**
     * Create a default number format.  The default number format is based on
     * {@link NumberFormat#getInstance(java.util.Locale)} with the only
     * customizing is the maximum number of fraction digits, which is set to 2.  
     * @param locale the specific locale used by the format.
     * @return the default number format specific to the given locale.
     */
    private static NumberFormat getDefaultNumberFormat(Locale locale) {
        NumberFormat nf = NumberFormat.getInstance(locale);
        nf.setMaximumFractionDigits(2);
        return nf;
    }
    
    /**
     * Access the imaginaryCharacter.
     * @return the imaginaryCharacter.
     */
    public String getImaginaryCharacter() {
        return imaginaryCharacter;
    }
    
    /**
     * Access the imaginaryFormat.
     * @return the imaginaryFormat.
     */
    public NumberFormat getImaginaryFormat() {
        return imaginaryFormat;
    }
    
    /**
     * Returns the default complex format for the current locale.
     * @return the default complex format.
     */
    public static ComplexFormat getInstance() {
        return getInstance(Locale.getDefault());
    }
    
    /**
     * Returns the default complex format for the given locale.
     * @param locale the specific locale used by the format.
     * @return the complex format specific to the given locale.
     */
    public static ComplexFormat getInstance(Locale locale) {
        NumberFormat f = getDefaultNumberFormat(locale);
        return new ComplexFormat(f);
    }
    
    /**
     * Access the realFormat.
     * @return the realFormat.
     */
    public NumberFormat getRealFormat() {
        return realFormat;
    }

    /**
     * Parses a string to produce a {@link Complex} object.
     *
     * @param source the string to parse
     * @return the parsed {@link Complex} object.
     * @exception ParseException if the beginning of the specified string
     *            cannot be parsed.
     */
    public Complex parse(String source) throws ParseException {
        ParsePosition parsePosition = new ParsePosition(0);
        Complex result = parse(source, parsePosition);
        if (parsePosition.getIndex() == 0) {
            throw new ParseException("Unparseable complex number: \"" + source +
                "\"", parsePosition.getErrorIndex());
        }
        return result;
    }
    
    /**
     * Parses a string to produce a {@link Complex} object.
     *
     * @param source the string to parse
     * @param pos input/ouput parsing parameter.
     * @return the parsed {@link Complex} object.
     */
    public Complex parse(String source, ParsePosition pos) {
        int initialIndex = pos.getIndex();

        // parse whitespace
        parseAndIgnoreWhitespace(source, pos);

        // parse real
        Number re = parseNumber(source, getRealFormat(), pos);
        if (re == null) {
            // invalid real number
            // set index back to initial, error index should already be set
            // character examined.
            pos.setIndex(initialIndex);
            return null;
        }

        // parse sign
        int startIndex = pos.getIndex();
        char c = parseNextCharacter(source, pos);
        int sign = 0;
        switch (c) {
        case 0 :
            // no sign
            // return real only complex number
            return new Complex(re.doubleValue(), 0.0);
        case '-' :
            sign = -1;
            break;
        case '+' :
            sign = 1;
            break;
        default :
            // invalid sign
            // set index back to initial, error index should be the last
            // character examined.
            pos.setIndex(initialIndex);
            pos.setErrorIndex(startIndex);
            return null;
        }

        // parse whitespace
        parseAndIgnoreWhitespace(source, pos);

        // parse imaginary
        Number im = parseNumber(source, getRealFormat(), pos);
        if (im == null) {
            // invalid imaginary number
            // set index back to initial, error index should already be set
            // character examined.
            pos.setIndex(initialIndex);
            return null;
        }

        // parse imaginary character
        int n = getImaginaryCharacter().length();
        startIndex = pos.getIndex();
        int endIndex = startIndex + n;
        if ((startIndex >= source.length()) ||
            (endIndex > source.length()) ||
            source.substring(startIndex, endIndex).compareTo(
            getImaginaryCharacter()) != 0) {
            // set index back to initial, error index should be the start index
            // character examined.
            pos.setIndex(initialIndex);
            pos.setErrorIndex(startIndex);
            return null;
        }
        pos.setIndex(endIndex);

        return new Complex(re.doubleValue(), im.doubleValue() * sign);
    }
     
    /**
     * Parses <code>source</code> until a non-whitespace character is found.
     *
     * @param source the string to parse
     * @param pos input/ouput parsing parameter.  On output, <code>pos</code>
     *        holds the index of the next non-whitespace character.
     */
    private void parseAndIgnoreWhitespace(String source, ParsePosition pos) {
        parseNextCharacter(source, pos);
        pos.setIndex(pos.getIndex() - 1);
    }

    /**
     * Parses <code>source</code> until a non-whitespace character is found.
     *
     * @param source the string to parse
     * @param pos input/ouput parsing parameter.
     * @return the first non-whitespace character.
     */
    private char parseNextCharacter(String source, ParsePosition pos) {
         int index = pos.getIndex();
         int n = source.length();
         char ret = 0;

         if (index < n) {
             char c;
             do {
                 c = source.charAt(index++);
             } while (Character.isWhitespace(c) && index < n);
             pos.setIndex(index);
         
             if (index < n) {
                 ret = c;
             }
         }
         
         return ret;
    }
    
    /**
     * Parses <code>source</code> for a special double values.  These values
     * include Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY.
     *
     * @param source the string to parse
     * @param value the special value to parse.
     * @param pos input/ouput parsing parameter.
     * @return the special number.
     */
    private Number parseNumber(String source, double value, ParsePosition pos) {
        Number ret = null;
        
        StringBuffer sb = new StringBuffer();
        sb.append('(');
        sb.append(value);
        sb.append(')');
        
        int n = sb.length();
        int startIndex = pos.getIndex();
        int endIndex = startIndex + n;
        if (endIndex < source.length()) {
            if (source.substring(startIndex, endIndex).compareTo(sb.toString()) == 0) {
                ret = new Double(value);
                pos.setIndex(endIndex);
            }
        }
        
        return ret;
    }
    
    /**
     * Parses <code>source</code> for a number.  This method can parse normal,
     * numeric values as well as special values.  These special values include
     * Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY.
     *
     * @param source the string to parse
     * @param format the number format used to parse normal, numeric values.
     * @param pos input/ouput parsing parameter.
     * @return the parsed number.
     */
    private Number parseNumber(String source, NumberFormat format, ParsePosition pos) {
        int startIndex = pos.getIndex();
        Number number = format.parse(source, pos);
        int endIndex = pos.getIndex();
        
        // check for error parsing number
        if (startIndex == endIndex) {
            // try parsing special numbers
            double[] special = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
            for (int i = 0; i < special.length; ++i) {
                number = parseNumber(source, special[i], pos);
                if (number != null) {
                    break;
                }
            }
        }
        
        return number;
    }

    /**
     * Parses a string to produce a object.
     *
     * @param source the string to parse
     * @param pos input/ouput parsing parameter.
     * @return the parsed object.
     * @see java.text.Format#parseObject(java.lang.String, java.text.ParsePosition)
     */
    public Object parseObject(String source, ParsePosition pos) {
        return parse(source, pos);
    }
    /**
     * Modify the imaginaryCharacter.
     * @param imaginaryCharacter The new imaginaryCharacter value.
     * @throws IllegalArgumentException if <code>imaginaryCharacter</code> is
     *         <code>null</code> or an empty string.
     */
    public void setImaginaryCharacter(String imaginaryCharacter) {
        if (imaginaryCharacter == null || imaginaryCharacter.length() == 0) {
            throw new IllegalArgumentException(
                "imaginaryCharacter must be a non-empty string.");
        }
        this.imaginaryCharacter = imaginaryCharacter;
    }
    
    /**
     * Modify the imaginaryFormat.
     * @param imaginaryFormat The new imaginaryFormat value.
     * @throws IllegalArgumentException if <code>imaginaryFormat</code> is
     *         <code>null</code>.
     */
    public void setImaginaryFormat(NumberFormat imaginaryFormat) {
        if (imaginaryFormat == null) {
            throw new IllegalArgumentException(
                "imaginaryFormat can not be null.");
        }
        this.imaginaryFormat = imaginaryFormat;
    }
    
    /**
     * Modify the realFormat.
     * @param realFormat The new realFormat value.
     * @throws IllegalArgumentException if <code>realFormat</code> is
     *         <code>null</code>.
     */
    public void setRealFormat(NumberFormat realFormat) {
        if (realFormat == null) {
            throw new IllegalArgumentException(
                "realFormat can not be null.");
        }
        this.realFormat = realFormat;
    }
}
