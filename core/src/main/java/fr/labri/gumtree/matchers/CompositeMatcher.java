package fr.labri.gumtree.matchers;

import fr.labri.gumtree.tree.ITree;

public class CompositeMatcher extends Matcher {

    protected final Matcher[] matchers;

    public CompositeMatcher(ITree src, ITree dst, MappingStore store, Matcher[] matchers) {
        super(src, dst, store);
        this.matchers = matchers;
    }

    public void match() {
        for (Matcher matcher : matchers) {
            matcher.match();
        }
    }

}
