package fr.labri.gumtree.actions.model;

import fr.labri.gumtree.tree.Tree;

public class Permute extends Addition {

	public Permute(Tree node, Tree parent, int pos) {
		super(node, parent, pos);
	}

	@Override
	protected String getName() {
		return "PER";
	}

}
