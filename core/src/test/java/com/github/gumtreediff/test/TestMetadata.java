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
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import static com.github.gumtreediff.tree.TypeSet.type;
import static org.junit.jupiter.api.Assertions.*;

public class TestMetadata {
    Tree someNode;
    TreeContext tc;

    final String key = "key";
    final String v1 = "test";
    final String v2 = "other";
    final String v3 = "more";

    @BeforeEach
    public void setUp() throws Exception {
        tc = new TreeContext();
        someNode = tc.createTree(type("type0"), "");
        tc.setRoot(someNode);
    }

    @Test
    public void testPutNode() throws Exception {
        assertNull(someNode.getMetadata(key));
        assertNull(someNode.setMetadata(key, v1));
        assertEquals(v1, someNode.getMetadata(key));
        assertEquals(v1, someNode.setMetadata(key, v2));
        assertEquals(v2, someNode.getMetadata(key));
        assertEquals(v2, someNode.setMetadata(key, null));
        assertNull(someNode.setMetadata(key, v2));
    }

    @Test
    public void testLocalIterator() throws Exception {
        String[] keys = {key, v1, v2, v3};
        Integer[] values = {0, 1, 2, 3};
        populate(keys, values, keys.length);

        checkIterator(keys, values, someNode.getMetadata());
    }

    private void checkIterator(String[] keys, Integer[] values, Iterator<Entry<String, Object>> it) {
        List<String> keyList = Arrays.asList(keys);
        List<String> seen = new ArrayList<>(keys.length);
        seen.addAll(keyList);
        while (it.hasNext()) {
            Entry<String, Object> e = it.next();
            int i = seen.indexOf(e.getKey());
            assertNotEquals(-1, i,"Iterate more than once");
            seen.remove(i);
            i = keyList.indexOf(e.getKey());
            assertEquals(i, (Object) values[i],"Not the right entry");
        }
        assertEquals(0, seen.size(),"Some metadata are not iterated");
    }

    @Test
    public void testGlobalIterator() throws Exception {
        TreeContext ctx = new TreeContext();
        Object res1 = ctx.setMetadata("foo", "bar");
        assertEquals("bar", ctx.getMetadata("foo"));
        assertNull(res1);
        Object res2 = ctx.setMetadata("foo", "baz");
        assertEquals("baz", ctx.getMetadata("foo"));
        assertEquals("bar", res2);
        ctx.setMetadata("null", null);
        assertNull(ctx.getMetadata("null"));
        assertNull(ctx.getMetadata("unknown"));
    }

    private void populate(String[] keys, Integer[] values, int size) {
        for (int i = 0; i < size; i ++)
            someNode.setMetadata(keys[i], values[i]);
    }

    @Test
    public void testExportCustom() throws Exception {
        final String pos = "pos";
        someNode.setMetadata(key, v1);
        someNode.setMetadata(pos, new int[]{1,2,3,4});
        tc.setMetadata(v2, v3);
        tc.setMetadata(v3, v3);

        tc.export(key, v2);
        tc.export(pos, x -> Arrays.toString((int[]) x));

        assertEquals(valJson, TreeIoUtils.toJson(tc).export(v3).toString(), "Export JSON");
        assertEquals(valLisp, TreeIoUtils.toLisp(tc).toString(),"Export LISP");
        assertEquals(valXml, TreeIoUtils.toXml(tc).toString(),"Export XML");
        assertEquals(valXmlCompact, TreeIoUtils.toCompactXml(tc).toString(),"Export Compact XML");

        assertEquals(Sets.newHashSet(key, v2, pos), tc.getSerializers().exports());
        tc.getSerializers().remove(pos);
        assertEquals(Sets.newHashSet(key, v2), tc.getSerializers().exports());
    }

    @Test
    public void testExportInvalid1() {
        assertThrows(RuntimeException.class, () -> {
            tc.export("Test key");
        });
    }

    @Test
    public void testExportInvalid2() {
        assertThrows(RuntimeException.class, () -> {
            TreeIoUtils.toJson(tc).export("Test key");
        });
    }

    final String valJson = "{\n"
            + "\t\"other\": \"more\",\n"
            + "\t\"more\": \"more\",\n"
            + "\t\"root\": {\n"
            + "\t\t\"type\": \"type0\",\n"
            + "\t\t\"pos\": \"0\",\n"
            + "\t\t\"length\": \"0\",\n"
            + "\t\t\"key\": \"test\",\n"
            + "\t\t\"pos\": \"[1, 2, 3, 4]\",\n"
            + "\t\t\"children\": []\n"
            + "\t}\n"
            + "}";

    final String valLisp = "(((:other more) ) (type0 \"\" ((0 0)(:key test) (:pos \"[1, 2, 3, 4]\") ) ())";

    final String valXml = "<?xml version=\"1.0\" ?>\n"
            + "<root>\n"
            + "  <context>\n"
            + "    <other>more</other>\n"
            + "  </context>\n"
            + "  <tree type=\"type0\" pos=\"0\" length=\"0\">\n"
            + "    <key>test</key>\n"
            + "    <pos>[1, 2, 3, 4]</pos>\n"
            + "  </tree>\n"
            + "</root>\n";

    final String valXmlCompact = "<?xml version=\"1.0\" ?>\n"
            + "<root other=\"more\">\n"
            + "  <type0 key=\"test\" pos=\"[1, 2, 3, 4]\"/>\n"
            + "</root>\n";
}
