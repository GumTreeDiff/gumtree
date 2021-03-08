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

package com.github.gumtreediff.test;

import com.github.gumtreediff.io.LineReader;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.Tree;
import static com.github.gumtreediff.tree.TypeSet.type;
import com.github.gumtreediff.tree.Type;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;
public class TestTreeIoUtils {
    private static final Type TYPE_0 = type("TYPE_0");
    private static final Type TYPE_1 = type("TYPE_1");
    private static final Type TYPE_2 = type("TYPE_2");
    private static final Type TYPE_3 = type("TYPE_3");

    @Test
    public void testSerializeTree() throws Exception {
        TreeContext tc = getTreeContext();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TreeIoUtils.toXml(tc).writeTo(bos);
        assertEquals("<?xml version=\"1.0\" ?>\n"
                     + "<root>\n"
                     + "  <context></context>\n"
                     + "  <tree type=\"TYPE_0\" pos=\"0\" length=\"1000\">\n"
                     + "    <tree type=\"TYPE_1\" pos=\"1\" length=\"50\">\n"
                     + "      <tree type=\"TYPE_3\" label=\"a\" pos=\"11\" length=\"10\"></tree>\n"
                     + "      <tree type=\"TYPE_3\" label=\"b\" pos=\"21\" length=\"10\"></tree>\n"
                     + "    </tree>\n"
                     + "    <tree type=\"TYPE_2\" pos=\"51\" length=\"900\"></tree>\n"
                     + "  </tree>\n"
                     + "</root>\n", bos.toString());
        TreeContext tca = TreeIoUtils.fromXml().generateFrom().string(bos.toString());
        assertEquals(tca.getRoot().getPos(), 0);
        assertEquals(tca.getRoot().getLength(), 1000);
        assertEquals(tca.getRoot().getChild(0).getPos(), 1);
        assertEquals(tca.getRoot().getChild(0).getLength(), 50);
        assertEquals(tca.getRoot().getChild(1).getPos(), 51);
        assertEquals(tca.getRoot().getChild(1).getLength(), 900);
        assertTrue(tc.getRoot().isIsomorphicTo(tca.getRoot()));
    }

    @Test
    public void testLineReader() throws IOException {
        LineReader lr = new LineReader(new StringReader("foo\nbar\nbaz\n"));
        int intValueOfChar;
        String targetString = "";
        while ((intValueOfChar = lr.read()) != -1) {
            targetString += (char) intValueOfChar;
        }
        lr.close();
        
        // (line, column) to offset
        assertEquals(4, lr.positionFor(2, 1));
        assertEquals(8, lr.positionFor(3, 1));
        assertEquals(-1, lr.positionFor(5, 1));
        
        // offset to (line, column)
        assertArrayEquals(new int[] { 2, 1 }, lr.positionFor(4));
        assertArrayEquals(new int[] { 3, 1 }, lr.positionFor(8));
        assertArrayEquals(new int[] { 1, 3 }, lr.positionFor(2));
        assertArrayEquals(new int[] { 2, 3 }, lr.positionFor(6));
        assertArrayEquals(new int[] { 3, 2 }, lr.positionFor(9));
    }

    @Test
    public void testPrintTextTree() throws Exception {
        TreeContext tc = getTreeContext();
        assertEquals("TYPE_0 [0,1000]\n"
                     + "    TYPE_1 [1,51]\n"
                     + "        TYPE_3: a [11,21]\n"
                     + "        TYPE_3: b [21,31]\n"
                     + "    TYPE_2 [51,951]", tc.toString());
        assertEquals("TYPE_0 [0,1000]\n"
                     + "    TYPE_1 [1,51]\n"
                     + "        TYPE_3: a [11,21]\n"
                     + "        TYPE_3: b [21,31]\n"
                     + "    TYPE_2 [51,951]", tc.getRoot().toTreeString());
        assertEquals("TYPE_1 [1,51]\n"
                     + "    TYPE_3: a [11,21]\n"
                     + "    TYPE_3: b [21,31]", tc.getRoot().getChild(0).toTreeString());
    }

    @Test
    public void testDotFormatter() {
        TreeContext tc = getTreeContext();
        assertEquals("digraph G {\n"
                     + "\tid_0 [label=\"TYPE_0 [0,1000]\"];\n"
                     + "\tid_1 [label=\"TYPE_1 [1,51]\"];\n"
                     + "\tid_0 -> id_1;\n"
                     + "\tid_2 [label=\"TYPE_3: a [11,21]\"];\n"
                     + "\tid_1 -> id_2;\n"
                     + "\tid_3 [label=\"TYPE_3: b [21,31]\"];\n"
                     + "\tid_1 -> id_3;\n"
                     + "\tid_4 [label=\"TYPE_2 [51,951]\"];\n"
                     + "\tid_0 -> id_4;\n"
                     + "}", TreeIoUtils.toDot(tc).toString());
    }

    private static TreeContext getTreeContext() {
        TreeContext tc = new TreeContext();
        Tree a = tc.createTree(TYPE_0);
        a.setPos(0);
        a.setLength(1000);
        tc.setRoot(a);

        Tree b = tc.createTree(TYPE_1);
        b.setPos(1);
        b.setLength(50);
        b.setParentAndUpdateChildren(a);
        Tree c = tc.createTree(TYPE_3, "a");
        c.setPos(11);
        c.setLength(10);
        c.setParentAndUpdateChildren(b);
        Tree d = tc.createTree(TYPE_3, "b");
        d.setPos(21);
        d.setLength(10);
        d.setParentAndUpdateChildren(b);
        Tree e = tc.createTree(TYPE_2);
        e.setPos(51);
        e.setLength(900);
        e.setParentAndUpdateChildren(a);

        return tc;
    }
}
