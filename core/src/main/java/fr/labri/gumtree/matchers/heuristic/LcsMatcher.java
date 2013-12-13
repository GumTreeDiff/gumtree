package fr.labri.gumtree.matchers.heuristic;

import java.util.List;

import fr.labri.gumtree.algo.StringAlgorithms;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.tree.Tree;
import fr.labri.gumtree.tree.TreeUtils;

public class LcsMatcher extends Matcher {

	public LcsMatcher(Tree src, Tree dst) {
		super(src, dst);
	}

	@Override
	public void match() {
		List<Tree> srcSeq = TreeUtils.preOrder(src);
		List<Tree> dstSeq = TreeUtils.preOrder(dst);
		List<int[]> lcs = StringAlgorithms.lcss(srcSeq, dstSeq);
		System.out.println(lcs.size());
		for (int[] x: lcs) {

			Tree t1 = srcSeq.get(x[0]);
			Tree t2 = dstSeq.get(x[1]);
			addMapping(t1, t2);
		}
	}

}
