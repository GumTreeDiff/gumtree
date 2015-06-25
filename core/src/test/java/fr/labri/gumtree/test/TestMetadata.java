package fr.labri.gumtree.test;

import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.TreeContext;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestMetadata {
    ITree someNode;

    @Before
    public void setUp() throws Exception {
        someNode = new TreeContext().createTree(0, "", "");
    }

    @Test
    public void testPut() throws Exception {
        String v1 = "test";
        String v2 = "other";
        assertTrue(someNode.setMetadata("test", v1));
        assertEquals(someNode.getMetadata("test", null), v1);
        assertFalse(someNode.setMetadata("test", v2));
        assertEquals(someNode.getMetadata("test", null), v2);
    }
}
