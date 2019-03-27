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

import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Symbol;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.Test;

import java.util.List;
import java.util.ListIterator;

import java.io.ByteArrayOutputStream;

import static com.github.gumtreediff.tree.Symbol.symbol;
import static org.junit.Assert.*;

public class TestTreeIoUtils {

    private static final Symbol TYPE_0 = symbol("TYPE_0");
    private static final Symbol TYPE_1 = symbol("TYPE_1");
    private static final Symbol TYPE_2 = symbol("TYPE_2");
    private static final Symbol TYPE_3 = symbol("TYPE_3");

    @Test
    public void testSerializeTree() throws Exception {
        TreeContext tc = new TreeContext();
        ITree a = tc.createTree(TYPE_0, "a");
        tc.setRoot(a);

        ITree b = tc.createTree(TYPE_1, "b");
        b.setParentAndUpdateChildren(a);
        ITree c = tc.createTree(TYPE_3, "c");
        c.setParentAndUpdateChildren(b);
        ITree d = tc.createTree(TYPE_3, "d");
        d.setParentAndUpdateChildren(b);
        ITree e = tc.createTree(TYPE_2, null);
        e.setParentAndUpdateChildren(a);
        // Refresh metrics is called because it is automatically called in fromXML
        tc.validate();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        TreeIoUtils.toXml(tc).writeTo(bos);
        TreeContext tca = TreeIoUtils.fromXml().generateFromString(bos.toString());
        ITree ca = tca.getRoot();

        assertTrue(a.isIsomorphicTo(ca));
        assertTrue(ca.getType() == TYPE_0);
        assertTrue(ca.getLabel().equals("a"));
    }

    @Test
    public void testPrintTextTree() throws Exception {
        TreeContext tc = new TreeContext();
        ITree a = tc.createTree(TYPE_0, "a");
        tc.setRoot(a);

        ITree b = tc.createTree(TYPE_1, "b");
        b.setParentAndUpdateChildren(a);
        ITree c = tc.createTree(TYPE_3, "c");
        c.setParentAndUpdateChildren(b);
        ITree d = tc.createTree(TYPE_3, "d");
        d.setParentAndUpdateChildren(b);
        ITree e = tc.createTree(TYPE_2, null);
        e.setParentAndUpdateChildren(a);
        // Refresh metrics is called because it is automatically called in fromXML
        tc.validate();

        System.out.println("*****************");
        System.out.println(a.toPrettyTreeString(tc));
        System.out.println("-----------------");
        System.out.println(b.toPrettyTreeString(tc));
    }

    @Test
    public void testLoadBigTree() {
        ITree big = TreeLoader.getDummyBig();
        assertEquals("a", big.getLabel());
        compareList(big.getChildren(), "b", "e", "f");
    }

    void compareList(List<ITree> lst, String... expected) {
        ListIterator<ITree> it = lst.listIterator();
        for (String e: expected) {
            ITree n = it.next();
            assertEquals(e, n.getLabel());
        }
        assertFalse(it.hasNext());
    }
}
