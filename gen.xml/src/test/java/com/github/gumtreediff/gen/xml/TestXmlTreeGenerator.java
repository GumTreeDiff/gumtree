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
 * Copyright 2023 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen.xml;

import java.io.IOException;

import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.tree.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.github.gumtreediff.tree.TypeSet.type;
import static org.junit.jupiter.api.Assertions.*;

public class TestXmlTreeGenerator {
    @Test
    public void testSimpleSyntax() throws IOException {
        String input = "<students>\n"
                + "    <list>Students list</list>\n"
                + "    <student name=\"foo\"/>\n"
                + "    <student name=\"bar\"/>\n"
                + "    <student name=\"baz\"/>\n"
                + "</student>";
        Tree t = new XmlTreeGenerator().generateFrom().string(input).getRoot();
        System.out.println(t.toTreeString());
    }

    @Test
    public void testXmlDeclaration() throws IOException {
        String input = "<?xml version=\"1.0\" encoding=\"utf-8\"?><root><foo arg=\"bar\"/><baz></baz></root>";
        Tree t = new XmlTreeGenerator().generateFrom().string(input).getRoot();
        System.out.println(t.toTreeString());
    }
}
