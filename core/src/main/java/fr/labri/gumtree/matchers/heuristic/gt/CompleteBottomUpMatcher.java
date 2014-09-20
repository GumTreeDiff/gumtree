package fr.labri.gumtree.matchers.heuristic.gt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.labri.gumtree.matchers.Mapping;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.MatcherFactory;
import fr.labri.gumtree.matchers.optimal.rted.RtedMatcher;
import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.TreeMap;
import fr.labri.gumtree.tree.TreeUtils;

/**
 * Match the nodes using a bottom-up approach. It browse the nodes of the source and destination trees
 * using a post-order traversal, testing if the two selected trees might be mapped. The two trees are mapped 
 * if they are mappable and have a dice coefficient greater than SIM_THRESHOLD. Whenever two trees are mapped
 * a exact ZS algorithm is applied to look to possibly forgotten nodes.
 */
public class CompleteBottomUpMatcher extends Matcher {

	private static final double SIM_THRESHOLD = 0.50D;
	
	private static final int SIZE_THESHOLD = 100;

	private TreeMap srcIds;
	
	private TreeMap dstIds;

	public CompleteBottomUpMatcher(ITree src, ITree dst) {
		super(src, dst);
	}

	public void match() {
		srcIds = new TreeMap(src);
		dstIds = new TreeMap(dst);
		
		for (ITree t: src.postOrder())  {
			if (t.isRoot()) {
				addMapping(t, this.dst);
				lastChanceMatch(t, this.dst);
				break;
			} else if (!(t.isMatched() || t.isLeaf())) {
				List<ITree> candidates = getDstCandidates(t);
				ITree best = null;
				double max = -1D;
				
				for (ITree cand: candidates) {
					double sim = jaccardSimilarity(t, cand);
					if (sim > max && sim >= SIM_THRESHOLD) {
						max = sim;
						best = cand;
					}
				}

				if (best != null) {
					lastChanceMatch(t, best);
					addMapping(t, best);
				}
			}
		}
		clean();
	}

	private List<ITree> getDstCandidates(ITree src) {
		List<ITree> seeds = new ArrayList<>();
		for (ITree c: src.getDescendants()) {
			ITree m = mappings.getDst(c);
			if (m != null) seeds.add(m);
		}
		List<ITree> candidates = new ArrayList<>();
		Set<ITree> visited = new HashSet<>();
		for(ITree seed: seeds) {
			while (seed.getParent() != null) {
				ITree parent = seed.getParent();
				if (visited.contains(parent)) break;
				visited.add(parent);
				if (parent.getType() == src.getType() && !parent.isMatched() && !parent.isRoot()) candidates.add(parent);
				seed = parent;
			}
		}

		return candidates;
	}

	//FIXME checks if it is better or not to remove the already found mappings.
	private void lastChanceMatch(ITree src, ITree dst) {
		ITree cSrc = src.deepCopy();
		ITree cDst = dst.deepCopy();
		TreeUtils.removeMatched(cSrc);
		TreeUtils.removeMatched(cDst);

		if (cSrc.getSize() < SIZE_THESHOLD || cDst.getSize() < SIZE_THESHOLD) {
			Matcher m = new RtedMatcher(cSrc, cDst);
			m.match();
			for (Mapping candidate: m.getMappings()) {
				ITree left = srcIds.getTree(candidate.getFirst().getId());
				ITree right = dstIds.getTree(candidate.getSecond().getId());

				if (left.getId() == src.getId() || right.getId() == dst.getId()) {
					//System.err.println("Trying to map already mapped source node.");
					continue;
				} else if (!left.isMatchable(right)) {
					//System.err.println("Trying to map not compatible nodes.");
					continue;
				} else if (left.getParent().getType() != right.getParent().getType()) {
					//System.err.println("Trying to map nodes with incompatible parents");
					continue;
				} else addMapping(left, right);
			}
		}
		
		for (ITree t : src.getTrees()) t.setMatched(true);
		for (ITree t : dst.getTrees()) t.setMatched(true);
	}
	
	public static class CompleteBottumUpMatcherFactory implements MatcherFactory {

		@Override
		public Matcher newMatcher(ITree src, ITree dst) {
			return new CompleteBottomUpMatcher(src, dst);
		}
	}
}
