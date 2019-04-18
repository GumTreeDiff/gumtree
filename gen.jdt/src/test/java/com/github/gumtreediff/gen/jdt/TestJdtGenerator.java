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
import com.github.gumtreediff.tree.*;
import org.eclipse.jdt.core.dom.ASTNode;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestJdtGenerator {

    private static final Symbol COMPILATION_UNIT = AbstractJdtVisitor.nodeAsSymbol(ASTNode.COMPILATION_UNIT);

    @Test
    public void testSimpleSyntax() throws IOException {
        String input = "public class Foo { public int foo; }";
        ITree tree = new JdtTreeGenerator().generateFrom().string(input).getRoot();
        assertEquals(COMPILATION_UNIT, tree.getType());
        TreeMetricsProvider m = MetricProviderFactory.computeTreeMetrics(tree);
        assertEquals(10, m.get(tree).size);
    }

    @Test
    public void testJava5Syntax() throws IOException {
        String input = "public class Foo<A> { public List<A> foo; public void foo() "
                + "{ for (A f : foo) { System.out.println(f); } } }";
        ITree tree = new JdtTreeGenerator().generateFrom().string(input).getRoot();
        assertEquals(COMPILATION_UNIT, tree.getType());
        TreeMetricsProvider m = MetricProviderFactory.computeTreeMetrics(tree);
        assertEquals(35, m.get(tree).size);
    }

    @Test
    public void testMethodInvocation() throws IOException {
        String input = "class Main {\n"
                + "    public static void foo() {\n"
                + "        a(b);\n"
                + "    }\n"
                + "}\n";
        TreeContext ctx = new JdtTreeGenerator().generateFrom().string(input);
        String o1 = TreeIoUtils.toLisp(ctx).toString();

        input = "class Main {\n"
                + "    public static void foo() {\n"
                + "        a.b();\n"
                + "    }\n"
                + "}";
        ctx = new JdtTreeGenerator().generateFrom().string(input);
        String o2 = TreeIoUtils.toLisp(ctx).toString();
        assertNotEquals(o1, o2);
    }

    @Test
    public void testJava8Syntax() throws IOException {
        String input = "public class Foo { public void foo(){ new ArrayList<Object>().stream().forEach(a -> {}); } }";
        ITree tree = new JdtTreeGenerator().generateFrom().string(input).getRoot();
        assertEquals(COMPILATION_UNIT, tree.getType());
        TreeMetricsProvider m = MetricProviderFactory.computeTreeMetrics(tree);
        assertEquals(28, m.get(tree).size);
    }

    @Test(expected = SyntaxException.class)
    public void badSyntax() throws IOException {
        String input = "public clas Foo {}";
        TreeContext ct = new JdtTreeGenerator().generateFrom().string(input);
    }

    @Test
    public void testTypeDefinition() throws IOException {
        String input1 = "public class Foo {}";
        String input2 = "public interface Foo {}";
        TreeContext ct1 = new JdtTreeGenerator().generateFrom().string(input1);
        TreeMetricsProvider m1 = MetricProviderFactory.computeTreeMetrics(ct1.getRoot());
        TreeContext ct2 = new JdtTreeGenerator().generateFrom().string(input2);
        TreeMetricsProvider m2 = MetricProviderFactory.computeTreeMetrics(ct2.getRoot());
        assertNotEquals(m1.get(ct1.getRoot()).hash, m2.get(ct2.getRoot()).hash);
    }

    @Test
    public void testInfixOperator() throws IOException {
        String input = "class Foo { int i = 3 + 3}";
        TreeContext ct = new JdtTreeGenerator().generateFrom().string(input);
        System.out.println(ct.getRoot().toTreeString());
    }

    @Test
    public void testAssignment() throws IOException {
        String input = "class Foo { void foo() { s.foo  = 12; } }";
        TreeContext ct = new JdtTreeGenerator().generateFrom().string(input);
        System.out.println(ct.getRoot().toTreeString());
    }

    @Test
    public void testPrefixExpression() throws IOException {
        String input = "class Foo { void foo() { ++s.i; } }";
        TreeContext ct = new JdtTreeGenerator().generateFrom().string(input);
        System.out.println(ct.getRoot().toTreeString());
    }

    @Test
    public void testPostfixExpression() throws IOException {
        String input = "class Foo { void foo() { s.i++; } }";
        TreeContext ct = new JdtTreeGenerator().generateFrom().string(input);
        System.out.println(ct.getRoot().toTreeString());
    }

    @Test
    public void testArrayCreation() throws IOException {
        String input1 = "class Foo { void foo() { int[][] t = new int[12][]; } }";
        TreeContext ct1 = new JdtTreeGenerator().generateFrom().string(input1);
        System.out.println(ct1.getRoot().toTreeString());

        String input2 = "class Foo { void foo() { int[][] t = new int[][12]; } }";
        TreeContext ct2 = new JdtTreeGenerator().generateFrom().string(input2);
        System.out.println(ct2.getRoot().toTreeString());
    }

}
