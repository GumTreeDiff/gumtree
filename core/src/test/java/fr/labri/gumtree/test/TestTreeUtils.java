package fr.labri.gumtree.test;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.TreeUtils;

public class TestTreeUtils {
	
	@Test
	public void testPostOrderNumbering() {
		ITree root = TreeLoader.getDummySrc();
		TreeUtils.postOrderNumbering(root);
		assertEquals(4, root.getId());
		assertEquals(2, root.getChildren().get(0).getId());
		assertEquals(0, root.getChildren().get(0).getChildren().get(0).getId());
		assertEquals(1, root.getChildren().get(0).getChildren().get(1).getId());
		assertEquals(3, root.getChildren().get(1).getId());
	}
	
	@Test
	public void testDepth() {
		ITree root = TreeLoader.getDummySrc();
		TreeUtils.computeDepth(root);
		assertEquals(0, root.getDepth());
		assertEquals(1, root.getChildren().get(0).getDepth());
		assertEquals(2, root.getChildren().get(0).getChildren().get(0).getDepth());
		assertEquals(2, root.getChildren().get(0).getChildren().get(1).getDepth());
		assertEquals(1, root.getChildren().get(1).getDepth());
	}
	
	@Test
	public void testHeight() {
		ITree root = TreeLoader.getDummySrc();
		assertEquals(2, root.getHeight()); // depth of a 
		assertEquals(1, root.getChildren().get(0).getHeight()); // depth of b
		assertEquals(0, root.getChildren().get(0).getChildren().get(0).getHeight()); // depth of c
		assertEquals(0, root.getChildren().get(0).getChildren().get(1).getHeight()); // depth of d
		assertEquals(0, root.getChildren().get(1).getHeight()); // depth of e
	}
	
	@Test
	public void testPreOrderNumbering() {
		ITree root = TreeLoader.getDummySrc();
		TreeUtils.preOrderNumbering(root);
		assertEquals(0, root.getId()); // id of a
		assertEquals(1, root.getChildren().get(0).getId()); // id of b
		assertEquals(2, root.getChildren().get(0).getChildren().get(0).getId()); // id of c
		assertEquals(3, root.getChildren().get(0).getChildren().get(1).getId()); // id of d
		assertEquals(4, root.getChildren().get(1).getId()); // id of e
	}
	
	@Test
	public void testBreadthFirstNumbering() {
		ITree root = TreeLoader.getDummySrc();
		TreeUtils.breadthFirstNumbering(root);
		assertEquals(0, root.getId());
		assertEquals(1, root.getChildren().get(0).getId());
		assertEquals(2, root.getChildren().get(1).getId());
		assertEquals(3, root.getChildren().get(0).getChildren().get(0).getId());
		assertEquals(4, root.getChildren().get(0).getChildren().get(1).getId());
	}
	
	@Test
	public void testRemoveCompletelyMappedDescendants() {
		ITree dst = TreeLoader.getDummyDst();
		dst.getChildren().get(0).setMatched(true);
		dst.getChildren().get(0).getChildren().get(0).getChildren().get(0).setMatched(true);
		dst.getChildren().get(1).setMatched(true);
		dst.getChildren().get(1).getChildren().get(0).setMatched(true);
		dst = TreeUtils.removeCompletelyMapped(dst);
		TreeUtils.computeSize(dst);
		assertTrue(dst.getSize() == 5);
	}
	
	@Test
	public void testPostOrder() {
		ITree src = TreeLoader.getDummySrc();
		List<ITree> lst = TreeUtils.postOrder(src);
		Iterator<ITree> it = TreeUtils.postOrderIterator(src);
		compareListIterator(lst, it);
	}
	
	@Test
	public void testPostOrder2() {
		ITree dst = TreeLoader.getDummyDst();
		List<ITree> lst = TreeUtils.postOrder(dst);
		Iterator<ITree> it = TreeUtils.postOrderIterator(dst);
		compareListIterator(lst, it);
	}
	
	@Test
	public void testPostOrder3() {
		ITree big = TreeLoader.getDummyBig();
		List<ITree> lst = TreeUtils.postOrder(big);
		Iterator<ITree> it = TreeUtils.postOrderIterator(big);
		compareListIterator(lst, it);
	}
	
	@Test
	public void testBFS() {
		ITree src = TreeLoader.getDummySrc();
		List<ITree> lst = TreeUtils.breadthFirst(src);
		Iterator<ITree> it = TreeUtils.breadthFirstIterator(src);
		compareListIterator(lst, it);
	}
	
	
	@Test
	public void testBFSList() {
		ITree src = TreeLoader.getDummySrc();
		ITree dst = TreeLoader.getDummyDst();
		ITree big = TreeLoader.getDummyBig();
		compareListIterator(TreeUtils.breadthFirstIterator(src), "a", "b", "e", "c", "d");
		compareListIterator(TreeUtils.breadthFirstIterator(dst), "a", "f", "i", "b", "j", "c", "d", "h");
		compareListIterator(TreeUtils.breadthFirstIterator(big), "a", "b", "e", "f", "c", "d", "g", "l", "h", "m", "i", "j", "k");
	}
	
	@Test
	public void testPreOrderList() {
		ITree src = TreeLoader.getDummySrc();
		ITree dst = TreeLoader.getDummyDst();
		ITree big = TreeLoader.getDummyBig();
		compareListIterator(TreeUtils.preOrderIterator(src), "a", "b", "c", "d", "e");
		compareListIterator(TreeUtils.preOrderIterator(dst), "a", "f", "b", "c", "d", "h", "i", "j");
		compareListIterator(TreeUtils.preOrderIterator(big), "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m");
	}
	
	void compareListIterator(List<ITree> lst, Iterator<ITree> it) {
		for (ITree i: lst) {
			assertEquals(i, it.next());
		}
		assertFalse(it.hasNext());
	}
	
	void compareListIterator(Iterator<ITree> it, String... expected) {
		for(String e: expected) {
			ITree n = it.next();
			assertEquals(e, n.getLabel());
		}
		assertFalse("Iterator has next", it.hasNext());
	}
	
	@Test
	public void testBFS2() {
		ITree dst = TreeLoader.getDummyDst();
		List<ITree> lst = TreeUtils.breadthFirst(dst);
		Iterator<ITree> it = TreeUtils.breadthFirstIterator(dst);
		compareListIterator(lst, it);
	}
	
	@Test
	public void testBFS3() {
		ITree big = TreeLoader.getDummySrc();
		List<ITree> lst = TreeUtils.breadthFirst(big);
		Iterator<ITree> it = TreeUtils.breadthFirstIterator(big);
		compareListIterator(lst, it);
	}
}
