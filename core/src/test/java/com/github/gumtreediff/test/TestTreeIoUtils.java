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
import com.github.gumtreediff.tree.ITree;
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
        System.out.println(bos);
        assertEquals("<?xml version=\"1.0\" ?>\n"
                     + "<root>\n"
                     + "  <context></context>\n"
                     + "  <tree type=\"TYPE_0\" label=\"a\" pos=\"0\" length=\"0\">\n"
                     + "    <tree type=\"TYPE_1\" label=\"b\" pos=\"0\" length=\"0\">\n"
                     + "      <tree type=\"TYPE_3\" label=\"c\" pos=\"0\" length=\"0\"></tree>\n"
                     + "      <tree type=\"TYPE_3\" label=\"d\" pos=\"0\" length=\"0\"></tree>\n"
                     + "    </tree>\n"
                     + "    <tree type=\"TYPE_2\" pos=\"0\" length=\"0\"></tree>\n"
                     + "  </tree>\n"
                     + "</root>\n", bos.toString());
        TreeContext tca = TreeIoUtils.fromXml().generateFrom().string(bos.toString());
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
        assertEquals(3, lr.positionFor(2, 1));
        assertEquals(7, lr.positionFor(3, 1));
        assertEquals(-1, lr.positionFor(5, 1));
    }

    @Test
    public void testPrintTextTree() throws Exception {
        TreeContext tc = getTreeContext();
        System.out.println(tc.toString());
        assertEquals("TYPE_0: a [0,0]\n"
                     + "    TYPE_1: b [0,0]\n"
                     + "        TYPE_3: c [0,0]\n"
                     + "        TYPE_3: d [0,0]\n"
                     + "    TYPE_2 [0,0]", tc.toString());
        assertEquals("TYPE_0: a [0,0]\n"
                     + "    TYPE_1: b [0,0]\n"
                     + "        TYPE_3: c [0,0]\n"
                     + "        TYPE_3: d [0,0]\n"
                     + "    TYPE_2 [0,0]", tc.getRoot().toTreeString());
        assertEquals("TYPE_1: b [0,0]\n"
                     + "    TYPE_3: c [0,0]\n"
                     + "    TYPE_3: d [0,0]", tc.getRoot().getChild(0).toTreeString());
    }

    @Test
    public void testDotFormatter() {
        TreeContext tc = getTreeContext();
        assertEquals("digraph G {\n"
                     + "\tid_0 [label=\"TYPE_0: a [0,0]\"];\n"
                     + "\tid_1 [label=\"TYPE_1: b [0,0]\"];\n"
                     + "\tid_0 -> id_1;\n"
                     + "\tid_2 [label=\"TYPE_3: c [0,0]\"];\n"
                     + "\tid_1 -> id_2;\n"
                     + "\tid_3 [label=\"TYPE_3: d [0,0]\"];\n"
                     + "\tid_1 -> id_3;\n"
                     + "\tid_4 [label=\"TYPE_2 [0,0]\"];\n"
                     + "\tid_0 -> id_4;\n"
                     + "}", TreeIoUtils.toDot(tc).toString());
    }

    private static TreeContext getTreeContext() {
        TreeContext tc = new TreeContext();
        ITree a = tc.createTree(TYPE_0, "a");
        tc.setRoot(a);

        ITree b = tc.createTree(TYPE_1, "b");
        b.setParentAndUpdateChildren(a);
        ITree c = tc.createTree(TYPE_3, "c");
        c.setParentAndUpdateChildren(b);
        ITree d = tc.createTree(TYPE_3, "d");
        d.setParentAndUpdateChildren(b);
        ITree e = tc.createTree(TYPE_2);
        e.setParentAndUpdateChildren(a);

        return tc;
    }
}
