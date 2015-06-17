package fr.labri.gumtree.client.diff;

import fr.labri.gumtree.gen.TreeGeneratorRegistry;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.MatcherFactories;
import fr.labri.gumtree.tree.TreeContext;

import java.io.IOException;

public abstract class DiffClient {
	TreeContext src, dst;
	Matcher matcher;
	protected DiffOptions diffOptions;

	public DiffClient(DiffOptions diffOptions) {
		this.diffOptions = diffOptions;
	}

	public abstract void start();

	protected Matcher matchTrees() {
		if (matcher != null)
			return matcher;
		matcher = (diffOptions.getMatcher() == null)
				? MatcherFactories.newMatcher(getSrcTreeContext().getRoot(), getDstTreeContext().getRoot())
				: MatcherFactories.newMatcher(getSrcTreeContext().getRoot(), getDstTreeContext().getRoot(), diffOptions.getMatcher());
		matcher.match();
		return matcher;
	}

	protected TreeContext getSrcTreeContext() {
		if (src == null)
			src = getTreeContext(diffOptions.getSrc());
		return src;
	}

	protected TreeContext getDstTreeContext() {
		if (dst == null)
			dst = getTreeContext(diffOptions.getDst());
		return dst;
	}

	private TreeContext getTreeContext(String file) {
		try {
			TreeContext t = TreeGeneratorRegistry.getInstance().getTree(file);
			return t;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
