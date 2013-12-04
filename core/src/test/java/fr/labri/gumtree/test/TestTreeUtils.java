package fr.labri.gumtree.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import static fr.labri.gumtree.test.TestConstants.*;
import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.tree.Tree;
import fr.labri.gumtree.tree.TreeUtils;

public class TestTreeUtils {
	
	@Test
	public void testPostOrderNumbering() {
		Tree root = TreeIoUtils.fromXml(DUMMY_SRC);
		TreeUtils.postOrderNumbering(root);
		assertEquals(root.getId(), 4);
		assertEquals(root.getChildren().get(0).getId(), 2);
		assertEquals(root.getChildren().get(0).getChildren().get(0).getId(), 0);
		assertEquals(root.getChildren().get(0).getChildren().get(1).getId(), 1);
		assertEquals(root.getChildren().get(1).getId(), 3);
	}
	
	@Test
	public void testDepth() {
		Tree root = TreeIoUtils.fromXml(DUMMY_SRC);
		TreeUtils.computeDepth(root);
		assertEquals(root.getDepth(), 0);
		assertEquals(root.getChildren().get(0).getDepth(), 1);
		assertEquals(root.getChildren().get(0).getChildren().get(0).getDepth(), 2);
		assertEquals(root.getChildren().get(0).getChildren().get(1).getDepth(), 2);
		assertEquals(root.getChildren().get(1).getDepth(), 1);
	}
	
	@Test
	public void testHeight() {
		Tree root = TreeIoUtils.fromXml(DUMMY_SRC);
		assertEquals(2, root.getHeight()); // depth of a 
		assertEquals(1, root.getChildren().get(0).getHeight()); // depth of b
		assertEquals(0, root.getChildren().get(0).getChildren().get(0).getHeight()); // depth of c
		assertEquals(0, root.getChildren().get(0).getChildren().get(1).getHeight()); // depth of d
		assertEquals(0, root.getChildren().get(1).getHeight()); // depth of e
	}
	
	@Test
	public void testPreOrderNumbering() {
		Tree root = TreeIoUtils.fromXml(DUMMY_SRC);
		TreeUtils.preOrderNumbering(root);
		assertEquals(0, root.getId()); // id of a
		assertEquals(1, root.getChildren().get(0).getId()); // id of b
		assertEquals(2, root.getChildren().get(0).getChildren().get(0).getId()); // id of c
		assertEquals(3, root.getChildren().get(0).getChildren().get(1).getId()); // id of d
		assertEquals(4, root.getChildren().get(1).getId()); // id of e
	}
	
	@Test
	public void testBreadthFirstNumbering() {
		Tree tree = TreeIoUtils.fromXml(DUMMY_SRC);
		TreeUtils.bfsOrderNumbering(tree);
		assertEquals(tree.getId(), 0);
		assertEquals(tree.getChildren().get(0).getId(), 1);
		assertEquals(tree.getChildren().get(0).getChildren().get(0).getId(), 3);
		assertEquals(tree.getChildren().get(0).getChildren().get(1).getId(), 4);
		assertEquals(tree.getChildren().get(1).getId(), 2);
	}
	
	@Test
	public void testDigest() {
		Tree root = TreeIoUtils.fromXml(DUMMY_SRC);
		Tree croot = root.deepCopy();
		TreeUtils.computeDigest(root);
		TreeUtils.computeDigest(croot);
		assertTrue(root.getDigest() == croot.getDigest());
		croot.getChildren().get(0).getChildren().get(0).setLabel("x");
		TreeUtils.computeDigest(croot);
		assertFalse(root.getDigest() == croot.getDigest());
		System.out.println(root.toDigestTreeString());
	}
	
	@Test
	public void testRemoveCompletelyMappedDescendants() {
		Tree root = TreeIoUtils.fromXml(DUMMY_DST);
		root.getChildren().get(0).setMatched(true);
		root.getChildren().get(0).getChildren().get(0).getChildren().get(0).setMatched(true);
		root.getChildren().get(1).setMatched(true);
		root.getChildren().get(1).getChildren().get(0).setMatched(true);
		root = TreeUtils.removeCompletelyMapped(root);
		TreeUtils.computeSize(root);
		assertTrue(root.getSize() == 5);
	}

}
