package fr.labri.gumtree.matchers.heuristic;

import static fr.labri.gumtree.tree.TreeUtils.postOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.MatcherFactory;
import fr.labri.gumtree.tree.Tree;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Match the nodes using a bottom-up approach. It browse the nodes of the source and destination trees
 * using a post-order traversal, testing if the two selected trees might be mapped. The two trees are mapped 
 * if they are mappable and have a dice coefficient greater than SIM_THRESHOLD. Whenever two trees are mapped
 * a exact ZS algorithm is applied to look to possibly forgotten nodes.
 */
public class XyBottomUpMatcher extends Matcher {

	private static final double SIM_THRESHOLD = 0.001D;
	
	private TIntObjectMap<Tree> srcs = new TIntObjectHashMap<>();
	
	private TIntObjectMap<Tree> dsts = new TIntObjectHashMap<>();

	public XyBottomUpMatcher(Tree src, Tree dst) {
		super(src, dst);
	}

	public void match() {
		for (Tree t : src.getTrees()) srcs.put(t.getId(), t);
		for (Tree t : dst.getTrees()) dsts.put(t.getId(), t);
		
		for (Tree src: src.postOrder())  {
			if (src.isRoot()) {
				addMapping(src, this.dst);
				lastChanceMatch(src, this.dst);
			} else if (!(src.isMatched() || src.isLeaf())) {
				Set<Tree> candidates = getDstCandidates(src);
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
		clean();
	}

	private Set<Tree> getDstCandidates(Tree src) {
		Set<Tree> seeds = new HashSet<>();
		for (Tree c: src.getDescendants()) {
			Tree m = mappings.getDst(c);
			if (m != null) seeds.add(m);
		}
		Set<Tree> candidates = new HashSet<>();
		Set<Tree> visited = new HashSet<>();
		for(Tree seed: seeds) {
			while (seed.getParent() != null) {
				Tree parent = seed.getParent();
				if (visited.contains(parent)) break;
				visited.add(parent);
				if (parent.getType() == src.getType() && !parent.isMatched()) candidates.add(parent);
				seed = parent;
			}
		}

		return candidates;
	}

	private void lastChanceMatch(Tree src, Tree dst) {
		Map<Integer,List<Tree>> srcKinds = new HashMap<>();
		Map<Integer,List<Tree>> dstKinds = new HashMap<>();
		for (Tree c: src.getChildren()) {
			if (!srcKinds.containsKey(c.getType())) srcKinds.put(c.getType(), new ArrayList<Tree>());
			srcKinds.get(c.getType()).add(c);
		}
		for (Tree c: dst.getChildren()) {
			if (!dstKinds.containsKey(c.getType())) dstKinds.put(c.getType(), new ArrayList<Tree>());
			dstKinds.get(c.getType()).add(c);
		}
		
		for (int t: srcKinds.keySet()) 
			if (srcKinds.get(t).size() == dstKinds.get(t).size() && srcKinds.get(t).size() == 1)
				addMapping(srcKinds.get(t).get(0), dstKinds.get(t).get(0));

	}
	
	public static class XyBottomUpMatcherFactory implements MatcherFactory {

		@Override
		public Matcher newMatcher(Tree src, Tree dst) {
			return new XyBottomUpMatcher(src, dst);
		}
		
	}

}
