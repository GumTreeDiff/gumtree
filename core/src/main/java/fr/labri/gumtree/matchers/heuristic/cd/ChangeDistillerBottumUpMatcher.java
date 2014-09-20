package fr.labri.gumtree.matchers.heuristic.cd;

import java.util.List;

import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.MatcherFactory;
import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.TreeUtils;

public class ChangeDistillerBottumUpMatcher extends Matcher {

	public static final double STRUCT_SIM_THRESHOLD_1 = 0.6D;

	public static final double STRUCT_SIM_THRESHOLD_2 = 0.4D;

	public ChangeDistillerBottumUpMatcher(ITree src, ITree dst) {
		super(src, dst);
	}

	@Override
	public void match() {
		List<ITree> poDst = TreeUtils.postOrder(dst);
		for (ITree src: src.postOrder()) {
			int l = numberOfLeafs(src);
			for (ITree dst: poDst) {
				if (src.isMatchable(dst) && !(src.isLeaf() || dst.isLeaf())) {
					double sim = chawatheSimilarity(src, dst);
					if ((l > 4 && sim >= STRUCT_SIM_THRESHOLD_1) || (l <= 4 && sim >= STRUCT_SIM_THRESHOLD_2)) {
						addMapping(src, dst);
						break;
					}
				}
			}
		}
	}
	
	private int numberOfLeafs(ITree root) {
		int l = 0;
		for (ITree t : root.getDescendants()) if (t.isLeaf()) l++;
		return l;
	}
	
	public static class ChangeDistillerBottomUpMatcherFactory implements MatcherFactory {

		@Override
		public Matcher newMatcher(ITree src, ITree dst) {
			return new ChangeDistillerBottumUpMatcher(src, dst);
		}
		
	}

}
