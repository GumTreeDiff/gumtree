package fr.labri.gumtree.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.tree.Tree;

public class TestSerializeTree {

	@Test
	public void testSerializeTree() {
		Tree a = new Tree(0, "a", "type0");
		Tree b = new Tree(1, "b");
		b.setParentAndUpdateChildren(a);
		Tree c = new Tree(3, "c");
		c.setParentAndUpdateChildren(b);
		Tree d = new Tree(3, "d");
		d.setParentAndUpdateChildren(b);
		Tree e = new Tree(2, "e");
		e.setParentAndUpdateChildren(a);
		// Refresh metrics is called because it is automatically called in fromXML
		a.refresh();
		TreeIoUtils.toXml(a, "target/test-classes/test-serialize.xml");
		Tree ca = TreeIoUtils.fromXml("target/test-classes/test-serialize.xml");
		
		assertTrue(a.isClone(ca));
		assertTrue(ca.getType() == 0);
		assertTrue(ca.getTypeLabel().equals("type0"));
		assertTrue(ca.getLabel().equals("a"));
	}

}
