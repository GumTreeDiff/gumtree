package com.github.gumtreediff.tree;

import java.util.List;

public interface ITree {

    // Begin constants
    public static final String OPEN_SYMBOL = "[(";
    public static final String CLOSE_SYMBOL = ")]";
    public static final String SEPARATE_SYMBOL = "@@";
    public static final int NO_ID = Integer.MIN_VALUE;
    public static final String NO_LABEL = "";
    public static final int NO_VALUE = -1;

    /**
     * Add the given tree as a child, and update its parent.
     */
    public abstract void addChild(ITree t);

    /**
     * Indicate whether or not all the descendants of the trees are already mapped.
     */
    public abstract boolean areDescendantsMatched();

    /**
     * Make a deep copy of the tree.
     * @return a deep copy of the tree.
     */
    public abstract ITree deepCopy();

    /**
     * Returns the position of the given child in the tree.
     * @return the position of the child, or -1 if the given child is not in the children list.
     */
    public abstract int getChildPosition(ITree child);

    public abstract List<ITree> getChildren();

    public abstract ITree getChild(int position);

    public abstract String getChildrenLabels();

    public abstract int getDepth();

    /**
     * Returns all the descendants of the tree, using a pre-order.
     */
    public abstract List<ITree> getDescendants();

    public abstract int getHash();

    public abstract int getEndPos();

    public abstract int getHeight();

    public abstract int getId();

    public abstract boolean hasLabel();

    public abstract String getLabel();

    public abstract int[] getLcPosEnd();

    public abstract int[] getLcPosStart();

    public abstract List<ITree> getLeaves();

    public abstract int getLength();

    public abstract ITree getParent();

    /**
     * Returns all the parents of the tree.
     */
    public abstract List<ITree> getParents();

    public abstract int getPos();

    public abstract String getShortLabel();

    public abstract int getSize();

    public abstract Object getTmpData();

    /**
     * Return all the nodes contains in the tree, using a pre-order.
     */
    public abstract List<ITree> getTrees();

    public abstract int getType();

    /**
     * Indicates if the two trees are isomorphics.
     */
    public abstract boolean isClone(ITree tree);

    /**
     * Indicate if the trees have the same type.
     */
    public abstract boolean isCompatible(ITree t);

    /**
     * Indicate whether or not the tree has children.
     */
    public abstract boolean isLeaf();

    /**
     * Indicate whether or not the tree is mappable to the given tree.
     * @return true if both trees are not mapped and if the trees have the same type, false either.
     */
    public abstract boolean isMatchable(ITree t);

    public abstract boolean isMatched();

    public abstract boolean isRoot();

    /**
     * Indicate whether or not the tree is similar to the given tree.
     * @return true if they are compatible and have same label, false either
     */
    public abstract boolean isSimilar(ITree t);

    public abstract Iterable<ITree> preOrder();

    public abstract Iterable<ITree> postOrder();

    public abstract Iterable<ITree> breadthFirst();

    public abstract int positionInParent();

    public abstract void refresh();

    public abstract void setChildren(List<ITree> children);

    public abstract void setDepth(int depth);

    public abstract void setHash(int hash);

    public abstract void setHeight(int height);

    public abstract void setId(int id);

    public abstract void setLabel(String label);

    public abstract void setLcPosEnd(int[] lcPosEnd);

    public abstract void setLcPosStart(int[] lcPosStart);

    public abstract void setLength(int length);

    public abstract void setMatched(boolean matched);

    public abstract void setParent(ITree parent);

    public abstract void setParentAndUpdateChildren(ITree parent);

    public abstract void setPos(int pos);

    public abstract void setSize(int size);

    public abstract void setTmpData(Object tmpData);

    public abstract void setType(int type);

    public abstract String toStaticHashString();

    public abstract String toShortString();

    public String toTreeString();

    public abstract String toPrettyString(TreeContext ctx);

    interface TreeInfo {
    }
}
