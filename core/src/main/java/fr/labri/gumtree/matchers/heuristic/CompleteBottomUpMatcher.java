package fr.labri.gumtree.matchers.heuristic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.labri.gumtree.matchers.Mapping;
import fr.labri.gumtree.matchers.MappingStore;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.optimal.rted.RtedMatcher;
import fr.labri.gumtree.tree.Tree;
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

	public CompleteBottomUpMatcher(Tree src, Tree dst, MappingStore mappings) {
		super(src, dst, mappings);
		match();
	}

	public void match() {
		srcIds = new TreeMap(src);
		dstIds = new TreeMap(dst);
		match(TreeUtils.postOrder(src), TreeUtils.postOrder(dst));
		clean();
	}

	private void match(List<Tree> srcs, List<Tree> dsts) {
		for (Tree src: srcs)  {
			if (src.isRoot()) {
				addMapping(src, this.dst);
				lastChanceMatch(src, this.dst);
				break;
			} else if (!(src.isMatched() || src.isLeaf())) {
				List<Tree> candidates = getDstCandidates(src);
				Tree best = null;
				double max = -1D;
				
				for (Tree cand: candidates ) {
					double sim = jaccardSimilarity(src, cand);
					if (sim > max && sim >= SIM_THRESHOLD) {
						max = sim;
						best = cand;
					}
				}

				if (best != null) {
					lastChanceMatch(src, best);
					addMapping(src, best);
				}
			}
		}
	}

	private List<Tree> getDstCandidates(Tree src) {
		List<Tree> seeds = new ArrayList<>();
		for (Tree c: src.getDescendants()) {
			Tree m = mappings.getDst(c);
			if (m != null) seeds.add(m);
		}
		List<Tree> candidates = new ArrayList<>();
		Set<Tree> visited = new HashSet<>();
		for(Tree seed: seeds) {
			while (seed.getParent() != null) {
				Tree parent = seed.getParent();
				if (visited.contains(parent)) break;
				visited.add(parent);
				if (parent.getType() == src.getType() && !parent.isMatched() && !parent.isRoot()) candidates.add(parent);
				seed = parent;
			}
		}

		return candidates;
	}

	//FIXME checks if it is better or not to remove the already found mappings.
	private void lastChanceMatch(Tree src, Tree dst) {
		Tree cSrc = src.deepCopy();
		Tree cDst = dst.deepCopy();
		TreeUtils.removeMapped(cSrc);
		TreeUtils.removeMapped(cDst);

		if (cSrc.getSize() < SIZE_THESHOLD || cDst.getSize() < SIZE_THESHOLD) {
			Matcher m = new RtedMatcher(cSrc, cDst);
			for (Mapping candidate: m.getMappings()) {
				Tree left = srcIds.getTree(candidate.getFirst().getId());
				Tree right = dstIds.getTree(candidate.getSecond().getId());

				if (left.getId() == src.getId() || right.getId() == dst.getId()) {
					//System.err.println("Trying to map already mapped source node.");
					continue;
				} else if (!left.isMatchable(right)) {
					//System.err.println("Trying to map not compatible nodes.");
					continue;
				//} else if (left.getParent().getType() != right.getParent().getType()) {
					//System.err.println("Trying to map nodes with incompatible parents");
					//continue;
				} else addMapping(left, right);
			}
		}
		
		for (Tree t : src.getTrees()) t.setMatched(true);
		for (Tree t : dst.getTrees()) t.setMatched(true);
	}

}
