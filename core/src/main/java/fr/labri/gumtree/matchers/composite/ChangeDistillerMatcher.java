package fr.labri.gumtree.matchers.composite;

import java.util.List;

import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.heuristic.ChangeDistillerLeavesMatcher;
import fr.labri.gumtree.tree.Tree;
import fr.labri.gumtree.tree.TreeUtils;

public class ChangeDistillerMatcher extends Matcher {

	public static final double STRUCT_SIM_THRESHOLD_1 = 0.6D;

	public static final double STRUCT_SIM_THRESHOLD_2 = 0.4D;

	public ChangeDistillerMatcher(Tree src, Tree dst) {
		super(prepare(src), prepare(dst));
		match();
	}

	@Override
	public void match() {
		Matcher m = new ChangeDistillerLeavesMatcher(src, dst, this.getMappings());
		m.match();
		
		List<Tree> poSrc = TreeUtils.postOrder(src);
		List<Tree> poDst = TreeUtils.postOrder(dst);
		for (Tree src: poSrc) {
			int l = numberOfLeafs(src);
			for (Tree dst: poDst) {
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
	
	private static Tree prepare(Tree tree) {
		TreeUtils.postOrderNumbering(tree);
		TreeUtils.computeHeight(tree);
		TreeUtils.computeDigest(tree);
		return tree;
	}
	
	private int numberOfLeafs(Tree root) {
		int l = 0;
		for (Tree t : root.getDescendants()) if (t.isLeaf()) l++;
		return l;
	}

}
