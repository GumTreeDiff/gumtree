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

package com.github.gumtreediff.gen.python;

import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TestPythonTreeGenerator {

    @Test
    public void testSimple() throws IOException {
        String input = "import sys\nimport json as json\n";
        TreeContext ctx = new PythonTreeGenerator().generateFrom().string(input);
        Tree t = ctx.getRoot();
        assertEquals(9, t.getMetrics().size);
    }

    @Test
    public void testBadSyntax() throws IOException {
        String input = "impot sys";
        assertThrows(SyntaxException.class, () -> {
            new PythonTreeGenerator().generateFrom().string(input);
        });
    }

}
