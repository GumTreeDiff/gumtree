package fr.labri.gumtree.test;

import static fr.labri.gumtree.test.Constants.DUMMY_BIG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.junit.Test;

import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.tree.Tree;

public class TestTreeIoUtils {

	@Test
	public void testSerializeTree() {
		Tree a = new Tree(0, "a", "type0");
		Tree b = new Tree(1, "b");
		b.setParentAndUpdateChildren(a);
		Tree c = new Tree(3, "c");
		c.setParentAndUpdateChildren(b);
		Tree d = new Tree(3, "d");
		d.setParentAndUpdateChildren(b);
		Tree e = new Tree(2);
		e.setParentAndUpdateChildren(a);
		// Refresh metrics is called because it is automatically called in fromXML
		a.refresh();
		System.out.println(TreeIoUtils.toXml(a));
		TreeIoUtils.toXml(a, "target/test-classes/test-serialize.xml");
		Tree ca = TreeIoUtils.fromXmlFile("target/test-classes/test-serialize.xml");
		
		assertTrue(a.isClone(ca));
		assertTrue(ca.getType() == 0);
		assertTrue(ca.getTypeLabel().equals("type0"));
		assertTrue(ca.getLabel().equals("a"));
	}

	@Test
	public void testLoadBigTree() {
		Tree big = TreeIoUtils.fromXml(getClass().getResourceAsStream(DUMMY_BIG));
		assertEquals("a", big.getLabel());
		compareList(big.getChildren(), "b", "e", "f");
	}
	
	void compareList(List<Tree> lst, String... expected) {
		ListIterator<Tree> it = lst.listIterator();
		for(String e: expected) {
			Tree n = it.next();
			assertEquals(e, n.getLabel());
		}
		assertFalse(it.hasNext());
	}
}
