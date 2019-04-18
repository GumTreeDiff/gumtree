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
     * Returns a list containing the node and its descendants, ordered using a pre-order.
     *
     */
    Iterable<ITree> preOrder();

    /**
     * Returns a list containing the node and its descendants, ordered using a post-order.
     *
     */
    Iterable<ITree> postOrder();

    /**
     * Returns a list containing the node and its descendants, ordered using a breadth-first order.
     *
     */
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

    /**
     * Returns the child node at the given URL.
     * @param url the URL, such as <code>0.1.2</code>
     */
    ITree getChild(String url);

    /**
     * Returns a list containing the node's children. If the node has no children, the list is empty.
     * @see #isLeaf()
     * @return
     */
    List<ITree> getChildren();

    /**
     * @return a boolean indicating if the tree has at least one child or not.
     */
    boolean isLeaf();

    /**
     * @return all the descendants (children, children of children, etc.) of the tree, using a pre-order.
     *
     */
    List<ITree> getDescendants();

    /**
     * Set the parent of this node. Be careful that the parent node won't have this node in its
     * children list.
     */
    void setParent(ITree parent);

    /**
     * Set the parent of this node. The parent will have this node in its
     * children list, at the last position.
     * @see #setParentAndUpdateChildren(ITree)
     */
    void setParentAndUpdateChildren(ITree parent);

    /**
     * Returns a boolean indicating if the tree has a parent or not, and therefore is the root.
     * @return
     */
    boolean isRoot();

    /**
     * Returns the parent node of the node. If the node is a root, the method returns null.
     * @see #isRoot()
     * @return
     */
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
     * Make a deep copy of the tree. Deep copy of node however shares Metadata
     * @return
     */
    ITree deepCopy();

    /**
     * Indicates whether the node has a label or not.
     * @return
     */
    boolean hasLabel();

    /**
     * Returns the label of the node. If the node has no label, an empty string is returned.
     * @see #hasLabel()
     * @return
     */
    String getLabel();

    /**
     * Sets the label of the node.
     */
    void setLabel(String label);

    /**
     * Returns the absolute character beginning position of the node in its defining stream.
     * @return
     */
    int getPos();

    /**
     * Sets the absolute character beginning index of the node in its defining stream.
     *
     */
    void setPos(int pos);

    /**
     * Returns the number of character corresponding to the node in its defining stream.
     * @return
     */
    int getLength();

    /**
     * Sets the number of character corresponding to the node in its defining stream.
     * @return
     */
    void setLength(int length);

    /**
     * @return the absolute character index where the node ends in its defining stream.
     */
    default int getEndPos()  {
        return getPos() + getLength();
    }

    /**
     * Returns the type (i.e. IfStatement).
     * @return
     */
    Type getType();

    /**
     * Sets the type of the node (i.e. IfStatement).
     *
     */
    void setType(Type type);

    /**
     * @return a boolean indicating if the trees have the same type.
     */
    boolean hasSameType(ITree t);

    /**
     * Indicllates whether or not this node and its descendants are isomorphic to the node
     * given in parameter and its descendants (which must not be null).
     * This test fails fast.
     */
    boolean isIsomorphicTo(ITree tree);

    /**
     * Indicate whether or not the tree is similar to the given tree.
     * @return true if they are compatible and have same label, false either
     */
    boolean hasSameTypeAndLabel(ITree t);

    /**
     * Returns a string description of the node as well as its descendants.
     * @return
     */
    String toTreeString();

    TreeMetrics getMetrics();

    void setMetrics(TreeMetrics metrics);

    Object getMetadata(String key);

    Object setMetadata(String key, Object value);

    Iterator<Entry<String, Object>> getMetadata();
}
