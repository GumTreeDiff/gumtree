package fr.labri.gumtree.matchers.composite;

import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.heuristic.*;
import fr.labri.gumtree.tree.Tree;
import fr.labri.gumtree.tree.TreeUtils;

public class GumTreeMatcher extends Matcher {

	public GumTreeMatcher(Tree src, Tree dst) {
		super(prepare(src), prepare(dst));
		match();
	}
	
	@Override
	public void match() {
		long tic, toc;
		tic = System.currentTimeMillis();
		new GreedySubTreeMatcher(src, dst, this.getMappings());
		toc = System.currentTimeMillis();
		long t1 = toc - tic;
		tic = System.currentTimeMillis();
		new CompleteBottomUpMatcher(src, dst, this.getMappings());
		toc = System.currentTimeMillis();
		long t2 = toc - tic;
		LOGGER.fine(String.format("GumTree matching performed. SubTreeMatcher: %d ms, BottumUpMatcher: %d ms", t1, t2));
	}
	
	public static Tree prepare(Tree tree) {
		TreeUtils.postOrderNumbering(tree);
		TreeUtils.computeHeight(tree);
		TreeUtils.computeDigest(tree);
		return tree;
	}

}
