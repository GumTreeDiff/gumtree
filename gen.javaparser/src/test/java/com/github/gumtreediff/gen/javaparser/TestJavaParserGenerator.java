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

import static com.github.gumtreediff.tree.TypeSet.type;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.util.stream.Stream;

import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.tree.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TestJavaParserGenerator {
    public static final String COMPILATION_UNIT = "CompilationUnit";

    static Stream<Arguments> provideStringAndExpectedLength() {
        return Stream.of(
                arguments(COMPILATION_UNIT, 12,
                        "package foo.bar; public class Foo { public int foo; }"),
                arguments(COMPILATION_UNIT, 37, // Java 5
                        "public class Foo<A> { public List<A> foo; public void foo() "
                                + "{ for (A f : foo) { System.out.println(f); } } }"),
                arguments(COMPILATION_UNIT, 23, // Java 8
                        "public class Foo {\n"
                                + "\tpublic void foo() {\n"
                                + "\t\tnew ArrayList<Object>().stream().forEach(a -> {});\n"
                                + "\t}\n"
                                + "}"));
    }

    @ParameterizedTest
    @MethodSource("provideStringAndExpectedLength")
    public void testSimpleSyntax(String expectedRootType, int expectedSize, String input) throws IOException {
        Tree tree = new JavaParserGenerator().generateFrom().string(input).getRoot();
        assertEquals(type(expectedRootType), tree.getType());
        assertEquals(expectedSize, tree.getMetrics().size);
    }

    @Test
    public void testRange() throws IOException {
        String input = "public class Foo {}";
        Tree tree = new JavaParserGenerator().generateFrom().string(input).getRoot();
        assertEquals(0,tree.getPos());
        assertEquals(19,tree.getLength());
        assertEquals(0, tree.getChild("0.0").getPos()); // modifier pos
        assertEquals(6, tree.getChild("0.0").getLength()); // modifier length
        assertEquals(13, tree.getChild("0.1").getPos()); // identifier pos
        assertEquals(3, tree.getChild("0.1").getLength()); // identifier length
    }

    @Test
    public void testBadSyntax() {
        String input = "public clas Foo {}";
        assertThrows(SyntaxException.class, () -> {
            TreeContext ct = new JavaParserGenerator().generateFrom().string(input);
        });
    }
}
