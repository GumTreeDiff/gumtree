package fr.labri.gumtree.actions;

import java.util.List;
import java.util.Set;

import fr.labri.gumtree.actions.model.Action;
import fr.labri.gumtree.actions.model.Delete;
import fr.labri.gumtree.actions.model.Insert;
import fr.labri.gumtree.actions.model.Move;
import fr.labri.gumtree.actions.model.Update;
import fr.labri.gumtree.matchers.Mapping;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.TreeContext;

public class LeavesClassifier extends TreeClassifier {

	public LeavesClassifier(TreeContext src, TreeContext dst, Set<Mapping> rawMappings, List<Action> actions) {
		super(src, dst, rawMappings, actions);
	}
	
	public LeavesClassifier(TreeContext src, TreeContext dst, Matcher m) {
		super(src, dst, m);
	}


	@Override
	public void classify() {
		for (Action a: actions) {
			if (a instanceof Delete && isLeafAction(a)) {
				srcDelTrees.add(a.getNode());
			} else if (a instanceof Insert && isLeafAction(a)) {
				dstAddTrees.add(a.getNode());
			} else if (a instanceof Update && isLeafAction(a)) {
					srcUpdTrees.add(a.getNode());
					dstUpdTrees.add(mappings.getDst(a.getNode()));
			} else if (a instanceof Move && isLeafAction(a)) {
				srcMvTrees.add(a.getNode());
				dstMvTrees.add(mappings.getDst(a.getNode()));
			}
		}
	}
	
	private boolean isLeafAction(Action a) {
		for (ITree d: a.getNode().getDescendants()) {
			for (Action c: actions)
				if (a != c && d == c.getNode()) return false;
		}
		
		return true;
	}
}
