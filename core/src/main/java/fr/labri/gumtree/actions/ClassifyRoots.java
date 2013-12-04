package fr.labri.gumtree.actions;

import java.util.List;
import java.util.Set;

import fr.labri.gumtree.matchers.composite.Matcher;
import fr.labri.gumtree.tree.Mapping;
import fr.labri.gumtree.tree.Tree;

public class ClassifyRoots extends ClassifyTrees {

	public ClassifyRoots(Tree src, Tree dst, Set<Mapping> rawMappings, List<Action> script) {
		super(src, dst, rawMappings, script);	
	}
	
	public ClassifyRoots(Tree src, Tree dst, Matcher m) {
		super(src, dst, m);
	}
	
	public void classify() {
		for (Action a: actions) {
			if (a instanceof Delete) srcDelTrees.add(a.getNode());
			else if (a instanceof Insert) dstAddTrees.add(a.getNode());
			else if (a instanceof Update) {
				srcUpdTrees.add(a.getNode());
				dstUpdTrees.add(mappings.getDst(a.getNode()));
			} else if (a instanceof Move || a instanceof Permute) {
				srcMvTrees.add(a.getNode());
				dstMvTrees.add(mappings.getDst(a.getNode()));
			}
		}
	}
	

}
