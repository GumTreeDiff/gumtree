package fr.labri.gumtree.actions.model;

import fr.labri.gumtree.tree.ITree;

public class Delete extends Action {

	public Delete(ITree node) {
		super(node);
	}

	@Override
	protected String getName() {
		return "DEL";
	}

	@Override
	public String toString() {
		return getName() + " " + node.toShortString();
	}

}
