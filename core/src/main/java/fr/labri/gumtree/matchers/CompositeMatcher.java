package fr.labri.gumtree.matchers;

import fr.labri.gumtree.tree.Tree;

public class CompositeMatcher extends Matcher {
	
	protected MatcherFactory[] factories;
	
	public CompositeMatcher(Tree src, Tree dst, MatcherFactory[] factories) {
		super(src, dst);
		this.factories = factories;
	}
	
	public void match() {
		long[] perfs = new long[factories.length];
		for (int i = 0; i < factories.length; i++) {
			MatcherFactory f = factories[i];
			Matcher m = f.newMatcher(src, dst);
			m.setMappings(mappings);
			long tic = System.currentTimeMillis();
			m.match();
			long toc = System.currentTimeMillis();
			perfs[i] = toc - tic;
		}
		StringBuffer b = new StringBuffer();
		b.append("Matching performed. Times: ");
		for (int i = 0; i < factories.length; i++) {
			b.append(factories[i].getClass().getSimpleName() + "=" + perfs[i]);
			if (i != factories.length - 1) b.append(",");
		}
		LOGGER.info(b.toString());
	}
	
}
