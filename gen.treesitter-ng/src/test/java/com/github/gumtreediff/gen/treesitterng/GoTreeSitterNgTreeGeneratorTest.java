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

public class GoTreeSitterNgTreeGeneratorTest {
    private final GoTreeSitterNgTreeGenerator generator = new GoTreeSitterNgTreeGenerator();

    @Test
    public void testHelloWorld() throws IOException {
        TreeContext src = generator.generateFrom().string("package main\n" +
                "import \"fmt\"\n" +
                "func main() {\n" +
                "    fmt.Println(\"hello world\")\n" +
                "}");
        assertEquals(20, src.getRoot().getMetrics().size);
    }
}