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

import com.github.gumtreediff.tree.Tree;

public class TestJsGenerator {
    @Test
    public void testStatement() throws IOException {
        String input = "console.log(\"Hello world!\");";
        Tree tree = new RhinoTreeGenerator().generateFrom().string(input).getRoot();
        assertEquals(7, tree.getMetrics().size);
    }

    @Test
    public void testLambda() throws IOException {
        String input = "let f = (foo, bar) => foo + bar;";
        Tree tree = new RhinoTreeGenerator().generateFrom().string(input).getRoot();
        assertEquals(12, tree.getMetrics().size);
    }

    @Test
    public void testComment() throws IOException {
        String input = "console.log(\"Hello world!\"); /* with comment */";
        Tree tree = new RhinoTreeGenerator().generateFrom().string(input).getRoot();
        assertEquals(8, tree.getMetrics().size);
    }

    @Test
    public void badSyntax() {
        String input = "function foo((bar) {}";
        assertThrows(SyntaxException.class, () -> {
            new RhinoTreeGenerator().generateFrom().string(input);
        });
    }
}
