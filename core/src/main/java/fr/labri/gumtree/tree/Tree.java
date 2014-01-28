package fr.labri.gumtree.tree;

import java.util.ArrayList;
import java.util.List;

public class Tree {
	
	// Begin constants
	public static final int NO_ID = Integer.MIN_VALUE;
	public static final String NO_LABEL = "";
	public static final int NO_VALUE = -1;
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
	private Tree parent;
	private List<Tree> children;
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

	// Takes a lot of useless memory, should not be implemented this way.
	//TODO fix implementation of type label.
	private String typeLabel;
	
	// Needed for Rted :(
	private Object tmpData;
	
	public Tree(int type) {
		this(type, NO_LABEL);
	}

	public Tree(int type, String label) {
		this(type, label, NO_LABEL);
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
		this.pos = NO_VALUE;
		this.length = NO_VALUE;
		this.matched = false;
	}

	/**
	 * Add the given tree as a child, and update its parent.
	 * @param t
	 */
	public void addChild(Tree t) {
		children.add(t);
		t.setParent(this);
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
	 * Make a deep copy of the tree.
	 * @return a deep copy of the tree.
	 */
	public Tree deepCopy() {
		Tree copy = copy();
		for (Tree child: getChildren()) copy.addChild(child.deepCopy());
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

	/**
	 * Returns the position of the given child in the tree. 
	 * @param child
	 * @return the position of the child, or -1 if the given child is not in the children list.
	 */
	public int getChildPosition(Tree child) {
		return children.indexOf(child);
	}

	public List<Tree> getChildren() {
		return children;
	}

	public String getChildrenLabels() {
		StringBuffer b = new StringBuffer();
		for (Tree child: getChildren()) if (!"".equals(child.getLabel())) b.append(child.getLabel() + " ");
		return b.toString().trim();
	}

	public int getDepth() {
		return depth;
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

	public int getDigest() {
		return digest;
	}

	public int getEndPos() {
		return pos + length;
	}

	public int getHeight() {
		return height;
	}
	
	public int getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public int[] getLcPosEnd() {
		return lcPosEnd;
	}

	public int[] getLcPosStart() {
		return lcPosStart;
	}

	public List<Tree> getLeaves() {
		List<Tree> leafs = new ArrayList<Tree>();
		for (Tree t: this.getTrees()) if (t.isLeaf()) leafs.add(t);
		return leafs;
	}

	public int getLength() {
		return length;
	}

	public Tree getParent() {
		return parent;
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

	public int getPos() {
		return pos;
	}

	public String getShortLabel() {
		return label.substring(0, Math.min(50, label.length()));
	}

	public int getSize() {
		return size;
	}

	public Object getTmpData() {
		return tmpData;
	}

	/**
	 * Return all the nodes contains in the tree, using a pre-order.
	 * @return
	 */
	public List<Tree> getTrees() {
		return TreeUtils.preOrder(this);
	}

	public int getType() {
		return type;
	}
	
	public String getTypeLabel() {
		return typeLabel;
	}

	@Override
	public int hashCode() {
		if (id != NO_ID) return id;
		else return super.hashCode();
	}

	private String indent(Tree t) {
		StringBuffer b = new StringBuffer();
		for (int i = 0; i < t.getDepth(); i++) b.append("\t");
		return b.toString();
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

	/**
	 * Indicate if the trees have the same type.
	 * @param t
	 * @return
	 */
	public boolean isCompatible(Tree t) {
		return this.getType() == t.getType();
	}

	/**
	 * Indicate whether or not the tree has children.
	 * @return
	 */
	public boolean isLeaf() {
		return getChildren().size() == 0;
	}

	/**
	 * Indicate whether or not the tree is mappable to the given tree.
	 * @param t 
	 * @return true if both trees are not mapped and if the trees have the same type, false either.
	 */
	public boolean isMatchable(Tree t) {
		return this.isCompatible(t) && !(this.isMatched()  || t.isMatched());
	}

	public boolean isMatched() {
		return matched;
	}

	public boolean isRoot() {
		return parent == null;
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

	public int positionInParent() {
		if (parent == null)
			return -1;
		else
			return parent.children.indexOf(this);
	}

	public void refresh() {
		TreeUtils.computeSize(this);
		TreeUtils.computeDepth(this);
		TreeUtils.computeHeight(this);
		TreeUtils.computeDigest(this);
	}

	public void setChildren(List<Tree> children) {
		this.children = children;
		for (Tree c: children) c.setParent(this);
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public void setDigest(int digest) {
		this.digest = digest;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setLcPosEnd(int[] lcPosEnd) {
		this.lcPosEnd = lcPosEnd;
	}

	public void setLcPosStart(int[] lcPosStart) {
		this.lcPosStart = lcPosStart;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public void setMatched(boolean matched) {
		this.matched = matched;
	}

	public void setParent(Tree parent) {
		this.parent = parent;
	}

	public void setParentAndUpdateChildren(Tree parent) {
		if (this.parent != null) this.parent.getChildren().remove(this);
		this.parent = parent;
		if (this.parent != null) parent.getChildren().add(this);
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public void setTmpData(Object tmpData) {
		this.tmpData = tmpData;
	}

	public void setType(int type) {
		this.type = type;
	}

	public void setTypeLabel(String typeLabel) {
		this.typeLabel = typeLabel;
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
			/*if (!"".equals(getChildrenLabels())) return getTypeLabel() + ": " + getChildrenLabels();
			else*/ return getTypeLabel();
		}

	}

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
		for (Tree t : TreeUtils.preOrder(this)) b.append(indent(t) + t.toString() + "\n");
		return b.toString();
	}
}
