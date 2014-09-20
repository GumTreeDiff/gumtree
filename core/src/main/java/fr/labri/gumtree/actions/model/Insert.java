package fr.labri.gumtree.actions.model;

import fr.labri.gumtree.tree.ITree;

public class Insert extends Addition {

	public Insert(ITree node, ITree parent, int pos) {
		super(node, parent, pos);
	}

	@Override
	protected String getName() {
		return "INS";
	}

	
}
