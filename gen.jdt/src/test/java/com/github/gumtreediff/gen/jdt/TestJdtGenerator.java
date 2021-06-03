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
import com.github.gumtreediff.tree.*;
import org.eclipse.jdt.core.dom.ASTNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestJdtGenerator {
    private static final Type COMPILATION_UNIT = AbstractJdtVisitor.nodeAsSymbol(ASTNode.COMPILATION_UNIT);

    @Test
    public void testSimpleSyntax() throws IOException {
        String input = "public class Foo { public int foo; }";
        Tree tree = new JdtTreeGenerator().generateFrom().string(input).getRoot();
        assertEquals(COMPILATION_UNIT, tree.getType());
        assertEquals(10, tree.getMetrics().size);
    }

    @Test
    public void testJava5Syntax() throws IOException {
        String input = "public class Foo<A> { public List<A> foo; public void foo() "
                + "{ for (A f : foo) { System.out.println(f); } } }";
        Tree tree = new JdtTreeGenerator().generateFrom().string(input).getRoot();
        assertEquals(COMPILATION_UNIT, tree.getType());
        assertEquals(35, tree.getMetrics().size);
    }

    @Test
    public void testMethodInvocation() throws IOException {
        String leftInput = "class Main {\n"
                + "    public static void foo() {\n"
                + "        a(b);\n"
                + "    }\n"
                + "}\n";
        TreeContext leftCtx = new JdtTreeGenerator().generateFrom().string(leftInput);
        String rightInput = "class Main {\n"
                + "    public static void foo() {\n"
                + "        a.b();\n"
                + "    }\n"
                + "}";
        TreeContext rightCtx = new JdtTreeGenerator().generateFrom().string(rightInput);
        assertFalse(rightCtx.getRoot().isIsomorphicTo(leftCtx.getRoot()));
    }

    @Test
    public void testVarargs() throws IOException {
        String leftInput = "class Main {\n"
                + "    public foo(String a) {}\n"
                + "}\n";
        TreeContext leftCtx = new JdtTreeGenerator().generateFrom().string(leftInput);
        String rightInput = "class Main {\n"
                + "    public foo(String... a) {}\n"
                + "}\n";
        TreeContext rightCtx = new JdtTreeGenerator().generateFrom().string(rightInput);
        assertFalse(rightCtx.getRoot().isIsomorphicTo(leftCtx.getRoot()));

        String input = "class Main {\n"
                + "    public foo(String... a) {}\n"
                + "    public bar(String a) {}\n"
                + "}\n";
        TreeContext ctx = new JdtTreeGenerator().generateFrom().string(input);
        assertEquals(4, ctx.getRoot().getChild(0).getChildren().size());
    }

    @Test
    public void testJava8Syntax() throws IOException {
        String input = "public class Foo { public void foo(){ new ArrayList<Object>().stream().forEach(a -> {}); } }";
        Tree tree = new JdtTreeGenerator().generateFrom().string(input).getRoot();
        assertEquals(COMPILATION_UNIT, tree.getType());
        assertEquals(28, tree.getMetrics().size);
    }

    @Test
    public void badSyntax() throws IOException {
        String input = "public clas Foo {}";
        assertThrows(SyntaxException.class, () -> {
            new JdtTreeGenerator().generateFrom().string(input);
        });
    }

    @Test
    public void testTypeDefinition() throws IOException {
        String leftInput = "public class Foo {}";
        String rightInput = "public interface Foo {}";
        TreeContext leftContext = new JdtTreeGenerator().generateFrom().string(leftInput);
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().string(rightInput);
        assertFalse(leftContext.getRoot().isIsomorphicTo(rightContext.getRoot()));
    }

    @Test
    public void testInfixOperator() throws IOException {
        String leftInput = "class Foo { int i = 3 + 3; }";
        String rightInput = "class Foo { int i = 3 - 3; }";
        TreeContext leftContext = new JdtTreeGenerator().generateFrom().string(leftInput);
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().string(rightInput);
        assertFalse(leftContext.getRoot().isIsomorphicTo(rightContext.getRoot()));
    }

    @Test
    public void testAssignment() throws IOException {
        String leftInput = "class Foo { void foo() { int i = 12; } }";
        String rightInput = "class Foo { void foo() { int i += 12; } }";
        TreeContext leftContext = new JdtTreeGenerator().generateFrom().string(leftInput);
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().string(rightInput);
        assertFalse(leftContext.getRoot().isIsomorphicTo(rightContext.getRoot()));
    }

    @Test
    public void testPrefixExpression() throws IOException {
        String leftInput = "class Foo { void foo() { ++i; } }";
        String rightInput = "class Foo { void foo() { --i; } }";
        TreeContext leftContext = new JdtTreeGenerator().generateFrom().string(leftInput);
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().string(rightInput);
        assertFalse(leftContext.getRoot().isIsomorphicTo(rightContext.getRoot()));
    }

    @Test
    public void testPostfixExpression() throws IOException {
        String leftInput = "class Foo { void foo() { i++; } }";
        String rightInput = "class Foo { void foo() { i--; } }";
        TreeContext leftContext = new JdtTreeGenerator().generateFrom().string(leftInput);
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().string(rightInput);
        assertFalse(leftContext.getRoot().isIsomorphicTo(rightContext.getRoot()));
    }

    @Test
    public void testArrayCreation() throws IOException {
        String leftInput = "class Foo { int[][] tab = new int[12][]; }";
        TreeContext leftContext = new JdtTreeGenerator().generateFrom().string(leftInput);
        String rightInput = "class Foo { int[][] tab = new int[12][12]; }";
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().string(rightInput);
        assertFalse(leftContext.getRoot().isIsomorphicTo(rightContext.getRoot()));
    }

    @Test
    public void testIds() throws IOException {
        String input = "class Foo { String a; void foo(int a, String b) {}; void bar() { } }";
        TreeContext ct = new JdtTreeGenerator().generateFrom().string(input);
        assertEquals(ct.getRoot().getChild(0).getMetadata("id"), "Type Foo");
        assertEquals(ct.getRoot().getChild("0.2").getMetadata("id"), "Field a");
        assertEquals(ct.getRoot().getChild("0.3").getMetadata("id"), "Method foo( int String)");
        assertEquals(ct.getRoot().getChild("0.4").getMetadata("id"), "Method bar()");
    }
}
