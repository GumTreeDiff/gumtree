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

import com.github.gumtreediff.tree.TreeContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PythonTreeSitterNgTreeGeneratorTest {
    private final PythonTreeSitterNgTreeGenerator generator = new PythonTreeSitterNgTreeGenerator();

    @Test
    public void testHelloWorld() throws IOException {
        TreeContext src = generator.generateFrom().string("print(\"Hello World!\")");
        assertEquals(6, src.getRoot().getMetrics().size);
    }

    @Test
    public void testString() throws IOException {
        TreeContext src = generator.generateFrom().file("testData/python/foo.py");
        assertEquals(12, src.getRoot().getMetrics().size);
    }

    @ParameterizedTest
    @ValueSource(strings = { "<", "<=", ">", ">=", "==", "!=" })
    public void testComparisonOperators(String operator) throws IOException {
        TreeContext src = generator.generateFrom().string("3 " + operator + " 2");
        assertEquals("comparison_operator_literal", src.getRoot().getChild("0.0.1").getType().name);
    }

    @ParameterizedTest
    @ValueSource(strings = { "and", "or"})
    public void testBooleanOperators(String operator) throws IOException {
        TreeContext src = generator.generateFrom().string("true " + operator + " false");
        assertEquals("logical_operator_literal", src.getRoot().getChild("0.0.1").getType().name);
    }

    @ParameterizedTest
    @ValueSource(strings = { "=", "+=", "-=", "*=", "/=", "//=", "%=", "**="})
    public void testAssignmentOperators(String operator) throws IOException {
        TreeContext src = generator.generateFrom().string("x " + operator + " 12");
        assertEquals("assignment", src.getRoot().getChild("0.0").getType().name);
        assertEquals("assignment_operator_literal", src.getRoot().getChild("0.0.1").getType().name);
    }
}