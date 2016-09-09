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

package com.github.gumtreediff.gen.js;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.junit.Test;

import com.github.gumtreediff.tree.ITree;

public class TestJsGenerator {

    @Test
    public void testFileParsing() throws IOException {
        Reader r = new InputStreamReader(getClass().getResourceAsStream("/sample.js"));
        ITree tree = new RhinoTreeGenerator().generateFromReader(r).getRoot();
        assertEquals(136, tree.getType());
        assertEquals(401, tree.getSize());
    }

    @Test
    public void testSimpleSyntax() throws IOException {
        String input = "console.log(\"Hello world!\");";
        ITree tree = new RhinoTreeGenerator().generateFromString(input).getRoot();
        assertEquals(136, tree.getType());
        assertEquals(7, tree.getSize());
    }

    @Test
    public void testComments() throws IOException {
        String input = "console.log(\"Hello world!\"); /* with comment */";
        ITree tree = new RhinoTreeGenerator().generateFromString(input).getRoot();
        assertEquals(8, tree.getSize());
    }

}
