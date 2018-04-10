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
 * Copyright 2018 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen.javaparser;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import com.github.gumtreediff.tree.TreeContext;
import org.junit.Test;

import com.github.gumtreediff.tree.ITree;

public class TestJavaParserGenerator {
    @Test
    public void testSimpleSyntax() throws IOException {
        String input = "public class Foo { public int foo; }";
        ITree tree = new JavaParserGenerator().generateFromString(input).getRoot();
        assertEquals(-1795686804, tree.getType());
        assertEquals(7, tree.getSize());
    }

    @Test
    public void testJava5Syntax() throws IOException {
        String input = "public class Foo<A> { public List<A> foo; public void foo() "
                + "{ for (A f : foo) { System.out.println(f); } } }";
        TreeContext context = new JavaParserGenerator().generateFromString(input);
        ITree tree = context.getRoot();

        System.out.println(tree.toTreeString());
        assertEquals(-1795686804, tree.getType());
        assertEquals(34, tree.getSize());
    }

    @Test
    public void testJava8Syntax() throws IOException {
        String input = "public class Foo { public void foo(){ new ArrayList<Object>().stream().forEach(a -> {}); } }";
        ITree tree = new JavaParserGenerator().generateFromString(input).getRoot();
        assertEquals(-1795686804, tree.getType());
        assertEquals(21, tree.getSize());
    }

}
