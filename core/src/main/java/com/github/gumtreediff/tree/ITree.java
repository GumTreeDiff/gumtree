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

/**
 * Interface to represent abstract syntax trees.
 */
public interface ITree {

    String NO_LABEL = "";

    int NO_POS = -1;

    /**
     * @return all the nodes contained in the tree, using a pre-order.
     */
    List<ITree> getTrees();

    Iterable<ITree> preOrder();

    Iterable<ITree> postOrder();

    Iterable<ITree> breadthFirst();

    /**
     * Add the given tree as a child, and update its parent.
     */
    void addChild(ITree t);

    /**
     * Insert the given tree as the position-th child, and update its parent.
     */
    void insertChild(ITree t, int position);

    void setChildren(List<ITree> children);

    /**
     * @return the position of the child, or -1 if the given child is not in the children list.
     */
    int getChildPosition(ITree child);

    /**
     * @param position the child position, starting at 0
     */
    ITree getChild(int position);

    List<ITree> getChildren();

    /**
     * @return a boolean indicating if the tree has at least one child or not
     */
    boolean isLeaf();

    /**
     * @return all the descendants (children, children of children, etc.) of the tree,
     *     using a pre-order.
     */
    List<ITree> getDescendants();

    /**
     * Set the parent of this node. The parent won't have this node in its
     * children list
     */
    void setParent(ITree parent);

    /**
     * Set the parent of this node. The parent will have this node in its
     * children list, at the last position
     */
    void setParentAndUpdateChildren(ITree parent);

    /**
     * @return a boolean indicating if the tree has a parent or not
     */
    boolean isRoot();

    ITree getParent();

    /**
     * @return the list of all parents of the node (parent, parent of parent, etc.)
     */
    List<ITree> getParents();

    /**
     * @return the position of the node in its parent children list
     */
    int positionInParent();

    /**
     * Make a deep copy of the tree.
     * Deep copy of node however shares Metadata
     * @return a deep copy of the tree.
     */
    ITree deepCopy();

    boolean hasLabel();

    String getLabel();

    void setLabel(String label);

    int getPos();

    void setPos(int pos);

    int getLength();

    void setLength(int length);

    /**
     * @return the absolute character index where the tree ends
     */
    default int getEndPos()  {
        return getPos() + getLength();
    }

    Symbol getType();

    void setType(Symbol type);

    /**
     * @return a boolean indicating if the trees have the same type.
     */
    boolean hasSameType(ITree t);

    /**
     * @return a boolean indicating if the two trees are isomorphics, defined has
     *     having the same hash and the same hash serialization.
     */
    boolean isIsomorphicTo(ITree tree);

    /**
     * Indicate whether or not the tree is similar to the given tree.
     * @return true if they are compatible and have same label, false either
     */
    boolean hasSameTypeAndLabel(ITree t);

    String toTreeString();

    String toPrettyTreeString(TreeContext ctx);

    Object getMetadata(String key);

    Object setMetadata(String key, Object value);

    Iterator<Entry<String, Object>> getMetadata();
}
