package fr.labri.gumtree.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


public final class TreeUtils {

	private TreeUtils() {
	}

	public static void computeAllMetrics(Tree tree) {
		computeSize(tree);
		computeDepth(tree);
		computeHeight(tree);
		computeDigest(tree);
	}

	/**
	 * Compute the depth of every node of the tree. The size is set
	 * directly on the nodes and is then accessible using {@link Tree#getSize()}.
	 * @param tree a Tree
	 */
	public static void computeSize(Tree tree) {
		List<Tree> trees = postOrder(tree);
		for (Tree t: trees) {
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
		List<Tree> trees = postOrder(tree);
		for (Tree t: trees) {
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

	public static void order(Tree tree) {
		order(tree, OrderKind.POST_ORDER);
	}
	
	public static void order(Tree tree, OrderKind kind) {
		switch(kind) {
		case PRE_ORDER:
			preOrderNumbering(tree);
			return;
		case POST_ORDER:
			postOrderNumbering(tree);
			return;
		case BFS_ORDER:
			bfsOrderNumbering(tree);
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
	public static List<Tree> bfsOrder(Tree tree) {
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

	public static void bfsOrderNumbering(Tree tree) {
		List<Tree> trees = bfsOrder(tree);
		for (int i = 0; i < trees.size(); i++) trees.get(i).setId(i);
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

	public static void postOrderNumbering(Tree tree) {
		List<Tree> trees = postOrder(tree);
		for (int i = 0; i < trees.size(); i++) trees.get(i).setId(i);
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
	public static Tree removeMapped(Tree tree) {
		for (Tree t: tree.getTrees()) {
			if (t.isMatched()) {
				if (t.getParent() != null) t.getParent().getChildren().remove(t);
				t.setParent(null);
			}
		}
		tree.refreshMetrics();
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

	public enum OrderKind {
		PRE_ORDER,
		POST_ORDER,
		BFS_ORDER;
	}

}
