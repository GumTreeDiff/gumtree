package fr.labri.gumtree.tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Tree {
	
	public static final int NO_ID = Integer.MIN_VALUE;

	public static final String NO_LABEL = "";

	public static final int NO_VALUE = -1;

	private static final String OPEN_SYMBOL = "[(";
	
	private static final String CLOSE_SYMBOL = ")]";
	
	private static final String SEPARATE_SYMBOL = "@@";
	
	// Unique id of the node. Might not be useful?
	private int id;

	// Type of the token
	private int type;

	// Label of the token
	private String label;

	private Tree parent;

	private List<Tree> children;

	private int depth;

	private int digest;

	// Useless it should be implemented outside
	private boolean matched;

	private int height;

	private int size;

	// Position and length of the token in the stream
	
	private int pos;

	private int length;

	// Takes a lot of useless memory, should not be implemented this way.
	private String typeLabel;

	// Needed for Rted :(
	private Object tmpData;

	public Tree(int type) {
		this(type, NO_LABEL);
	}

	public Tree(int type, String label) {
		this(type, label, Integer.toString(type));
	}

	public Tree(int type, String label, String typeLabel) {
		this.type = type;
		this.label = (label == null ) ? "" : label;
		this.typeLabel = typeLabel;
		this.children = new ArrayList<Tree>();
		this.id = NO_ID;
		this.depth = NO_VALUE;
		this.digest = NO_VALUE;
		this.height = NO_VALUE;
		this.depth = NO_VALUE;
		this.size = NO_VALUE;
		this.matched = false;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * Indicate whether or not the tree is mappable to the given tree.
	 * @param t 
	 * @return true if both trees are not mapped and if the trees have the same type, false either.
	 */
	public boolean isMatchable(Tree t) {
		return this.isCompatible(t) && !(this.isMatched()  || t.isMatched());
	}

	/**
	 * Indicate if the trees have the same type.
	 * @param t
	 * @return
	 */
	public boolean isCompatible(Tree t) {
		return this.getType() == t.getType();
	}

	/**
	 * Indicate whether or not all the descendants of the trees are already mapped. 
	 * @return
	 */
	public boolean areDescendantsMatched() {
		for (Tree c: getDescendants()) if (!c.isMatched()) return false;
		return true;
	}

	/**
	 * Indicate whether or not the tree has children.
	 * @return
	 */
	public boolean isLeaf() {
		return getChildren().size() == 0;
	}

	/**
	 * Indicate whether or not the tree is similar to the given tree.
	 * @param t
	 * @return true if they are compatible and have same label, false either
	 */
	public boolean isSimilar(Tree t) {
		if (!this.isCompatible(t)) return false;
		else if (!this.getLabel().equals(t.getLabel())) return false;
		return true;
	}

	/**
	 * Indicates if the two trees are isomorphics.
	 * @param tree
	 * @return
	 */
	public boolean isClone(Tree tree) {
		if (this.getDigest() != tree.getDigest()) return false;
		else {
			boolean res = (this.toDigestTreeString().equals(tree.toDigestTreeString())); 
			return res;
		}
	}

	public void refreshMetrics() {
		TreeUtils.computeAllMetrics(this);
	}
	
	/**
	 * Make a deep copy of the tree.
	 * @return a deep copy of the tree.
	 */
	public Tree deepCopy() {
		Tree copy = copy();
		for (Tree child: getChildren()) copy.addChild(child.deepCopy());
		return copy;
	}

	/**
	 * Make a shallow copy of the tree.
	 * @return a shallow copy of the tree, including type, id, label, typeLabel, position and length. 
	 */
	public Tree copy() {
		Tree t = new Tree(this.getType(), this.getLabel());
		t.setTypeLabel(this.getTypeLabel());
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

	/**
	 * Return all the nodes contains in the tree, using a pre-order.
	 * @return
	 */
	public List<Tree> getTrees() {
		return TreeUtils.preOrder(this);
	}

	/**
	 * Returns all the descendants of the tree, using a pre-order.
	 * @return
	 */
	public List<Tree> getDescendants() {
		List<Tree> trees = TreeUtils.preOrder(this); 
		trees.remove(0);
		return trees;
	}

	/**
	 * Returns all the parents of the tree.
	 * @return
	 */
	public List<Tree> getParents() {
		List<Tree> parents = new ArrayList<Tree>();
		if (this.getParent() == null) return parents;
		else {
			parents.add(getParent());
			parents.addAll(getParent().getParents());
		}
		return parents;
	}

	/**
	 * Returns the position of the given child in the tree. 
	 * @param child
	 * @return the position of the child, or -1 if the given child is not in the children list.
	 */
	public int getChildPosition(Tree child) {
		return children.indexOf(child);
	}

	public String getChildrenLabels() {
		StringBuffer b = new StringBuffer();
		for (Tree child: getChildren()) if (!"".equals(child.getLabel())) b.append(child.getLabel() + " ");
		return b.toString().trim();
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

	public List<Tree> getLeaves() {
		List<Tree> leafs = new ArrayList<Tree>();
		for (Tree t: this.getTrees()) if (t.isLeaf()) leafs.add(t);
		return leafs;
	}

	@Override
	public int hashCode() {
		if (id != NO_ID) return id;
		else return super.hashCode();
	}

	public String toDigestString() {
		return getLabel() + SEPARATE_SYMBOL + getType();
	}

	public String toDigestTreeString() {
		StringBuffer b = new StringBuffer();
		b.append(OPEN_SYMBOL);
		b.append(this.toDigestString());
		for (Tree c: this.getChildren()) b.append(c.toDigestTreeString());
		b.append(CLOSE_SYMBOL);
		return b.toString();
	}

	@Override
	public String toString() {
		if (!"".equals(getLabel())) {
			return getTypeLabel() + ": " + getLabel();
		} else {
			if (!"".equals(getChildrenLabels())) return getTypeLabel() + ": " + getChildrenLabels();
			else return getTypeLabel();
		}

	}

	public String toTreeString() {
		if (isLeaf()) return this.toString();
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
		}	
	}

	public String toCompleteString() {
		return label + "@" + typeLabel + ":" + type + " [id=" + id + ", depth:" + depth + ", maxdepth=" + height + ", digest=" + digest + ", pos=" + pos + ", length=" + length + "]";
	}

	public String toCompleteTreeString() {
		if (isLeaf()) return this.toString();
		else {
			StringBuffer b = new StringBuffer();
			b.append(toString() + " (");
			for (Tree c : getChildren())
				b.append(c.toCompleteTreeString() + " ");
			b.append(")");
			return b.toString();
		}	
	}

	public int getPos() {
		return pos;
	}

	public int getEndPos() {
		return pos + length;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public List<Tree> getChildren() {
		return children;
	}

	public int getDepth() {
		return depth;
	}

	public int getDigest() {
		return digest;
	}

	public int getId() {
		return id;
	}

	public String getShortLabel() {
		return label.substring(0, Math.min(50, label.length()));
	}

	public String getLabel() {
		return label;
	}

	public int getLength() {
		return length;
	}

	public int getHeight() {
		return height;
	}

	public Tree getParent() {
		return parent;
	}

	public int getType() {
		return type;
	}

	public String getTypeLabel() {
		return typeLabel;
	}

	public boolean isMatched() {
		return matched;
	}

	public boolean isRoot() {
		return parent == null;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public void setDigest(int digest) {
		this.digest = digest;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public void setMatched(boolean matched) {
		this.matched = matched;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setParent(Tree parent) {
		this.parent = parent;
	}

	/**
	 * Add the given tree as a child, and update its parent.
	 * @param t
	 */
	public void addChild(Tree t) {
		children.add(t);
		t.setParent(this);
	}

	public void setChildren(List<Tree> children) {
		this.children = children;
		for (Tree c: children) c.setParent(this);
	}

	public void setParentAndUpdateChildren(Tree parent) {
		if (this.parent != null) this.parent.getChildren().remove(this);
		this.parent = parent;
		if (this.parent != null) parent.getChildren().add(this);
	}

	public void setType(int type) {
		this.type = type;
	}

	public void setTypeLabel(String typeLabel) {
		this.typeLabel = typeLabel;
	}

	public Object getTmpData() {
		return tmpData;
	}

	public void setTmpData(Object tmpData) {
		this.tmpData = tmpData;
	}

}
