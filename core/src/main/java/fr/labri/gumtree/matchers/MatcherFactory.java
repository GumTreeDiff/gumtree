package fr.labri.gumtree.matchers;

import fr.labri.gumtree.tree.ITree;

public interface MatcherFactory {
	
	public Matcher newMatcher(ITree src, ITree dst);

}
