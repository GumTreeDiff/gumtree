package fr.labri.gumtree.actions.model;

import fr.labri.gumtree.tree.Tree;

public abstract class Addition extends Action {

	protected Tree parent;
	
	protected int pos;
	
	public Addition(Tree node, Tree parent, int pos) {
		super(node);
		this.parent = parent;
		this.pos = pos;
	}
	
	@Override
	public String toString() {
		return getName() + " " + node.toTreeString() + " to " + parent.toString() + " at " + pos;
	}
	
}
