/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.tree;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

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
     * Insert the given tree as the n-th child, and update its parent.
     */
    public abstract void insertChild(ITree t, int position);

    /**
     * Make a deep copy of the tree.
     * Deep copy of node however shares Metadata
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

    default int getEndPos()  {
        return getPos() + getLength();
    }

    public abstract int getHeight();

    public abstract int getId();

    public abstract boolean hasLabel();

    public abstract String getLabel();

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

    public abstract void setLength(int length);

    public abstract void setParent(ITree parent);

    public abstract void setParentAndUpdateChildren(ITree parent);

    public abstract void setPos(int pos);

    public abstract void setSize(int size);

    public abstract void setType(int type);

    public abstract String toStaticHashString();

    public abstract String toShortString();

    public String toTreeString();

    public abstract String toPrettyString(TreeContext ctx);

    Object getMetadata(String key);

    Object setMetadata(String key, Object value);

    Iterator<Entry<String, Object>> getMetadata();
}
