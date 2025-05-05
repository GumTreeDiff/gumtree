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
import org.junit.jupiter.api.Disabled;
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

    @Test
    public void testGenericFunctionWithTypeParameter() throws IOException {
        String input = "class testStructure { void foo() { Collections.<String>emptyList(); } }";
        TreeContext ct = new JdtTreeGenerator().generateFrom().string(input);
        String expected = """
                CompilationUnit [0,71]
                    TypeDeclaration [0,71]
                        TYPE_DECLARATION_KIND: class [0,5]
                        SimpleName: testStructure [6,19]
                        MethodDeclaration [22,69]
                            PrimitiveType: void [22,26]
                            SimpleName: foo [27,30]
                            Block [33,69]
                                ExpressionStatement [35,67]
                                    MethodInvocation [35,66]
                                        METHOD_INVOCATION_RECEIVER [35,46]
                                            SimpleName: Collections [35,46]
                                            SimpleType [48,54]
                                                SimpleName: String [48,54]
                                        SimpleName: emptyList [55,64]""";
        assertEquals(expected, ct.getRoot().toTreeString());
    }

    @Test
    public void testTagElement() throws IOException {
        String input = "/** @author john */ class C {}";
        TreeContext ct = new JdtTreeGenerator().generateFrom().string(input);
        String expected = """
                CompilationUnit [0,30]
                    TypeDeclaration [0,30]
                        Javadoc [0,19]
                            TagElement [4,17]
                                TAG_NAME: @author [4,11]
                                TextElement:  john  [11,17]
                        TYPE_DECLARATION_KIND: class [20,25]
                        SimpleName: C [26,27]""";
        assertEquals(expected, ct.getRoot().toTreeString());
    }

    @Test
    public void testComments() throws IOException {
        String input = """
                class bar {
                        void foo(/*int a*/)
                        {
                                //run();
                        }
                }
                """;
        TreeContext ct = new JdtWithCommentsTreeGenerator().generateFrom().string(input);
        String expected = """
                CompilationUnit [0,87]
                    TypeDeclaration [0,86]
                        TYPE_DECLARATION_KIND: class [0,5]
                        SimpleName: bar [6,9]
                        MethodDeclaration [20,84]
                            PrimitiveType: void [20,24]
                            SimpleName: foo [25,28]
                            BlockComment: /*int a*/ [29,38]
                            Block [48,84]
                                LineComment: //run(); [66,74]""";
        assertEquals(expected, ct.getRoot().toTreeString());
    }

    @Test
    public void testComments2() throws IOException {
        String input = """
                /**
                         * test
                         */
                public class X {
                    void A(boolean b
                    ) {
                        /**
                         * test2\s
                         */
                        sleep();
                    }
                }
                """;
        String expected = """
                CompilationUnit [0,145]
                    TypeDeclaration [0,144]
                        Javadoc [0,31]
                            TagElement [15,19]
                                TextElement: test [15,19]
                        Modifier: public [32,38]
                        TYPE_DECLARATION_KIND: class [39,44]
                        SimpleName: X [45,46]
                        MethodDeclaration [53,142]
                            PrimitiveType: void [53,57]
                            SimpleName: A [58,59]
                            SingleVariableDeclaration [60,69]
                                PrimitiveType: boolean [60,67]
                                SimpleName: b [68,69]
                            Block [76,142]
                                Javadoc [86,119]
                                    TagElement [101,107]
                                        TextElement: test2  [101,107]
                                ExpressionStatement [128,136]
                                    MethodInvocation [128,135]
                                        SimpleName: sleep [128,133]""";
        TreeContext ct = new JdtWithCommentsTreeGenerator().generateFrom().string(input);
        assertEquals(expected, ct.getRoot().toTreeString());
    }

    @Test
    public void testJdtPropertyKeywords() throws IOException {
        String input = """
                package bug.missingSubtrees;
                
                public sealed class test permits A {
                    void X() throws RuntimeException{
                        sleep(3);
                    }
                }
                class B extends A {
                    void m2();
                }
                
                class C implements I {
                    void m1();
                }
                """;
        TreeContext ct = new JdtTreeGenerator().generateFrom().string(input);
        String treeString = ct.getRoot().toTreeString();
        //Check permits, implements, extends, throws keywords
        assertTrue(treeString.contains("permits"), "Expected 'permits' keyword in tree string");
        assertTrue(treeString.contains("implements"), "Expected 'implements' keyword in tree string");
        assertTrue(treeString.contains("extends"), "Expected 'extends' keyword in tree string");
        assertTrue(treeString.contains("throws"), "Expected 'throws' keyword in tree string");

    }

    @Test
    public void testJdtPropertyKeywords2() throws IOException {
        String input = """
                public sealed class MyClass extends BaseClass implements InterfaceA, InterfaceB permits P1, P2 {
                    // class body
                }
                """;
        TreeContext ct = new JdtTreeGenerator().generateFrom().string(input);
        String treeString = ct.getRoot().toTreeString();
        String expected = """
                CompilationUnit [0,117]
                    TypeDeclaration [0,116]
                        Modifier: public [0,6]
                        Modifier: sealed [7,13]
                        TYPE_DECLARATION_KIND: class [14,19]
                        SimpleName: MyClass [20,27]
                        CLASS_INHERITANCE_KEYWORD: extends [28,35]
                        SimpleType [36,45]
                            SimpleName: BaseClass [36,45]
                        CLASS_INHERITANCE_KEYWORD: implements [46,56]
                        SimpleType [57,67]
                            SimpleName: InterfaceA [57,67]
                        SimpleType [69,79]
                            SimpleName: InterfaceB [69,79]
                        PERMITS_KEYWORD: permits [80,87]
                        SimpleType [88,90]
                            SimpleName: P1 [88,90]
                        SimpleType [92,94]
                            SimpleName: P2 [92,94]""";
        assertEquals(expected, treeString);
    }
    @Test
    public void testJdtPropertyKeywords3() throws IOException {
        String input = """
                public sealed class MyClass implements InterfaceA, InterfaceB permits P1, P2 {
                    // class body
                }
                """;
        TreeContext ct = new JdtTreeGenerator().generateFrom().string(input);
        String treeString = ct.getRoot().toTreeString();
        //Check permits, implements, extends, throws keywords
        String expected = """
                CompilationUnit [0,99]
                    TypeDeclaration [0,98]
                        Modifier: public [0,6]
                        Modifier: sealed [7,13]
                        TYPE_DECLARATION_KIND: class [14,19]
                        SimpleName: MyClass [20,27]
                        CLASS_INHERITANCE_KEYWORD: implements [28,38]
                        SimpleType [39,49]
                            SimpleName: InterfaceA [39,49]
                        SimpleType [51,61]
                            SimpleName: InterfaceB [51,61]
                        PERMITS_KEYWORD: permits [62,69]
                        SimpleType [70,72]
                            SimpleName: P1 [70,72]
                        SimpleType [74,76]
                            SimpleName: P2 [74,76]""";
        assertEquals(expected, treeString);
    }
}
