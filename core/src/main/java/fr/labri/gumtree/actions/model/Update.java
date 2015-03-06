package fr.labri.gumtree.actions.model;

import fr.labri.gumtree.tree.ITree;

public class Update extends Action {
	
	private String value;
	
	public Update(ITree node, String value) {
		super(node);
		this.value = value;
	}

	@Override
	protected String getName() {
		return "UPD";
	}
	
	public String getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return getName() + " " + node.toString() + " from " + node.getLabel() + " to " + value;
	}

}
