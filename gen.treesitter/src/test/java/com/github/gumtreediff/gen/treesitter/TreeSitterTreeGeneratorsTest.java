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
 * along with GumTree. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2022 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen.treesitter;

import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.optimal.TopDownMatcher;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TreeSitterTreeGeneratorsTest {
    @Test
    public void testC() throws IOException {
        String input = "int main() {\n"
                + "\treturn 0;\n"
                + "}";
        TreeContext ctx = new CTreeSitterTreeGenerator().generateFrom().string(input);
        Tree t = ctx.getRoot();
        assertEquals(15, t.getMetrics().size);
    }

    @Test
    public void testCError() throws IOException {
        String input = "int main(";
        assertThrows(SyntaxException.class, () -> {
            new CTreeSitterTreeGenerator().generateFrom().string(input);
        });
    }

    @Test
    public void testCSharp() throws IOException {
        String input = "using System;\n"
                + "\n"
                + "namespace HelloWorld\n"
                + "{\n"
                + "  class Program\n"
                + "  {\n"
                + "    static void Main(string[] args)\n"
                + "    {\n"
                + "      Console.WriteLine(\"Hello World!\");\n"
                + "    }\n"
                + "  }\n"
                + "}";
        TreeContext ctx = new CSharpTreeSitterTreeGenerator().generateFrom().string(input);
        Tree t = ctx.getRoot();
        assertEquals(49, t.getMetrics().size);
    }

    @Test
    public void testCSharpError() throws IOException {
        String input = "using System\n"
                + "\n"
                + "namespace HelloWorld\n"
                + "{\n"
                + "  class Program\n"
                + "  {\n"
                + "    static void Main(string[] args)\n"
                + "    {\n"
                + "      Console.WriteLine(\"Hello World!\");\n"
                + "    }\n"
                + "  }\n"
                + "}";
        assertThrows(SyntaxException.class, () -> {
            new CSharpTreeSitterTreeGenerator().generateFrom().string(input);
        });
    }

    @Test
    public void testR() throws IOException {
        String input = "print(paste(\"How\",\"are\",\"you?\"))";
        TreeContext ctx = new RTreeSitterTreeGenerator().generateFrom().string(input);
        Tree t = ctx.getRoot();
        assertEquals(22, t.getMetrics().size);
    }

    @Test
    public void testRError() throws IOException {
        String input = "print)";
        assertThrows(SyntaxException.class, () -> {
            new RTreeSitterTreeGenerator().generateFrom().string(input);
        });
    }

    @Test
    public void testJs() throws IOException {
        String input = "let f = (a, b) => a + b";
        TreeContext ctx = new JavaScriptTreeSitterTreeGenerator().generateFrom().string(input);
        Tree t = ctx.getRoot();
        assertEquals(18, t.getMetrics().size);
    }

    @Test
    public void testJsError() {
        String input = "function foo((bar) {}";
        assertThrows(SyntaxException.class, () -> {
            new JavaScriptTreeSitterTreeGenerator().generateFrom().string(input);
        });
    }

    @Test
    public void testTs() throws IOException {
        String input = "let message: string = 'Hello, World!';";
        TreeContext ctx = new TypeScriptTreeSitterTreeGenerator().generateFrom().string(input);
        Tree t = ctx.getRoot();
        assertEquals(15, t.getMetrics().size);
    }

    @Test
    public void testTsError() {
        String input = "function foo((bar) {}";
        assertThrows(SyntaxException.class, () -> {
            new TypeScriptTreeSitterTreeGenerator().generateFrom().string(input);
        });
    }

    @Test
    public void testJava() throws IOException {
        String input = "public class Foo { int foo(int a, int b) { return a + b; } }";
        TreeContext ctx = new JavaTreeSitterTreeGenerator().generateFrom().string(input);
        Tree t = ctx.getRoot();
        assertEquals(23, t.getMetrics().size);
    }

    @Test
    public void testJavaError() throws IOException {
        String input = "public clazz Foo {";
        assertThrows(SyntaxException.class, () -> {
            new JavaTreeSitterTreeGenerator().generateFrom().string(input);
        });
    }

    @Test
    public void testOcaml() throws IOException {
        String input = "print_string \"Hello world!\\n\";;\n";
        TreeContext ctx = new OcamlTreeSitterTreeGenerator().generateFrom().string(input);
        Tree t = ctx.getRoot();
        assertEquals(7, t.getMetrics().size);
    }

    @Test
    public void testOcamlError() throws IOException {
        String input = "let return x x";
        assertThrows(SyntaxException.class, () -> {
            new OcamlTreeSitterTreeGenerator().generateFrom().string(input);
        });
    }

    @Test
    public void testPython() throws IOException {
        String input = "l = [1, 2, 3]";
        TreeContext ctx = new PythonTreeSitterTreeGenerator().generateFrom().string(input);
        Tree t = ctx.getRoot();
        assertEquals(13, t.getMetrics().size);
    }

    @Test
    public void testPythonError() throws IOException {
        String input = "l = [1, 2, 3";
        assertThrows(SyntaxException.class, () -> {
            new PythonTreeSitterTreeGenerator().generateFrom().string(input);
        });
    }
}
