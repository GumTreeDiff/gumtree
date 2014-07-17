package fr.labri.gumtree.matchers.heuristic.gt;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.labri.gumtree.matchers.Mapping;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.MatcherFactory;
import fr.labri.gumtree.matchers.MultiMappingStore;
import fr.labri.gumtree.tree.Tree;

public class GreedySubtreeMatcher extends SubtreeMatcher {

	public GreedySubtreeMatcher(Tree src, Tree dst) {
		super(src, dst);
	}
	
	public void filterMappings(MultiMappingStore multiMappings) {
		//System.out.println("phase unique");
		// Select unique mappings first and extract ambiguous mappings.
		
		List<Mapping> ambiguousList = new LinkedList<>();
		Set<Tree> ignored = new HashSet<>();
		for (Tree src: multiMappings.getSrcs()) {
			if (multiMappings.isSrcUnique(src)) addFullMapping(src, multiMappings.getDst(src).iterator().next());
			else if (!ignored.contains(src)) {
				Set<Tree> adsts = multiMappings.getDst(src);
				Set<Tree> asrcs = multiMappings.getSrc(multiMappings.getDst(src).iterator().next());
				for (Tree asrc : asrcs) for(Tree adst: adsts) ambiguousList.add(new Mapping(asrc, adst));
				ignored.addAll(asrcs);
			}
		}

		// System.out.println("phase sorting");
		// Rank the mappings by score.
		Set<Tree> srcIgnored = new HashSet<>();
		Set<Tree> dstIgnored = new HashSet<>();
		Collections.sort(ambiguousList, new MappingComparator(ambiguousList));

		// System.out.println("phase ambiguous");
		// Select the best ambiguous mappings
		while (ambiguousList.size() > 0) {
			Mapping ambiguous = ambiguousList.remove(0);
			if (!(srcIgnored.contains(ambiguous.getFirst()) || dstIgnored.contains(ambiguous.getSecond()))) {
				addFullMapping(ambiguous.getFirst(), ambiguous.getSecond());
				srcIgnored.add(ambiguous.getFirst());
				dstIgnored.add(ambiguous.getSecond());
			}
		}
	}

	private class MappingComparator implements Comparator<Mapping> {
		
		private Map<Mapping, Double> simMap = new HashMap<>();
		
		public MappingComparator(List<Mapping> mappings) {
			for (Mapping mapping: mappings)
				simMap.put(mapping, sim(mapping.getFirst(), mapping.getSecond()));
		}
		
		public int compare(Mapping m1, Mapping m2) {
			return Double.compare(simMap.get(m2), simMap.get(m1));
		}
		
		 
		private Map<Tree, List<Tree>> srcDescendants = new HashMap<>();
		
		private Map<Tree, Set<Tree>> dstDescendants = new HashMap<>();

	
		protected int numberOfCommonDescendants(Tree src, Tree dst) {
			if (!srcDescendants.containsKey(src)) srcDescendants.put(src, src.getDescendants());
			if (!dstDescendants.containsKey(dst)) dstDescendants.put(dst, new HashSet<>(dst.getDescendants()));
		
			int common = 0;
			
			for (Tree t: srcDescendants.get(src)) {
				Tree m = mappings.getDst(t);
				if (m != null && dstDescendants.get(dst).contains(m)) common++;
			}

			return common;
		}
		
		protected double sim(Tree src, Tree dst) {
			double jaccard = jaccardSimilarity(src.getParent(), dst.getParent());
			int posSrc = (src.isRoot()) ? 0 : src.getParent().getChildPosition(src);
			int posDst = (dst.isRoot()) ? 0 : dst.getParent().getChildPosition(dst);
			int maxSrcPos =  (src.isRoot()) ? 1 : src.getParent().getChildren().size();
			int maxDstPos =  (dst.isRoot()) ? 1 : dst.getParent().getChildren().size();
			int maxPosDiff = Math.max(maxSrcPos, maxDstPos);
			double pos = 1D - ((double) Math.abs(posSrc - posDst) / (double) maxPosDiff);
			double po = 1D - ((double) Math.abs(src.getId() - dst.getId()) / (double) GreedySubtreeMatcher.this.getMaxTreeSize());
			return 100 * jaccard + 10 * pos + po;
		}
		
		protected double jaccardSimilarity(Tree src, Tree dst) {
			double num = (double) numberOfCommonDescendants(src, dst);
			double den = (double) srcDescendants.get(src).size() + (double) dstDescendants.get(dst).size() - num;
			return num/den;
		}
		
		/*
		
		@Override
		public int compare(Mapping m1, Mapping m2) {
			return Double.compare(sim(m2.getFirst(), m2.getSecond()), sim(m1.getFirst(), m1.getSecond()));
		}
		
		*/

	}
	
	public static class GreedySubtreeMatcherFactory implements MatcherFactory {

		@Override
		public Matcher newMatcher(Tree src, Tree dst) {
			return new GreedySubtreeMatcher(src, dst);
		}
		
	}

}
