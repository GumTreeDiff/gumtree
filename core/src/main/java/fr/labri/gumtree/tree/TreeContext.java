package fr.labri.gumtree.tree;

import java.util.HashMap;
import java.util.Map;

public class TreeContext {

	Map<Integer, String> typeLabels = new HashMap<>();
	
	ITree root;
	
	@Override
	public String toString() {
		return root.toPrettyString(this);
	}

	public void setRoot(ITree root) {
		this.root = root;
	}
	
	public ITree getRoot() {
		return root;
	}

	public String getTypeLabel(ITree tree) {
		return getTypeLabel(tree.getType());
	}
	
	public String getTypeLabel(int type) {
		String tl = typeLabels.get(type);
		if (tl == null)
			tl = Integer.toString(type);
		return tl;
	}

	protected void registerTypeLabel(int type, String name) {
		if (name == null || name.equals(ITree.NO_LABEL))
			return;
		String typeLabel = typeLabels.get(type);
		if (typeLabel == null) {
			typeLabels.put(type, name);
		} else if (!typeLabel.equals(name))
			throw new RuntimeException(String.format("Redefining type %d: '%s' with '%s'", type, typeLabel, name));
	}
	
	public ITree createTree(int type, String label, String typeLabel) {
		registerTypeLabel(type, typeLabel);
		
		return new Tree(type, label);
	}
	
	public ITree createTree(ITree... trees) {
		return new AbstractTree.FakeTree(trees);
	}

	public void validate() {
		root.refresh();
		TreeUtils.postOrderNumbering(root);
	}

	public boolean hasLabelFor(int type) {
		return typeLabels.containsKey(type);
	}
}
