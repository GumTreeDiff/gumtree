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
 * Copyright 2021 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen.jsacorn;

import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.tree.Tree;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class JsAcornTreeGeneratorTest {
    @Test
    public void testStatement() throws IOException {
        String input = "console.log(\"Hello world!\");";
        Tree tree = new JsAcornTreeGenerator().generateFrom().string(input).getRoot();
        assertEquals(7, tree.getMetrics().size);
    }

    @Test
    public void testLambda() throws IOException {
        String input = "let f = (foo, bar) => foo + bar;";
        Tree tree = new JsAcornTreeGenerator().generateFrom().string(input).getRoot();
        assertEquals(10, tree.getMetrics().size);
    }

    @Test
    public void testComment() throws IOException {
        String input = "console.log(\"Hello world!\"); /* with comment */";
        Tree tree = new JsAcornTreeGenerator().generateFrom().string(input).getRoot();
        assertEquals(7, tree.getMetrics().size);
    }

    @Test
    public void testJson() throws IOException {
        String input = "{test: \"foo\"}";
        Tree tree = new JsAcornTreeGenerator().generateFrom().string(input).getRoot();
        assertEquals(6, tree.getMetrics().size);
    }

    @Test
    public void badSyntax() {
        String input = "function foo((bar) {}";
        assertThrows(SyntaxException.class, () -> {
            new JsAcornTreeGenerator().generateFrom().string(input);
        });
    }
}
