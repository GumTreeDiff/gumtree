/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2026 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */
package com.github.gumtreediff.gen.cdt;

import com.github.gumtreediff.tree.TreeContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CdtTreeGeneratorTest {
    @Test
    public void testC() throws IOException {
        String source = "int main() { return 0; }";
        TreeContext context = new CCdtTreeGenerator().generateFrom().string(source);

        assertTrue(context.getRoot().getMetrics().size > 1);
        assertEquals(source.length(), context.getRoot().getLength());
        assertTrue(context.toString().contains("CASTFunctionDefinition"));
    }

    @Test
    public void testCpp() throws IOException {
        String source = "class Widget { public: int value() { return 1; } };";
        TreeContext context = new CppCdtTreeGenerator().generateFrom().string(source);

        assertTrue(context.getRoot().getMetrics().size > 1);
        assertEquals(source.length(), context.getRoot().getLength());
        assertTrue(context.toString().contains("CPPASTCompositeTypeSpecifier"));
    }
}
