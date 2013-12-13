package fr.labri.gumtree.matchers;

import fr.labri.gumtree.tree.Tree;

public interface MatcherFactory {
	
	public Matcher newMatcher(Tree src, Tree dst);

}
