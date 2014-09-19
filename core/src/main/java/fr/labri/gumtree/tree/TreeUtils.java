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
	public static void computeSize(Tree tree) {
		for (Tree t: tree.postOrder()) {
			int size = 1;
			if (!t.isLeaf()) for (Tree c: t.getChildren()) size += c.getSize();
			t.setSize(size);
		}
	}

	/**
	 * Compute the digest of every node of the tree. The digest is set 
	 * directly on the nodes and is then accessible using {@link Tree#getDigest()}.
	 * @param tree a Tree
	 */
	public static void computeDigest(Tree tree) {
		DigestGenerator.RollingMd5HashGenerator g = new DigestGenerator.RollingMd5HashGenerator();
		g.computeDigest(tree);
	}

	public static void computeDigest(Tree tree, DigestGenerator g) {
		g.computeDigest(tree);
	}

	/**
	 * Compute the depth of every node of the tree. The depth is set 
	 * directly on the nodes and is then accessible using {@link Tree#getDepth()}.
	 * @param tree a Tree
	 */
	public static void computeDepth(Tree tree) {
		List<Tree> trees = preOrder(tree);
		for (Tree t: trees) {
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
	public static void computeHeight(Tree tree) {
		for (Tree t: tree.postOrder()) {
			int height = 0;
			if (!t.isLeaf()) {
				for (Tree c: t.getChildren()) {
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
	public static List<Tree> preOrder(Tree tree) {
		List<Tree> trees = new ArrayList<>();
		preOrder(tree, trees);
		return trees;
	}

	private static void preOrder(Tree tree, List<Tree> trees) {
		trees.add(tree);
		if (!tree.isLeaf()) for (Tree c: tree.getChildren()) preOrder(c, trees);
	}

	public static void preOrderNumbering(Tree tree) {
		List<Tree> trees = preOrder(tree);
		for (int i = 0; i < trees.size(); i++) trees.get(i).setId(i);
	}

	/**
	 * Returns a list of every subtrees and the tree ordered using a breadth-first order.
	 * @param tree a Tree.
	 * @return
	 */
	public static List<Tree> breadthFirst(Tree tree) {
		List<Tree> trees = new ArrayList<>();
		List<Tree> currents = new ArrayList<>();
		currents.add(tree);
		while (currents.size() > 0) {
			Tree c = currents.remove(0);
			trees.add(c);
			currents.addAll(c.getChildren());
		}
		return trees;
	}
	
	private static Tree fakeTree(Tree tree) {
		Tree t = new Tree(-1);
		t.getChildren().add(tree);
		return t;
	}
	
	public static Iterator<Tree> breadthFirstIterator(final Tree tree) {
		return new Iterator<Tree>() {
			Deque<Iterator<Tree>> fifo = new ArrayDeque<>();
			{
				addLasts(fakeTree(tree));
			}
			
			@Override
			public boolean hasNext() {
				return !fifo.isEmpty();
			}
			
			@Override
			public Tree next() {
				while (!fifo.isEmpty()) {
					Iterator<Tree> it = fifo.getFirst();
					if (it.hasNext()) {
						Tree item = it.next();
						if (!it.hasNext())
							fifo.removeFirst();
						addLasts(item);
						return item;
					}
				}
				throw new NoSuchElementException();
			}
			private void addLasts(Tree item) {
				List<Tree> children = item.getChildren();
				if (!children.isEmpty())
					fifo.addLast(children.iterator());
			}
			@Override
			public void remove() {
				throw new RuntimeException("Not yet implemented implemented.");
			}
		};
	}
	
	public static void breadthFirstNumbering(Tree tree) {
		numbering(tree.breadthFirst());
	}
	
	public static void numbering(Iterable<Tree> iterable) {
		int i = 0;
		for (Tree t: iterable)
			t.setId(i++);
	}

	/**
	 * Returns a list of every subtrees and the tree ordered using a post-order.
	 * @param tree a Tree.
	 * @return
	 */
	public static List<Tree> postOrder(Tree tree) {
		List<Tree> trees = new ArrayList<>();
		postOrder(tree, trees);
		return trees;
	}

	private static void postOrder(Tree tree, List<Tree> trees) {
		if (!tree.isLeaf()) for (Tree c: tree.getChildren()) postOrder(c, trees);
		trees.add(tree);
	}
	
	public static Iterator<Tree> postOrderIterator(final Tree tree) {
		return new Iterator<Tree>() {
			Deque<Pair<Tree, Iterator<Tree>>> stack = new ArrayDeque<>();
			{
				push(tree);
			}
			
			@Override
			public boolean hasNext() {
				return stack.size() > 0;
			}

			@Override
			public Tree next() {
				if (stack.isEmpty())
					throw new NoSuchElementException();
				return selectNextChild(stack.peek().getSecond());
			}
			
			Tree selectNextChild(Iterator<Tree> it) {
				if (!it.hasNext())
					return stack.pop().getFirst();
				Tree item = it.next();
				if (item.isLeaf())
					return item;
				return selectNextChild(push(item));
			}

			private Iterator<Tree> push(Tree item) {
				Iterator<Tree> it = item.getChildren().iterator();
				stack.push(new Pair<>(item, it));
				return it;
			}

			@Override
			public void remove() {
				throw new RuntimeException("Not yet implemented implemented.");
			}
		};
	}
	
	public static Iterator<Tree> leafIterator(final Iterator<Tree> it) {
		return new Iterator<Tree>() {
			Tree current = it.hasNext() ? it.next() : null;
			@Override
			public boolean hasNext() {
				return current != null;
			}

			@Override
			public Tree next() {
				Tree val = current;
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
	
	public static void postOrderNumbering(Tree tree) {
		numbering(tree.postOrder());
	}

	public static void removeMapped(Collection<? extends Mapping> mappings) {
		Iterator<? extends Mapping> trIt = mappings.iterator();
		while (trIt.hasNext()) {
			Mapping t = trIt.next();
			if (t.getFirst().isMatched() || t.getSecond().isMatched()) trIt.remove();
		}
	}

	public static List<Tree> removeMapped(List<Tree> trees) {
		Iterator<Tree> trIt = trees.iterator();
		while (trIt.hasNext()) {
			Tree t = trIt.next();
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
	public static Tree removeMatched(Tree tree) {
		for (Tree t: tree.getTrees()) {
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
	public static Tree removeCompletelyMapped(Tree tree) {
		for (Tree t: tree.getTrees()) {
			if (t.isMatched() && t.areDescendantsMatched()) {
				t.getParent().getChildren().remove(t);
				t.setParent(null);
			}
		}
		return tree;
	}
}
