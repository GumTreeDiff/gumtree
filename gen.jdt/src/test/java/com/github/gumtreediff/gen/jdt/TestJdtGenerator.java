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

package com.github.gumtreediff.gen.jdt;

import java.io.IOException;

import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.Test;

import com.github.gumtreediff.tree.ITree;
import static org.junit.Assert.*;

public class TestJdtGenerator {

    @Test
    public void testSimpleSyntax() throws IOException {
        String input = "public class Foo { public int foo; }";
        ITree tree = new JdtTreeGenerator().generateFromString(input).getRoot();
        assertEquals(15, tree.getType());
        assertEquals(9, tree.getSize());
    }

    @Test
    public void testJava5Syntax() throws IOException {
        String input = "public class Foo<A> { public List<A> foo; public void foo() "
                + "{ for (A f : foo) { System.out.println(f); } } }";
        ITree tree = new JdtTreeGenerator().generateFromString(input).getRoot();
        assertEquals(15, tree.getType());
        assertEquals(34, tree.getSize());
    }

    @Test
    public void testMethodInvocation() throws IOException {
        String input = "class Main {\n"
                + "    public static void foo() {\n"
                + "        a(b);\n"
                + "    }\n"
                + "}\n";
        TreeContext ctx = new JdtTreeGenerator().generateFromString(input);
        String o1 = TreeIoUtils.toLisp(ctx).toString();

        input = "class Main {\n"
                + "    public static void foo() {\n"
                + "        a.b();\n"
                + "    }\n"
                + "}";
        ctx = new JdtTreeGenerator().generateFromString(input);
        String o2 = TreeIoUtils.toLisp(ctx).toString();
        assertNotEquals(o1, o2);
    }

    @Test
    public void testJava8Syntax() throws IOException {
        String input = "public class Foo { public void foo(){ new ArrayList<Object>().stream().forEach(a -> {}); } }";
        ITree tree = new JdtTreeGenerator().generateFromString(input).getRoot();
        assertEquals(15, tree.getType());
        assertEquals(27, tree.getSize());
    }

    @Test(expected = SyntaxException.class)
    public void badSyntax() throws IOException {
        String input = "public clas Foo {}";
        TreeContext ct = new JdtTreeGenerator().generateFromString(input);
    }

}
