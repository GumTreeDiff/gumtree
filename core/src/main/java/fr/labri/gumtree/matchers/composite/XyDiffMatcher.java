package fr.labri.gumtree.matchers.composite;

import fr.labri.gumtree.matchers.heuristic.*;
import fr.labri.gumtree.tree.Tree;
import fr.labri.gumtree.tree.TreeUtils;

public class XyDiffMatcher extends Matcher {

	public XyDiffMatcher(Tree src, Tree dst) {
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
		new XyBottomUpMatcher(src, dst, this.getMappings());
		toc = System.currentTimeMillis();
		long t2 = toc - tic;
		LOGGER.fine(String.format("GumTree matching performed. GreedySubTreeMatcher: %d ms, XyBottomUpMatcher: %d ms", t1, t2));
	}
	
	public static Tree prepare(Tree tree) {
		TreeUtils.postOrderNumbering(tree);
		TreeUtils.computeHeight(tree);
		TreeUtils.computeDigest(tree);
		return tree;
	}

}
