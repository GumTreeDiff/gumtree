package fr.labri.gumtree.matchers.composite;

import java.util.Iterator;

import fr.labri.gumtree.matchers.Mapping;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.optimal.rted.RtedMatcher;
import fr.labri.gumtree.tree.Tree;
import fr.labri.gumtree.tree.TreeUtils;

public class RawRtedMatcher extends Matcher {

	public RawRtedMatcher(Tree src, Tree dst) {
		super(prepare(src), prepare(dst));
		Matcher matcher = new RtedMatcher(src, dst);
		this.mappings = matcher.getMappings();
		Iterator<Mapping> mIt = mappings.iterator();
		while (mIt.hasNext()) {
			Mapping m = mIt.next();
			if (m.getFirst().getType() != m.getSecond().getType()) mIt.remove();
		}
	}
	
	public static Tree prepare(Tree tree) {
		TreeUtils.postOrderNumbering(tree);
		return tree;
	}
	
	@Override
	public void match() {
		new RtedMatcher(src, dst);
	}
	
}
