package fr.labri.gumtree.matchers;

import fr.labri.gumtree.matchers.heuristic.XyBottomUpMatcher;
import fr.labri.gumtree.matchers.heuristic.cd.ChangeDistillerBottumUpMatcher;
import fr.labri.gumtree.matchers.heuristic.cd.ChangeDistillerLeavesMatcher;
import fr.labri.gumtree.matchers.heuristic.gt.GreedySubtreeMatcher;
import fr.labri.gumtree.tree.ITree;

public class CompositeMatchers {

	public static class ChangeDistillerMatcherFactory implements MatcherFactory {

		@Override
		public Matcher newMatcher(ITree src, ITree dst) {
			return new CompositeMatcher(src, dst, new MatcherFactory[] {
					MatcherFactories.getFactory(ChangeDistillerLeavesMatcher.ChangeDistillerLeavesMatcherFactory.class),
					MatcherFactories.getFactory(ChangeDistillerBottumUpMatcher.ChangeDistillerBottomUpMatcherFactory.class)});
		}
	}
	
	public static class XyMatcherFactory implements MatcherFactory {

		@Override
		public Matcher newMatcher(ITree src, ITree dst) {
			return new CompositeMatcher(src, dst, new MatcherFactory[] {
					MatcherFactories.getFactory(GreedySubtreeMatcher.GreedySubtreeMatcherFactory.class),
					MatcherFactories.getFactory(XyBottomUpMatcher.XyBottomUpMatcherFactory.class)});
		}
	}
	
}