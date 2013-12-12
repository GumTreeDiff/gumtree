package fr.labri.gumtree.matchers.composite;

import java.util.HashSet;
import java.util.Iterator;

import fr.labri.gumtree.matchers.Mapping;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.optimal.zs.ZsMatcher;
import fr.labri.gumtree.tree.Tree;
import fr.labri.gumtree.tree.TreeUtils;

public class RawZsMatcher extends Matcher {

	public RawZsMatcher(Tree src, Tree dst) {
		super(prepare(src), prepare(dst));
		Matcher matcher = new ZsMatcher(src, dst, new HashSet<Mapping>());
		this.mappings = matcher.getMappings();
		Iterator<Mapping> mIt = mappings.iterator();
		while (mIt.hasNext()) {
			Mapping m = mIt.next();
			if (m.getFirst().getType() != m.getSecond().getType()) {
				mIt.remove();
				System.err.println("Trying to map not compatible nodes.");
			}
		}
	}

	public static Tree prepare(Tree tree) {
		TreeUtils.postOrderNumbering(tree);
		return tree;
	}
	
	@Override
	public void match() {
	}
	
}
