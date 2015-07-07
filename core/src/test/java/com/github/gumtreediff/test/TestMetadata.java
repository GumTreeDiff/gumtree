package com.github.gumtreediff.test;

import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import static org.junit.Assert.*;

public class TestMetadata {
    ITree someNode;
    TreeContext tc;

    final String key = "key";
    final String v1 = "test";
    final String v2 = "other";
    final String v3 = "more";

    @Before
    public void setUp() throws Exception {
        tc = new TreeContext();
        someNode = tc.createTree(0, "", "");
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
    public void testGlobalPutNode() throws Exception {
        assertNull(someNode.getMetadata(key));
        assertNull(tc.setMetadata(key, v1));
        assertEquals(v1, tc.getMetadata(key));
        assertEquals(v1, tc.getMetadata(someNode, key));

        assertEquals(v1, tc.setMetadata(key, v2));
        assertEquals(v2, tc.getMetadata(key));
        assertEquals(v2, tc.setMetadata(someNode, key, v1));
        assertEquals(v1, tc.getMetadata(someNode, key));
        assertEquals(v2, tc.getMetadata(key));
        assertEquals(v1, tc.setMetadata(someNode, key, null));
        assertEquals(v2, tc.getMetadata(someNode, key));
        assertEquals(v2, tc.setMetadata(someNode, key, v1));
        assertEquals(v1, someNode.setMetadata(key, null));
        assertEquals(v2, tc.getMetadata(someNode, key));
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
            assertNotEquals("Iterate more than once", -1, i);
            seen.remove(i);
            i = keyList.indexOf(e.getKey());
            assertEquals("Not the right entry", i, (Object) values[i]);
        }
        assertEquals("Some metadata are not iterated", 0, seen.size());
    }

    @Test
    public void testGlobalIterator() throws Exception {
        final String v4 = "lastkey";
        String[] keys = {key, v1, v2, v3, v4};
        Integer[] values = {0, 1, 2, 3, 4};
        populate(keys, values, keys.length - 1);

        tc.setMetadata(key, 5);
        tc.setMetadata(v4, 4);
        checkIterator(keys, values, tc.getMetadata(someNode));
    }

    private void populate(String[] keys, Integer[] values, int size) {
        for(int i = 0; i < size; i ++)
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

        assertEquals("Export JSON", valJSON, TreeIoUtils.toJson(tc).export(v3).toString());
        assertEquals("Export LISP", valLISP, TreeIoUtils.toLisp(tc).toString());
        assertEquals("Export XML", valXML, TreeIoUtils.toXml(tc).toString());
        assertEquals("Export Compact XML", valXMLCompact, TreeIoUtils.toCompactXml(tc).toString());
    }

    final String valJSON = "{\n" +
            "\t\"other\": \"more\",\n" +
            "\t\"more\": \"more\",\n" +
            "\t\"root\": {\n" +
            "\t\t\"type\": \"0\",\n" +
            "\t\t\"key\": \"test\",\n" +
            "\t\t\"pos\": \"[1, 2, 3, 4]\",\n" +
            "\t\t\"children\": []\n" +
            "\t}\n" +
            "}";

    final String valLISP = "(((:other \"more\") ) (0 \"0\" \"\" ((:key \"test\") (:pos \"[1, 2, 3, 4]\") ) ())";

    final String valXML = "<?xml version=\"1.0\" ?>\n" +
            "<root>\n" +
            "  <context>\n" +
            "    <other>more</other>\n" +
            "  </context>\n" +
            "  <tree type=\"0\">\n" +
            "    <key>test</key>\n" +
            "    <pos>[1, 2, 3, 4]</pos>\n" +
            "  </tree>\n" +
            "</root>\n";

    final String valXMLCompact = "<?xml version=\"1.0\" ?>\n" +
            "<root other=\"more\">\n" +
            "  <0 key=\"test\" pos=\"[1, 2, 3, 4]\"></0>\n" +
            "</root>\n";
}
