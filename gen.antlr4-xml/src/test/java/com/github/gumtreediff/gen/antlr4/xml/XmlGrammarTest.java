/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2017 Svante Schubert <svante.schubert gmail com>
 */
package com.github.gumtreediff.gen.antlr4.xml;

import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.gen.antlr4.TreeComparisonBase;

/**
 * The given test file pairs are being used as input for the tree generator. All
 * serializers are being used is used to dump the created graphs. In addition,
 * an edit script showing the difference of the pair is created. Finally, the
 * new output files from '/build/created-test-files' are being compared with
 * references from 'src\test\resources\references'.
 */
public class XmlGrammarTest extends TreeComparisonBase {

    /**
     * <b>IMPORTANT</b>
     * When adding new test pairs to the array, the new test output from the
     * output folder: '/build/created-test-files' needs to be copied manually to
     * the reference folder: 'src\test\resources\references
     */
    private static final String[][] testCouplesXML = new String[][]{
        {"books1.xml", "books2.xml"},
        {"books1.xml", "books1b.xml"},
        {"books1.xml", "books1c.xml"},
        {"books1b.xml", "books1c.xml"},
        {"web.xml", "web1.xml"}
    };

    public XmlGrammarTest() {
        super(testCouplesXML);
    }

    @Override
    protected TreeGenerator getTreeGenerator() {
        return new XmlTreeGenerator();
    }
}
