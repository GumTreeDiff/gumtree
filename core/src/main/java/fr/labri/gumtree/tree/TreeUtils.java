package fr.labri.gumtree.tree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import fr.labri.gumtree.matchers.Mapping;


public final class TreeUtils {

	private TreeUtils() {
	}

	/**
	 * Compute the depth of every node of the tree. The size is set
	 * directly on the nodes and is then accessible using {@link Tree#getSize()}.
	 * @param tree a Tree
	 */
	public static void computeSize(ITree tree) {
		for (ITree t: tree.postOrder()) {
			int size = 1;
			if (!t.isLeaf()) for (ITree c: t.getChildren()) size += c.getSize();
			t.setSize(size);
		}
	}

	/**
	 * Compute the digest of every node of the tree. The digest is set 
	 * directly on the nodes and is then accessible using {@link Tree#getDigest()}.
	 * @param tree a Tree
	 */
	public static void computeDigest(ITree tree) {
		DigestGenerator.RollingMd5HashGenerator g = new DigestGenerator.RollingMd5HashGenerator();
		g.computeDigest(tree);
	}

	public static void computeDigest(ITree tree, DigestGenerator g) {
		g.computeDigest(tree);
	}

	/**
	 * Compute the depth of every node of the tree. The depth is set 
	 * directly on the nodes and is then accessible using {@link Tree#getDepth()}.
	 * @param tree a Tree
	 */
	public static void computeDepth(ITree tree) {
		List<ITree> trees = preOrder(tree);
		for (ITree t: trees) {
			int depth = 0;
			if (!t.isRoot()) depth = t.getParent().getDepth() + 1;
			t.setDepth(depth);
		}
	}

	/**
	 * Compute the height of every node of the tree. The height is set 
	 * directly on the nodes and is then accessible using {@link Tree#getHeight()}.
	 * @param tree a Tree.
	 */
	public static void computeHeight(ITree tree) {
		for (ITree t: tree.postOrder()) {
			int height = 0;
			if (!t.isLeaf()) {
				for (ITree c: t.getChildren()) {
					int cHeight = c.getHeight();
					if (cHeight > height) height = cHeight;
				}
				height++;
			}
			t.setHeight(height);
		}
	}

	/**
	 * Returns a list of every subtrees and the tree ordered using a pre-order.
	 * @param tree a Tree.
	 * @return
	 */
	public static List<ITree> preOrder(ITree tree) {
		List<ITree> trees = new ArrayList<>();
		preOrder(tree, trees);
		return trees;
	}

	private static void preOrder(ITree tree, List<ITree> trees) {
		trees.add(tree);
		if (!tree.isLeaf())
			for (ITree c: tree.getChildren())
				preOrder(c, trees);
	}

	public static void preOrderNumbering(ITree tree) {
		List<ITree> trees = preOrder(tree);
		for (int i = 0; i < trees.size(); i++)
			trees.get(i).setId(i);
	}

	/**
	 * Returns a list of every subtrees and the tree ordered using a breadth-first order.
	 * @param tree a Tree.
	 * @return
	 */
	public static List<ITree> breadthFirst(ITree tree) {
		List<ITree> trees = new ArrayList<>();
		List<ITree> currents = new ArrayList<>();
		currents.add(tree);
		while (currents.size() > 0) {
			ITree c = currents.remove(0);
			trees.add(c);
			currents.addAll(c.getChildren());
		}
		return trees;
	}
	
	private static ITree fakeTree(ITree tree) {
		ITree t = new Tree(-1);
		t.getChildren().add(tree);
		return t;
	}
	
	public static Iterator<ITree> breadthFirstIterator(final ITree tree) {
		return new Iterator<ITree>() {
			Deque<Iterator<ITree>> fifo = new ArrayDeque<>();
			
			{
				addLasts(fakeTree(tree));
			}
			
			@Override
			public boolean hasNext() {
				return !fifo.isEmpty();
			}
			
			@Override
			public ITree next() {
				while (!fifo.isEmpty()) {
					Iterator<ITree> it = fifo.getFirst();
					if (it.hasNext()) {
						ITree item = it.next();
						if (!it.hasNext())
							fifo.removeFirst();
						addLasts(item);
						return item;
					}
				}
				throw new NoSuchElementException();
			}
			private void addLasts(ITree item) {
				List<ITree> children = item.getChildren();
				if (!children.isEmpty())
					fifo.addLast(children.iterator());
			}
			@Override
			public void remove() {
				throw new RuntimeException("Not yet implemented implemented.");
			}
		};
	}
	
	public static void breadthFirstNumbering(ITree tree) {
		numbering(tree.breadthFirst());
	}
	
	public static void numbering(Iterable<ITree> iterable) {
		int i = 0;
		for (ITree t: iterable)
			t.setId(i++);
	}

	/**
	 * Returns a list of every subtrees and the tree ordered using a post-order.
	 * @param tree a Tree.
	 * @return
	 */
	public static List<ITree> postOrder(ITree tree) {
		List<ITree> trees = new ArrayList<>();
		postOrder(tree, trees);
		return trees;
	}

	private static void postOrder(ITree tree, List<ITree> trees) {
		if (!tree.isLeaf()) for (ITree c: tree.getChildren()) postOrder(c, trees);
		trees.add(tree);
	}
	
	public static Iterator<ITree> postOrderIterator(final ITree tree) {
		return new Iterator<ITree>() {
			Deque<Pair<ITree, Iterator<ITree>>> stack = new ArrayDeque<>();
			{
				push(tree);
			}
			
			@Override
			public boolean hasNext() {
				return stack.size() > 0;
			}

			@Override
			public ITree next() {
				if (stack.isEmpty())
					throw new NoSuchElementException();
				return selectNextChild(stack.peek().getSecond());
			}
			
			ITree selectNextChild(Iterator<ITree> it) {
				if (!it.hasNext())
					return stack.pop().getFirst();
				ITree item = it.next();
				if (item.isLeaf())
					return item;
				return selectNextChild(push(item));
			}

			private Iterator<ITree> push(ITree item) {
				Iterator<ITree> it = item.getChildren().iterator();
				stack.push(new Pair<>(item, it));
				return it;
			}

			@Override
			public void remove() {
				throw new RuntimeException("Not yet implemented implemented.");
			}
		};
	}
	
	public static Iterator<ITree> leafIterator(final Iterator<ITree> it) {
		return new Iterator<ITree>() {
			ITree current = it.hasNext() ? it.next() : null;
			@Override
			public boolean hasNext() {
				return current != null;
			}

			@Override
			public ITree next() {
				ITree val = current;
				while (it.hasNext()) {
					current = it.next();
					if (current.isLeaf())
						break;
				}
				return val;
			}

			@Override
			public void remove() {
				throw new RuntimeException("Not yet implemented implemented.");
			}
		};
	}
	
	public static void postOrderNumbering(ITree tree) {
		numbering(tree.postOrder());
	}

	public static void removeMapped(Collection<? extends Mapping> mappings) {
		Iterator<? extends Mapping> trIt = mappings.iterator();
		while (trIt.hasNext()) {
			Mapping t = trIt.next();
			if (t.getFirst().isMatched() || t.getSecond().isMatched()) trIt.remove();
		}
	}

	public static List<ITree> removeMapped(List<ITree> trees) {
		Iterator<ITree> trIt = trees.iterator();
		while (trIt.hasNext()) {
			ITree t = trIt.next();
			if (t.isMatched()) trIt.remove();
		}
		return trees;
	}

	/**
	 * Remove mapped nodes from the tree. Be careful this method will invalidate
	 * all the metrics of this tree and its descendants. If you need them, you need
	 * to recompute them.
	 * @param tree 
	 * @return
	 */
	public static ITree removeMatched(ITree tree) {
		for (ITree t: tree.getTrees()) {
			if (t.isMatched()) {
				if (t.getParent() != null) t.getParent().getChildren().remove(t);
				t.setParent(null);
			}
		}
		tree.refresh();
		return tree;
	}

	/**
	 * Remove mapped nodes from the tree. Be careful this method will invalidate
	 * all the metrics of this tree and its descendants. If you need them, you need
	 * to recompute them.
	 * @param tree 
	 * @return
	 */
	public static ITree removeCompletelyMapped(ITree tree) {
		for (ITree t: tree.getTrees()) {
			if (t.isMatched() && t.areDescendantsMatched()) {
				t.getParent().getChildren().remove(t);
				t.setParent(null);
			}
		}
		return tree;
	}
}
