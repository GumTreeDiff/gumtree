package fr.labri.gumtree.matchers.heuristic.gt;

import fr.labri.gumtree.matchers.CompositeMatcher;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.MatcherFactories;
import fr.labri.gumtree.matchers.MatcherFactory;
import fr.labri.gumtree.tree.ITree;

public class GumtreeMatchers {

	public static class ClassicGumtreeMatcherFactory implements MatcherFactory {
		@Override
		public Matcher newMatcher(ITree src, ITree dst) {
			return new CompositeMatcher(src, dst, new MatcherFactory[] {
					MatcherFactories.getFactory(GreedySubtreeMatcher.GreedySubtreeMatcherFactory.class),
					MatcherFactories.getFactory(GreedyBottomUpMatcher.GreedyBottomUpMatcherFactory.class)});
		}
	}
	
	public static class CompleteGumtreeMatcherFactory implements MatcherFactory {
		@Override
		public Matcher newMatcher(ITree src, ITree dst) {
			return new CompositeMatcher(src, dst, new MatcherFactory[] {
					MatcherFactories.getFactory(CliqueSubtreeMatcher.CliqueSubtreeMatcherFactory.class),
					MatcherFactories.getFactory(CompleteBottomUpMatcher.CompleteBottomUpMatcherFactory.class)});
		}
	}
	
}
