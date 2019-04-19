/*
 * Copyright (c) 2006, John Kristian
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *      *   Redistributions of source code must retain the above copyright
 *          notice, this list of conditions and the following disclaimer.
 *
 *      *   Redistributions in binary form must reproduce the above copyright
 *          notice, this list of conditions and the following disclaimer in the
 *          documentation and/or other materials provided with the distribution.
 *
 *      *   Neither the name of StAX-Utils nor the names of its contributors
 *          may be used to endorse or promote products derived from this
 *          software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
package com.github.gumtreediff.io;

/**
 * Characters that represent line breaks and indentation. These are represented
 * as String-valued JavaBean properties.
 */
public interface Indentation {

    /** Two spaces; the default indentation. */
    String DEFAULT_INDENT = "  ";

    /**
     * Set the characters used for one level of indentation. The default is
     * {@link #DEFAULT_INDENT}. "\t" is a popular alternative.
     */
    void setIndent(String indent);

    /** The characters used for one level of indentation. */
    String getIndent();

    /**
     * "\n"; the normalized representation of end-of-line in <a
     * href="http://www.w3.org/TR/xml11/#sec-line-ends">XML</a>.
     */
    String NORMAL_END_OF_LINE = "\n";

    /**
     * Set the characters that introduce a new line. The default is
     * {@link #NORMAL_END_OF_LINE}.
     * {@link IndentingXMLStreamWriter#getLineSeparator}() is a popular
     * alternative.
     */
    void setNewLine(String newLine);

    /** The characters that introduce a new line. */
    String getNewLine();

}
