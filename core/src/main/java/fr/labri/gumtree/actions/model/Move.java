package fr.labri.gumtree.actions.model;

import fr.labri.gumtree.tree.Tree;

public class Move extends Addition {

	public Move(Tree node, Tree parent, int pos) {
		super(node, parent, pos);
	}

	@Override
	protected String getName() {
		return "MOV";
	}

}
