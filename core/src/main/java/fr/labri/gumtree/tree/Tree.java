package fr.labri.gumtree.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Tree implements ITree {
	
	private static final String OPEN_SYMBOL = "[(";
	private static final String CLOSE_SYMBOL = ")]";
	private static final String SEPARATE_SYMBOL = "@@";	
	// End constants
	
	// Unique id of the node.
	private int id;

	// Type of the token
	private int type;

	// Label of the token
	private String label;
	
	// Begin hierarchy of the tree
	private ITree parent;
	private List<ITree> children;
	// End hierarchy of the tree
	
	// Begin metrics on the tree
	private int height;
	private int size;
	private int depth;
	private int digest;
	// End metrics

	// Begin position of the tree in terms of absolute character index 
	private int pos;
	private int length;
	// End position
	
	// Begin position in terms of line and column start and end
	private int[] lcPosStart;
	private int[] lcPosEnd;
	// End position
	
	// Useless it should be implemented outside
	//TODO remove the matched attribute
	private boolean matched;

	// Needed for RTED :(
	private Object tmpData;
	
	public Tree(int type) {
		this(type, NO_LABEL);
	}

	public Tree(int type, String label) {
		this(type, label, NO_LABEL);
	}

	public Tree(int type, String label, String typeLabel) {
		this.type = type;
		registerTypeLabel(type, typeLabel == null ? NO_LABEL : typeLabel);
		this.label = (label == null) ? NO_LABEL : label.intern();
		this.children = new ArrayList<>();
		this.id = NO_ID;
		this.depth = NO_VALUE;
		this.digest = NO_VALUE;
		this.height = NO_VALUE;
		this.depth = NO_VALUE;
		this.size = NO_VALUE;
		this.pos = NO_VALUE;
		this.length = NO_VALUE;
		this.matched = false;
	}

	@Override
	public void addChild(ITree t) {
		children.add(t);
		t.setParent(this);
	}

	@Override
	public boolean areDescendantsMatched() {
		for (ITree c: getDescendants()) if (!c.isMatched()) return false;
		return true;
	}

	@Override
	public Tree copy() {
		Tree t = new Tree(this.getType(), this.getLabel(), this.getTypeLabel());
		t.setId(this.getId());
		t.setMatched(this.isMatched());
		t.setPos(this.getPos());
		t.setLength(this.getLength());
		t.setHeight(this.getHeight());
		t.setSize(this.getSize());
		t.setDepth(this.getDepth());
		t.setDigest(this.getDigest());
		t.setTmpData(this.getTmpData());
		return t;
	}

	@Override
	public Tree deepCopy() {
		Tree copy = copy();
		for (ITree child: getChildren())
			copy.addChild(child.deepCopy());
		return copy;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Tree)) return false;
		else {
			Tree t = (Tree) o;
			if (t.id != NO_ID) return t.id == id;
			else return super.equals(t);
		}
	}

	@Override
	public int getChildPosition(ITree child) {
		return children.indexOf(child);
	}

	@Override
	public List<ITree> getChildren() {
		return children;
	}

	@Override
	public String getChildrenLabels() {
		StringBuffer b = new StringBuffer();
		for (ITree child: getChildren()) if (!"".equals(child.getLabel())) b.append(child.getLabel() + " ");
		return b.toString().trim();
	}

	@Override
	public int getDepth() {
		return depth;
	}

	@Override
	public List<ITree> getDescendants() {
		List<ITree> trees = TreeUtils.preOrder(this); 
		trees.remove(0);
		return trees;
	}

	@Override
	public int getDigest() {
		return digest;
	}

	@Override
	public int getEndPos() {
		return pos + length;
	}

	@Override
	public int getHeight() {
		return height;
	}
	
	@Override
	public int getId() {
		return id;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public int[] getLcPosEnd() {
		return lcPosEnd;
	}

	@Override
	public int[] getLcPosStart() {
		return lcPosStart;
	}

	@Override
	public List<ITree> getLeaves() {
		List<ITree> leafs = new ArrayList<>();
		for (ITree t: this.getTrees()) if (t.isLeaf()) leafs.add(t);
		return leafs;
	}

	@Override
	public int getLength() {
		return length;
	}

	@Override
	public ITree getParent() {
		return parent;
	}

	@Override
	public List<ITree> getParents() {
		List<ITree> parents = new ArrayList<>();
		if (this.getParent() == null) return parents;
		else {
			parents.add(getParent());
			parents.addAll(getParent().getParents());
		}
		return parents;
	}

	@Override
	public int getPos() {
		return pos;
	}

	@Override
	public String getShortLabel() {
		return label.substring(0, Math.min(50, label.length()));
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public Object getTmpData() {
		return tmpData;
	}

	@Override
	public List<ITree> getTrees() {
		return TreeUtils.preOrder(this);
	}

	@Override
	public int getType() {
		return type;
	}
	
	@Override
	public String getTypeLabel() {
		return getFromTypeLabels(type);
	}

	@Override
	public int hashCode() {
		if (id != NO_ID) return id;
		else return super.hashCode();
	}

	private String indent(ITree t) {
		StringBuffer b = new StringBuffer();
		for (int i = 0; i < t.getDepth(); i++) b.append("\t");
		return b.toString();
	}

	@Override
	public boolean isClone(ITree tree) {
		if (this.getDigest() != tree.getDigest()) return false;
		else {
			boolean res = (this.toDigestTreeString().equals(tree.toDigestTreeString())); 
			return res;
		}
	}

	@Override
	public boolean isCompatible(ITree t) {
		return this.getType() == t.getType();
	}

	@Override
	public boolean isLeaf() {
		return getChildren().size() == 0;
	}

	@Override
	public boolean isMatchable(ITree t) {
		return this.isCompatible(t) && !(this.isMatched()  || t.isMatched());
	}

	@Override
	public boolean isMatched() {
		return matched;
	}

	@Override
	public boolean isRoot() {
		return parent == null;
	}

	@Override
	public boolean isSimilar(ITree t) {
		if (!this.isCompatible(t)) return false;
		else if (!this.getLabel().equals(t.getLabel())) return false;
		return true;
	}

	@Override
	public Iterable<ITree> postOrder() {
		return new Iterable<ITree>() {
			@Override
			public Iterator<ITree> iterator() {
				return TreeUtils.postOrderIterator(Tree.this);
			}
		};
	}
	
	@Override
	public Iterable<ITree> breadthFirst() {
		return new Iterable<ITree>() {
			@Override
			public Iterator<ITree> iterator() {
				return TreeUtils.breadthFirstIterator(Tree.this);
			}
		};
	}

	@Override
	public int positionInParent() {
		if (parent == null)
			return -1;
		else
			return parent.getChildren().indexOf(this);
	}

	@Override
	public void refresh() {
		TreeUtils.computeSize(this);
		TreeUtils.computeDepth(this);
		TreeUtils.computeHeight(this);
		TreeUtils.computeDigest(this);
	}

	@Override
	public void setChildren(List<ITree> children) {
		this.children = children;
		for (ITree c: children)
			c.setParent(this);
	}

	@Override
	public void setDepth(int depth) {
		this.depth = depth;
	}

	@Override
	public void setDigest(int digest) {
		this.digest = digest;
	}

	@Override
	public void setHeight(int height) {
		this.height = height;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

	@Override
	public void setLabel(String label) {
		this.label = label;
	}

	@Override
	public void setLcPosEnd(int[] lcPosEnd) {
		this.lcPosEnd = lcPosEnd;
	}

	@Override
	public void setLcPosStart(int[] lcPosStart) {
		this.lcPosStart = lcPosStart;
	}

	@Override
	public void setLength(int length) {
		this.length = length;
	}

	@Override
	public void setMatched(boolean matched) {
		this.matched = matched;
	}

	@Override
	public void setParent(ITree parent) {
		this.parent = parent;
	}

	@Override
	public void setParentAndUpdateChildren(ITree parent) {
		if (this.parent != null) this.parent.getChildren().remove(this);
		this.parent = parent;
		if (this.parent != null) parent.getChildren().add(this);
	}

	@Override
	public void setPos(int pos) {
		this.pos = pos;
	}

	@Override
	public void setSize(int size) {
		this.size = size;
	}

	@Override
	public void setTmpData(Object tmpData) {
		this.tmpData = tmpData;
	}

	@Override
	public void setType(int type) {
		this.type = type;
	}

	@Override
	public String toCompleteString() {
		return label + "@" + getTypeLabel() + ":" + type + " [id=" + id + ", depth:" + depth + ", maxdepth=" + height + ", digest=" + digest + ", pos=" + pos + ", length=" + length + "]";
	}

	@Override
	public String toCompleteTreeString() {
		if (isLeaf()) return toString();
		else {
			StringBuffer b = new StringBuffer();
			b.append(toString() + "(");
			for (ITree c : getChildren())
				b.append(c.toCompleteTreeString() + " ");
			b.append(")");
			return b.toString();
		}	
	}

	@Override
	public String toDigestString() {
		return getLabel() + SEPARATE_SYMBOL + getType();
	}

	@Override
	public String toDigestTreeString() {
		StringBuffer b = new StringBuffer();
		b.append(OPEN_SYMBOL);
		b.append(this.toDigestString());
		for (ITree c: this.getChildren()) b.append(c.toDigestTreeString());
		b.append(CLOSE_SYMBOL);
		return b.toString();
	}

	@Override
	public String toString() {
		if (!"".equals(getLabel())) {
			return getId() + ": " + getLabel();
		} else {
			/*if (!"".equals(getChildrenLabels())) return getTypeLabel() + ": " + getChildrenLabels();
			else*/ return getTypeLabel();
		}

	}

	@Override
	public String toTreeString() {
		/*if (isLeaf()) return this.toString();
		else {
			StringBuffer b = new StringBuffer();
			b.append(toString() + " (");
			Iterator<Tree> cIt = this.getChildren().iterator();
			while (cIt.hasNext()) {
				Tree c = cIt.next();
				b.append(c.toTreeString());
				if (cIt.hasNext()) b.append(" ");
			}
			b.append(")");
			return b.toString();
		}*/
		StringBuffer b = new StringBuffer();
		for (ITree t : TreeUtils.preOrder(this)) b.append(indent(t) + t.toString() + "\n");
		return b.toString();
	}

	static Map<Integer, String> typeLabels = new HashMap<>();
	
	static private String getFromTypeLabels(int type) {
		String tl = typeLabels.get(type);
		if (tl == null)
			tl = NO_LABEL;
		return tl;
	}
	
	static private void registerTypeLabel(int type, String name) {
		if (name.equals(NO_LABEL))
			return;
		String typeLabel = typeLabels.get(type);
		if (typeLabel == null) {
			typeLabels.put(type, name);
		} else if (!typeLabel.equals(name))
			throw new RuntimeException(String.format("Redefining type %d: '%s' with '%s'", type, typeLabel, name));
	}
}
