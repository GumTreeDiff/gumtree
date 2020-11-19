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

package com.github.gumtreediff.gen.c;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.github.gumtreediff.tree.Tree;

import static org.junit.jupiter.api.Assertions.*;

public class TestCGenerator {
    @Test
    public void testSimpleSyntax() throws IOException {
        String input = "int main() { printf(\"Hello world!\"); return 0; }";
        Tree t = new CTreeGenerator().generateFrom().string(input).getRoot();
        assertEquals(18, t.getMetrics().size);
    }
}
