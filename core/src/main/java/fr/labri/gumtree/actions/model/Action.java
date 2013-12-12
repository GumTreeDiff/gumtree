package fr.labri.gumtree.actions.model;

import fr.labri.gumtree.tree.Tree;

public abstract class Action {

	protected Tree node;
	
	public Action(Tree node) {
		this.node = node;
	}

	public Tree getNode() {
		return node;
	}

	public void setNode(Tree node) {
		this.node = node;
	}

	protected abstract String getName();
	
	public abstract String toString();
	
}
