package com.github.gumtreediff.test;

import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.Test;

import java.util.List;
import java.util.ListIterator;

import static org.junit.Assert.*;

public class TestTreeIoUtils {

    @Test
    public void testSerializeTree() throws Exception {
        TreeContext tc = new TreeContext();
        ITree a = tc.createTree(0, "a", "type0");
        tc.setRoot(a);

        ITree b = tc.createTree(1, "b", null);
        b.setParentAndUpdateChildren(a);
        ITree c = tc.createTree(3, "c", null);
        c.setParentAndUpdateChildren(b);
        ITree d = tc.createTree(3, "d", null);
        d.setParentAndUpdateChildren(b);
        ITree e = tc.createTree(2, null, null);
        e.setParentAndUpdateChildren(a);
        // Refresh metrics is called because it is automatically called in fromXML
        tc.validate();

        TreeIoUtils.toXml(tc).writeTo("target/test-classes/test-serialize.xml");
        TreeContext tca = TreeIoUtils.fromXmlFile("target/test-classes/test-serialize.xml");
        ITree ca = tca.getRoot();

        assertTrue(a.isClone(ca));
        assertTrue(ca.getType() == 0);
        assertTrue(tc.getTypeLabel(ca).equals("type0"));
        assertTrue(ca.getLabel().equals("a"));
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
