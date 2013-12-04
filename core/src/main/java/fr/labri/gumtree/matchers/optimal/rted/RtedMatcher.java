package fr.labri.gumtree.matchers.optimal.rted;

import java.util.List;

import fr.labri.gumtree.matchers.composite.Matcher;
import fr.labri.gumtree.tree.Tree;
import fr.labri.gumtree.tree.TreeUtils;

public class RtedMatcher extends Matcher {

	public RtedMatcher(Tree src, Tree dst) {
		super(src, dst);
		match();
	}

	@Override
	public void match() {
		RtedAlgorithm a = new RtedAlgorithm(1D, 1D, 1D);
		a.init(src, dst);
		a.computeOptimalStrategy();
		a.nonNormalizedTreeDist();
		List<int[]> arrayMappings = a.computeEditMapping();
		List<Tree> srcs = TreeUtils.postOrder(src);
		List<Tree> dsts = TreeUtils.postOrder(dst);
		for (int[] m: arrayMappings) if (m[0] != 0 && m[1] != 0) addMapping(srcs.get(m[0] - 1), dsts.get(m[1] - 1));
	}

}
