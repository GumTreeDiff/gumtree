package fr.labri.gumtree.test;

import static fr.labri.gumtree.test.Constants.DUMMY_SRC;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.TreeUtils;

public class TestTree {

	@Test
	public void testIdComparator() {
		ITree root = TreeIoUtils.fromXml(getClass().getResourceAsStream(DUMMY_SRC)).getRoot();
		List<ITree> nodes = root.getTrees();
		assertTrue(nodes.get(0).getLabel().equals("a"));
		assertTrue(nodes.get(1).getLabel().equals("b"));
		assertTrue(nodes.get(2).getLabel().equals("c"));
		assertTrue(nodes.get(3).getLabel().equals("d"));
		assertTrue(nodes.get(4).getLabel().equals("e"));
	}

	@Test
	public void testGetParents() {
		ITree tree = TreeIoUtils.fromXml(getClass().getResourceAsStream(DUMMY_SRC)).getRoot();
		List<ITree> trees = new LinkedList<>(tree.getTrees());
		ITree n = trees.get(2);
		assertTrue(n.getLabel().equals("c"));
		List<ITree> parents = n.getParents();
		assertTrue(parents.size() == 2);
		assertTrue(parents.get(0).getLabel().equals("b"));
		assertTrue(parents.get(1).getLabel().equals("a"));
	}
	
	@Test
	public void testDeepCopy() {
		ITree root = TreeIoUtils.fromXml(getClass().getResourceAsStream(DUMMY_SRC)).getRoot();
		TreeUtils.postOrderNumbering(root);
		ITree croot = root.deepCopy();
		assertTrue(croot.getSize() == root.getSize());
		root.setLabel("new");
		root.getChildren().get(0).setLabel("new");
		root.getChildren().get(0).getChildren().get(0).setLabel("new");
		assertTrue(croot.getLabel().equals("a"));
		assertTrue(croot.getChildren().get(0).getLabel().equals("b"));
		assertTrue(croot.getChildren().get(0).getChildren().get(0).getLabel().equals("c"));
		assertTrue(root.getLabel().equals("new"));
		assertTrue(root.getChildren().get(0).getLabel().equals("new"));
		assertTrue(root.getChildren().get(0).getChildren().get(0).getLabel().equals("new"));
	}
	
	@Test
	public void testIsClone() {
		ITree tree = TreeIoUtils.fromXml(getClass().getResourceAsStream(DUMMY_SRC)).getRoot();
		ITree copy = tree.deepCopy();
		assertTrue(tree.isClone(copy));
	}

}
