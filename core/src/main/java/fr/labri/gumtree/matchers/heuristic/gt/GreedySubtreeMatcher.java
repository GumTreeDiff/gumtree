package fr.labri.gumtree.matchers.heuristic.gt;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
		// System.out.println("phase unique");
		// Select unique mappings first and extract ambiguous mappings
		List<Mapping> ambiguousList = new LinkedList<>();
		Set<Tree> ignored = new HashSet<>();
		for (Tree src: multiMappings.getSrcs()) {
			if (multiMappings.isSrcUnique(src))
				addFullMapping(src, multiMappings.getDst(src).iterator().next());
			else if (!ignored.contains(src)) {
				Set<Tree> adsts = multiMappings.getDst(src);
				Set<Tree> asrcs = multiMappings.getSrc(multiMappings.getDst(src).iterator().next());
				for (Tree asrc : asrcs) for(Tree adst: adsts) ambiguousList.add(new Mapping(asrc, adst));
				ignored.addAll(asrcs);
			}
		}

		// System.out.println("phase sorting");
		// Select the best ambiguous mappings
		Set<Tree> srcIgnored = new HashSet<>();
		Set<Tree> dstIgnored = new HashSet<>();
		Collections.sort(ambiguousList, new MappingComparator());

		// System.out.println("phase ambiguous");
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

		@Override
		public int compare(Mapping m1, Mapping m2) {
			return Double.compare(GreedySubtreeMatcher.this.sim(m2.getFirst(), m2.getSecond()), 
					GreedySubtreeMatcher.this.sim(m1.getFirst(), m1.getSecond()));
		}

	}
	
	public static class GreedySubtreeMatcherFactory implements MatcherFactory {

		@Override
		public Matcher newMatcher(Tree src, Tree dst) {
			return new GreedySubtreeMatcher(src, dst);
		}
		
	}

}
