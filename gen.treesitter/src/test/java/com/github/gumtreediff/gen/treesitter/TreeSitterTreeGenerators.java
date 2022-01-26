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

package com.github.gumtreediff.gen.treesitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TreeSitterTreeGenerators {
    @Test
    public void testR() throws IOException {
        String input = "print(paste(\"How\",\"are\",\"you?\"))";
        TreeContext ctx = new RTreeSitterTreeGenerator().generateFrom().string(input);
        Tree t = ctx.getRoot();
        assertEquals(18, t.getMetrics().size);
    }

    @Test
    public void testJs() throws IOException {
        String input = "let f = (a, b) => a + b";
        TreeContext ctx = new JavaScriptTreeSitterTreeGenerator().generateFrom().string(input);
        Tree t = ctx.getRoot();
        assertEquals(18, t.getMetrics().size);
    }

    @Test
    public void testTs() throws IOException {
        String input = "let message: string = 'Hello, World!';";
        TreeContext ctx = new TypeScriptTreeSitterTreeGenerator().generateFrom().string(input);
        Tree t = ctx.getRoot();
        assertEquals(15, t.getMetrics().size);
    }

    @Test
    public void testJava() throws IOException {
        String input = "public class Foo { int foo(int a, int b) { return a + b; } }";
        TreeContext ctx = new JavaTreeSitterTreeGenerator().generateFrom().string(input);
        Tree t = ctx.getRoot();
        assertEquals(35, t.getMetrics().size);
    }

    @Test
    public void testOcaml() throws IOException {
        String input = "print_string \"Hello world!\\n\";;\n";
        TreeContext ctx = new OcamlTreeSitterTreeGenerator().generateFrom().string(input);
        Tree t = ctx.getRoot();
        assertEquals(11, t.getMetrics().size);
    }

    @Test
    public void testPython() throws IOException {
        String input = "l = [1, 2, 3]";
        TreeContext ctx = new PythonTreeSitterTreeGenerator().generateFrom().string(input);
        Tree t = ctx.getRoot();
        assertEquals(13, t.getMetrics().size);
    }
}
