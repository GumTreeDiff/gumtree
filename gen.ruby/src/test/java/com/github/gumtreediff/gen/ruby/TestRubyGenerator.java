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

package com.github.gumtreediff.gen.ruby;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.Test;
import static org.junit.Assert.*;

import com.github.gumtreediff.tree.ITree;

public class TestRubyGenerator {

    @Test
    public void testFileParsing() throws IOException {
        Reader r = new InputStreamReader(getClass().getResourceAsStream("/sample.rb"), "UTF-8");
        ITree tree = new RubyTreeGenerator().generateFromReader(r).getRoot();
        assertEquals(102, tree.getType());
        assertEquals(1726, tree.getSize());
    }

    @Test
    public void testSimpleSyntax() throws IOException {
        String input = "module Foo; puts \"Hello world!\"; end;";
        ITree t = new RubyTreeGenerator().generateFromString(input).getRoot();
        assertEquals(102, t.getType());
    }

    @Test
    public void testRuby2Syntax() throws IOException {
        String input = "{ foo: true }";
        ITree t = new RubyTreeGenerator().generateFromString(input).getRoot();
        assertEquals(102, t.getType());
    }

    @Test
    public void testPosition() throws IOException {
        String input = "module Baz\nclass Foo\n\tdef foo(bar)\n\t\tputs bar\n\tend\nend\nend";
        TreeContext ctx = new RubyTreeGenerator().generateFromString(input);
        ITree root = ctx.getRoot();
    }

}
