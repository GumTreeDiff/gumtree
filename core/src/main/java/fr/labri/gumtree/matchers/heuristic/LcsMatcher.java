package fr.labri.gumtree.matchers.heuristic;

import java.util.List;

import fr.labri.gumtree.algo.StringAlgorithms;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.TreeUtils;

public class LcsMatcher extends Matcher {

	public LcsMatcher(ITree src, ITree dst) {
		super(src, dst);
	}

	@Override
	public void match() {
		List<ITree> srcSeq = TreeUtils.preOrder(src);
		List<ITree> dstSeq = TreeUtils.preOrder(dst);
		List<int[]> lcs = StringAlgorithms.lcss(srcSeq, dstSeq);
		System.out.println(lcs.size());
		for (int[] x: lcs) {

			ITree t1 = srcSeq.get(x[0]);
			ITree t2 = dstSeq.get(x[1]);
			addMapping(t1, t2);
		}
	}

}
