package fr.labri.gumtree.actions.model;

import fr.labri.gumtree.tree.Tree;

public class Insert extends Addition {

	public Insert(Tree node, Tree parent, int pos) {
		super(node, parent, pos);
	}

	@Override
	protected String getName() {
		return "INS";
	}

	
}
