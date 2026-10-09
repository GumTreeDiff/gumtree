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
 * Copyright 2025 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen.json;

import java.io.IOException;

import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.tree.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestJsonTreeGenerator {
    @Test
    public void testJsonObject() throws IOException {
        String input = "{ \"foo\": \"bar\", \"age\": 31, \"arr\": [1, 2, 3] }";
        TreeContext ctx = new JsonTreeGenerator().generateFrom().string(input);
        assertEquals("Object", ctx.getRoot().getType().name);
    }

    @Test
    public void testJsonArray() throws IOException {
        String input = "[1, 2, 3]";
        TreeContext ctx = new JsonTreeGenerator().generateFrom().string(input);
        assertEquals("Array", ctx.getRoot().getType().name);
    }

    @Test
    public void testSyntaxError1() {
        String input = "{ \"foo\": \"bar\" ";
        Assertions.assertThrows(SyntaxException.class,
                () -> new JsonTreeGenerator().generateFrom().string(input));
    }

    @Test
    public void testSyntaxError2() {
        String input = "{ foo: \"bar\" }";
        Assertions.assertThrows(SyntaxException.class,
                () -> new JsonTreeGenerator().generateFrom().string(input));
    }

    @Test
    public void testSyntaxError3() {
        String input = "{ \"foo\" = \"bar\" }";
        Assertions.assertThrows(SyntaxException.class,
                () -> new JsonTreeGenerator().generateFrom().string(input));
    }
}
