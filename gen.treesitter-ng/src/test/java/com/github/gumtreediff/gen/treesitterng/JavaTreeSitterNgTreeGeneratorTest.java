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
package com.github.gumtreediff.gen.treesitterng;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaTreeSitterNgTreeGeneratorTest {
    private final JavaTreeSitterNgTreeGenerator generator = new JavaTreeSitterNgTreeGenerator();

    @Test
    public void testHelloWorld() throws IOException {
        TreeContext src = generator.generateFrom().string("class HelloWorld {\n"
                + "    public static void main(String[] args) {\n"
                + "        System.out.println(\"Hello World!\"); \n"
                + "    }\n"
                + "}");
        assertEquals(27, src.getRoot().getMetrics().size);
    }

    @Test
    public void testCommentLine() throws IOException {
        TreeContext src = generator.generateFrom().file("testData/java/commentLine/src/Test.java");
        TreeContext dst = generator.generateFrom().file("testData/java/commentLine/dst/Test.java");
        assertTrue(TreeSitterNgTestUtils.onlyOneUpdate(src.getRoot(), dst.getRoot()));
    }

    @Test
    public void testUnicodeInString() throws IOException {
        TreeContext src = generator.generateFrom().file("testData/java/unicodeInString/src/Test.java");
        TreeContext dst = generator.generateFrom().file("testData/java/unicodeInString/dst/Test.java");
        assertTrue(TreeSitterNgTestUtils.onlyOneUpdate(src.getRoot(), dst.getRoot()));
    }

    @Test
    public void testUnicodeInComment() throws IOException {
        TreeContext src = generator.generateFrom().file("testData/java/unicodeInComment/src/Test.java");
        TreeContext dst = generator.generateFrom().file("testData/java/unicodeInComment/dst/Test.java");
        assertTrue(TreeSitterNgTestUtils.onlyOneUpdate(src.getRoot(), dst.getRoot()));
    }

    @Test
    public void testAffectationOperatorChange() throws IOException {
        Tree src = generator.generateFrom().string(
                "class Foo { void foo() { a = b; } }").getRoot();
        Tree dst = generator.generateFrom().string(
                "class Foo { void foo() { a += b; } }").getRoot();
        assertTrue(TreeSitterNgTestUtils.onlyOneUpdate(src, dst));
        dst = generator.generateFrom().string(
                "class Foo { void foo() { a -= b; } }").getRoot();
        assertTrue(TreeSitterNgTestUtils.onlyOneUpdate(src, dst));
        dst = generator.generateFrom().string(
                "class Foo { void foo() { a *= b; } }").getRoot();
        assertTrue(TreeSitterNgTestUtils.onlyOneUpdate(src, dst));
        dst = generator.generateFrom().string(
                "class Foo { void foo() { a /= b; } }").getRoot();
        assertTrue(TreeSitterNgTestUtils.onlyOneUpdate(src, dst));
        dst = generator.generateFrom().string(
                "class Foo { void foo() { a |= b; } }").getRoot();
        assertTrue(TreeSitterNgTestUtils.onlyOneUpdate(src, dst));
        dst = generator.generateFrom().string(
                "class Foo { void foo() { a ^= b; } }").getRoot();
        assertTrue(TreeSitterNgTestUtils.onlyOneUpdate(src, dst));
    }
    
}