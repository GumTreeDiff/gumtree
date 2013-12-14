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
	
	private Set<Tree> srcModifiedTrees;

	private Set<Tree> dstModifiedTrees;
	
	public RootAndLeavesClassifier(Tree src, Tree dst, Set<Mapping> rawMappings, List<Action> actions) {
		super(src, dst, rawMappings, actions);
	}

	public RootAndLeavesClassifier(Tree src, Tree dst, Matcher m) {
		super(src, dst, m);
	}


	@Override
	public void classify() {
		srcModifiedTrees = new HashSet<>();
		dstModifiedTrees = new HashSet<>();
		for (Action a: actions) {
			if (a instanceof Insert) {
				dstModifiedTrees.add(a.getNode());
				dstAddTrees.add(a.getNode());
			} else if (a instanceof Delete) {
				srcModifiedTrees.add(a.getNode());
				srcDelTrees.add(a.getNode());
			} else if (a instanceof Update) {
				srcModifiedTrees.add(a.getNode());
				srcUpdTrees.add(a.getNode());
				dstModifiedTrees.add(mappings.getDst(a.getNode()));
				dstUpdTrees.add(mappings.getDst(a.getNode()));
			} else if (a instanceof Move) {
				srcModifiedTrees.add(a.getNode());
				srcMvTrees.add(a.getNode());
				dstModifiedTrees.add(mappings.getDst(a.getNode()));
				dstMvTrees.add(mappings.getDst(a.getNode()));
			}	
		}
		
		Set<Tree> fDstAddTrees = new HashSet<>(dstAddTrees);
		for (Tree t: dstAddTrees)
			if (dstAddTrees.containsAll(t.getDescendants()))
				fDstAddTrees.removeAll(t.getDescendants());
		dstAddTrees = fDstAddTrees;
		
		Set<Tree> fSrcDelTrees = new HashSet<>(srcDelTrees);
		for (Tree t: srcDelTrees) {
			if (srcDelTrees.containsAll(t.getDescendants()))
				fSrcDelTrees.removeAll(t.getDescendants());
		}
		srcDelTrees = fSrcDelTrees;
	}
	
	public int dstDepth(Tree dst) {
		int d = 1;
		for (Tree p: dst.getParents()) if (dstModifiedTrees.contains(p)) d++;
		return d;
	}
	
	public int srcDepth(Tree src) {
		int d = 1;
		for (Tree p: src.getParents()) if (srcModifiedTrees.contains(p)) d++;
		return d;
	}

}
