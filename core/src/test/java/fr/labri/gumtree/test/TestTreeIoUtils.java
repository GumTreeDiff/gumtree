package fr.labri.gumtree.test;

import static fr.labri.gumtree.test.Constants.DUMMY_BIG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.ListIterator;

import org.junit.Test;

import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.TreeContext;

public class TestTreeIoUtils {

	@Test
	public void testSerializeTree() throws IOException {
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
		a.refresh();
		TreeIoUtils.toXml(tc, "target/test-classes/test-serialize.xml");
		ITree ca = TreeIoUtils.fromXmlFile("target/test-classes/test-serialize.xml").getRoot();
		
		assertTrue(a.isClone(ca));
		assertTrue(ca.getType() == 0);
		assertTrue(tc.getTypeLabel(ca).equals("type0"));
		assertTrue(ca.getLabel().equals("a"));
	}

	@Test
	public void testLoadBigTree() {
		ITree big = TreeIoUtils.fromXml(getClass().getResourceAsStream(DUMMY_BIG)).getRoot();
		assertEquals("a", big.getLabel());
		compareList(big.getChildren(), "b", "e", "f");
	}
	
	void compareList(List<ITree> lst, String... expected) {
		ListIterator<ITree> it = lst.listIterator();
		for(String e: expected) {
			ITree n = it.next();
			assertEquals(e, n.getLabel());
		}
		assertFalse(it.hasNext());
	}
}
