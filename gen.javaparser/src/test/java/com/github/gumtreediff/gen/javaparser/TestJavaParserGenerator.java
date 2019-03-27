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

import static com.github.gumtreediff.tree.Symbol.symbol;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.tree.Symbol;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.Test;

import com.github.gumtreediff.tree.ITree;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TestJavaParserGenerator {

    private final String input;
    private final Symbol expectedRootSymbol;
    private final int expectedSize;

    public static final Symbol COMPILATION_UNIT = symbol("CompilationUnit");

    public TestJavaParserGenerator(Symbol expectedRootSymbol, int expectedSize, String input) {
        this.expectedRootSymbol = expectedRootSymbol;
        this.expectedSize = expectedSize;
        this.input = input;
    }

    @Parameterized.Parameters
    public static Collection provideStringAndExpectedLength() {
        return Arrays.asList(new Object[][] {
                {COMPILATION_UNIT, 9,
                        "public class Foo { public int foo; }"},
                {COMPILATION_UNIT, 37, // Java 5
                        "public class Foo<A> { public List<A> foo; public void foo() "
                                + "{ for (A f : foo) { System.out.println(f); } } }"},
                {COMPILATION_UNIT, 23, // Java 8
                        "public class Foo {\n"
                                + "\tpublic void foo() {\n"
                                + "\t\tnew ArrayList<Object>().stream().forEach(a -> {});\n"
                                + "\t}\n"
                                + "}"},
        });
    }

    @Test
    public void testSimpleSyntax() throws IOException {
        ITree tree = new JavaParserGenerator().generateFromString(input).getRoot();
        assertEquals(expectedRootSymbol, tree.getType());
        assertEquals(expectedSize, tree.getSize());
    }

    @Test(expected = SyntaxException.class)
    public void badSyntax() throws IOException {
        String input = "public clas Foo {}";
        TreeContext ct = new JavaParserGenerator().generateFromString(input);
    }

}
