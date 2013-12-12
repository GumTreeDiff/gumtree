package fr.labri.gumtree.actions;

import java.util.List;
import java.util.Set;

import fr.labri.gumtree.actions.model.Action;
import fr.labri.gumtree.actions.model.Delete;
import fr.labri.gumtree.actions.model.Insert;
import fr.labri.gumtree.actions.model.Move;
import fr.labri.gumtree.actions.model.Permute;
import fr.labri.gumtree.actions.model.Update;
import fr.labri.gumtree.matchers.Mapping;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.tree.Tree;

public class RootsClassifier extends TreeClassifier {

	public RootsClassifier(Tree src, Tree dst, Set<Mapping> rawMappings, List<Action> script) {
		super(src, dst, rawMappings, script);	
	}
	
	public RootsClassifier(Tree src, Tree dst, Matcher m) {
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
