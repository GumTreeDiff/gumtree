package fr.labri.gumtree.actions;

import java.util.HashSet;
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

public class RootAndLeavesClassifier extends TreeClassifier {
	
	public RootAndLeavesClassifier(TreeContext src, TreeContext dst, Set<Mapping> rawMappings, List<Action> actions) {
		super(src, dst, rawMappings, actions);
	}

	public RootAndLeavesClassifier(TreeContext src, TreeContext dst, Matcher m) {
		super(src, dst, m);
	}

	@Override
	public void classify() {
		for (Action a: actions) {
			if (a instanceof Insert) {
				dstAddTrees.add(a.getNode());
			} else if (a instanceof Delete) {
				srcDelTrees.add(a.getNode());
			} else if (a instanceof Update) {
				srcUpdTrees.add(a.getNode());
				dstUpdTrees.add(mappings.getDst(a.getNode()));
			} else if (a instanceof Move) {
				srcMvTrees.add(a.getNode());
				dstMvTrees.add(mappings.getDst(a.getNode()));
			}	
		}

		Set<ITree> fDstAddTrees = new HashSet<>();
		for (ITree t: dstAddTrees) 
			if (!dstAddTrees.contains(t.getParent()))
				fDstAddTrees.add(t);
		dstAddTrees = fDstAddTrees;
		
		Set<ITree> fSrcDelTrees = new HashSet<>();
		for (ITree t: srcDelTrees) {
			if (!srcDelTrees.contains(t.getParent()))
				fSrcDelTrees.add(t);
		}
		srcDelTrees = fSrcDelTrees;
		
		Set<ITree> fSrcMvTrees = new HashSet<>(); // FIXME check why it's unused
		for (ITree t: srcDelTrees) {
			if (!srcDelTrees.contains(t.getParent()))
				fSrcDelTrees.add(t);
		}
		srcDelTrees = fSrcDelTrees;
	}
	
}
