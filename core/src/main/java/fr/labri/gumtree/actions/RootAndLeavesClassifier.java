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
import fr.labri.gumtree.tree.Tree;

public class RootAndLeavesClassifier extends TreeClassifier {
	
	public RootAndLeavesClassifier(Tree src, Tree dst, Set<Mapping> rawMappings, List<Action> actions) {
		super(src, dst, rawMappings, actions);
	}

	public RootAndLeavesClassifier(Tree src, Tree dst, Matcher m) {
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

		Set<Tree> fDstAddTrees = new HashSet<>();
		for (Tree t: dstAddTrees) 
			if (!dstAddTrees.contains(t.getParent()))
				fDstAddTrees.add(t);
		dstAddTrees = fDstAddTrees;
		
		Set<Tree> fSrcDelTrees = new HashSet<>();
		for (Tree t: srcDelTrees) {
			if (!srcDelTrees.contains(t.getParent()))
				fSrcDelTrees.add(t);
		}
		srcDelTrees = fSrcDelTrees;
	}
	
}
