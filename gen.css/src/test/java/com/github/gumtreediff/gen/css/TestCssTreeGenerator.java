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
 * Copyright 2016 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen.css;

import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.MetricProviderFactory;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TreeMetricsProvider;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertEquals;

public class TestCssTreeGenerator {

    @Test
    public void testSimple() throws Exception {
        Reader r = new StringReader("@import url(\"bluish.css\") projection, tv;\n"
                + "body {\n"
                + "\tfont-size: 11pt;\n"
                + "}\n"
                + "ul li {\n"
                + "\tbackground-color: black;\n"
                + "}");
        TreeContext ctx = new CssTreeGenerator().generateFrom().reader(r);
        ITree tree = ctx.getRoot();
        TreeMetricsProvider m = MetricProviderFactory.computeTreeMetrics(tree);
        assertEquals(10, m.get(tree).size);
    }

    @Test(expected = SyntaxException.class)
    public void badSyntax() throws IOException {
        String input = ".foo \"toto {\nfont-size: 11pt;\n}";
        TreeContext ct = new CssTreeGenerator().generateFrom().string(input);
    }
}
