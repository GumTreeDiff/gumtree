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

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.optimal.TopDownMatcher;
import com.github.gumtreediff.tree.Tree;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TreeSitterJavaTreeGeneratorTest {
    @Test
    public void testClassToInterface() throws IOException {
        Tree src = new JavaTreeSitterTreeGenerator().generateFrom().string("class Foo {}").getRoot();
        Tree dst = new JavaTreeSitterTreeGenerator().generateFrom().string("interface Foo {}").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
    }

    @Test
    public void testClassToEnum() throws IOException {
        Tree src = new JavaTreeSitterTreeGenerator().generateFrom().string("class Foo {}").getRoot();
        Tree dst = new JavaTreeSitterTreeGenerator().generateFrom().string("enum Foo {}").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
    }

    @Test
    public void testArithmeticOperatorChange() throws IOException {
        Tree src = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a + b; } }").getRoot();
        Tree dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a - b; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
        dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a * b; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
        dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a / b; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
    }

    @Test
    public void testImportChange() throws IOException {
        Tree src = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "import foo;").getRoot();
        Tree dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "import bar.foo;").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
    }

    @Test
    public void testVisibilityChange() throws IOException {
        Tree src = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { public void foo() {} }").getRoot();
        Tree dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { private void foo() {} }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
        dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { protected void foo() {} }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
    }

    @Test
    public void testLogicalOperatorChange() throws IOException {
        Tree src = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a && b; } }").getRoot();
        Tree dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a || b; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
    }

    @Test
    public void testBitwiseOperatorChange() throws IOException {
        Tree src = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "public class Foo { void foo() { a & b; } }").getRoot();
        Tree dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "public class Foo { void foo() { a | b; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
        dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "public class Foo { void foo() { a ^ b; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
    }

    @Test
    public void testncrementOperatorChange() throws IOException {
        Tree src = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "public class Foo { void foo() { a++; } }").getRoot();
        Tree dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "public class Foo { void foo() { a--; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
    }

    @Test
    public void testAffectationOperatorChange() throws IOException {
        Tree src = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a = b; } }").getRoot();
        Tree dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a += b; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
        dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a -= b; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
        dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a *= b; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
        dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a /= b; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
        dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a |= b; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
        dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a ^= b; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
    }

    @Test
    public void testComparisonOperatorChange() throws IOException {
        Tree src = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a == b; } }").getRoot();
        Tree dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a != b; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
        dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a >= b; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
        dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a > b; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
        dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a <= b; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
        dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { a < b; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
    }

    @Test
    public void testTypeChange() throws IOException {
        Tree src = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { Foo a; } }").getRoot();
        Tree dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { Foo[] a; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
        dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { Foo<Bar> a; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
        dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { foo.Foo a; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
        dst = new JavaTreeSitterTreeGenerator().generateFrom().string(
                "class Foo { void foo() { int a; } }").getRoot();
        assertTrue(onlyOneUpdate(src, dst));
    }

    public static boolean onlyOneUpdate(Tree src, Tree dst) {
        TopDownMatcher m = new TopDownMatcher();
        MappingStore ms = m.match(src, dst);
        EditScriptGenerator g = new SimplifiedChawatheScriptGenerator();
        EditScript s = g.computeActions(ms);
        return s.size() == 1 && s.get(0) instanceof Update;
    }
}
