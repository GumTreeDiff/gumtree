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
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.gen.js;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.tree.MetricProviderFactory;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TreeMetricsProvider;

import com.github.gumtreediff.tree.ITree;

public class TestJsGenerator {
    @Test
    public void testStatement() throws IOException {
        String input = "console.log(\"Hello world!\");";
        ITree tree = new RhinoTreeGenerator().generateFrom().string(input).getRoot();
        TreeMetricsProvider m = MetricProviderFactory.computeTreeMetrics(tree);
        assertEquals(7, m.get(tree).size);
    }

    @Test
    public void testComment() throws IOException {
        String input = "console.log(\"Hello world!\"); /* with comment */";
        ITree tree = new RhinoTreeGenerator().generateFrom().string(input).getRoot();
        TreeMetricsProvider m = MetricProviderFactory.computeTreeMetrics(tree);
        assertEquals(8, m.get(tree).size);
    }

    @Test
    public void testComplexFile() throws IOException {
        ITree tree = new RhinoTreeGenerator().generateFrom().charset("UTF-8")
                .stream(getClass().getResourceAsStream("/sample.js")).getRoot();
        TreeMetricsProvider m = MetricProviderFactory.computeTreeMetrics(tree);
        assertEquals(402, m.get(tree).size);
    }

    @Test
    public void badSyntax() throws IOException {
        String input = "function foo((bar) {}";
        assertThrows(SyntaxException.class, () -> {
            TreeContext ct = new RhinoTreeGenerator().generateFrom().string(input);
        });
    }
}
