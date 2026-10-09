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

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Interface to represent abstract syntax trees.
 */
public interface Tree {
    Pattern urlPattern = Pattern.compile("\\d+(\\.\\d+)*");

    String NO_LABEL = "";

    int NO_POS = -1;

    /**
     * Returns a list containing the node and its descendants, ordered using a pre-order.
     *
     */
    default Iterable<Tree> preOrder() {
        return () -> TreeUtils.preOrderIterator(Tree.this);
    }

    /**
     * Returns a list containing the node and its descendants, ordered using a post-order.
     *
     */
    default Iterable<Tree> postOrder() {
        return () -> TreeUtils.postOrderIterator(Tree.this);
    }

    /**
     * Returns a list containing the node and its descendants, ordered using a breadth-first order.
     *
     */
    default Iterable<Tree> breadthFirst() {
        return () -> TreeUtils.breadthFirstIterator(Tree.this);
    }

    /**
     * Add the given tree as a child, at the last position and update its parent.
     */
    void addChild(Tree t);

    /**
     * Insert the given tree as the position-th child, and update its parent.
     */
    void insertChild(Tree t, int position);

    /**
     * Sets the list of children of this node.
     *
     */
    void setChildren(List<Tree> children);

    /**
     * @return the position of the child, or -1 if the given child is not in the children list.
     */
    default int getChildPosition(Tree child) {
        return getChildren().indexOf(child);
    }

    /**
     * @param position the child position, starting at 0
     */
    default Tree getChild(int position) {
        return getChildren().get(position);
    }

    /**
     * Return trees contained in a text positions interval
     * @param position the begin position
     * @param endPosition the end position (must be greater than position)
     * @return the list of trees contained in the interval, possibly empty
     */
    default List<Tree> getTreesBetweenPositions(int position, int endPosition) {
        List<Tree> trees = new ArrayList<>();
        for (Tree t: this.preOrder()) {
            if (t.getPos() >= position && t.getEndPos() <= endPosition)
                trees.add(t);
        }
        return trees;
    }

    /**
     * Returns the child node at the given URL.
     * @param url the URL, such as <code>0.1.2</code>
     */
    default Tree getChild(String url) {
        if (!urlPattern.matcher(url).matches())
            throw new IllegalArgumentException("Wrong URL format : " + url);

        List<String> path = new LinkedList<>(Arrays.asList(url.split("\\.")));
        Tree current = this;
        while (path.size() > 0) {
            int next = Integer.parseInt(path.remove(0));
            current = current.getChild(next);
        }

        return current;
    }

    /**
     * Returns a list containing the node's children. If the node has no children, the list is empty.
     * @see #isLeaf()
     */
    List<Tree> getChildren();

    /**
     * @return a boolean indicating if the tree has at least one child or not.
     */
    default boolean isLeaf() {
        return getChildren().isEmpty();
    }

    default List<Tree> searchSubtree(Tree subtree) {
        List<Tree> results = new ArrayList<>();
        for (Tree candidate : this.preOrder()) {
            if (candidate.getMetrics().hash == subtree.getMetrics().hash)
                if (candidate.isIsomorphicTo(subtree))
                    results.add(candidate);
        }
        return results;
    }

    /**
     * @return all the descendants (children, children of children, etc.) of the tree, using a pre-order.
     *
     */
    default List<Tree> getDescendants() {
        List<Tree> trees = TreeUtils.preOrder(this);
        trees.remove(0);
        return trees;
    }

    /**
     * Set the parent of this node. Be careful that the parent node won't have this node in its
     * children list.
     */
    void setParent(Tree parent);

    /**
     * Set the parent of this node. The parent will have this node in its
     * children list, at the last position.
     * @see #setParentAndUpdateChildren(Tree)
     */
    void setParentAndUpdateChildren(Tree parent);

    /**
     * Returns a boolean indicating if the tree has a parent or not, and therefore is the root.
     */
    default boolean isRoot() {
        return getParent() == null;
    }

    /**
     * Returns the parent node of the node. If the node is a root, the method returns null.
     * @see #isRoot()
     */
    Tree getParent();

    /**
     * @return the list of all parents of the node (parent, parent of parent, etc.)
     */
    default List<Tree> getParents() {
        List<Tree> parents = new ArrayList<>();
        if (getParent() == null)
            return parents;
        else {
            parents.add(getParent());
            parents.addAll(getParent().getParents());
        }
        return parents;
    }

    /**
     * @return the position of the node in its parent children list
     */
    default int positionInParent() {
        Tree p = getParent();
        if (p == null)
            return -1;
        else
            return p.getChildren().indexOf(this);
    }

    /**
     * Make a deep copy of the tree. Metadata is not copied over.
     */
    Tree deepCopy();

    /**
     * Indicates whether the node has a label or not.
     */
    default boolean hasLabel() {
        return !NO_LABEL.equals(getLabel());
    }

    /**
     * Returns the label of the node. If the node has no label, an empty string is returned.
     * @see #hasLabel()
     */
    String getLabel();

    /**
     * Sets the label of the node.
     */
    void setLabel(String label);

    /**
     * Returns the absolute character beginning position of the node in its defining stream.
     */
    int getPos();

    /**
     * Sets the absolute character beginning index of the node in its defining stream.
     *
     */
    void setPos(int pos);

    /**
     * Returns the number of character corresponding to the node in its defining stream.
     */
    int getLength();

    /**
     * Sets the number of character corresponding to the node in its defining stream.
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
    default boolean hasSameType(Tree t) {
        return getType() == t.getType();
    }

    /**
     * Indicates whether or not the tree is similar to the given tree.
     * @return true if they are compatible and have same label, false either
     */
    default boolean hasSameTypeAndLabel(Tree t) {
        return hasSameType(t) && getLabel().equals(t.getLabel());
    }

    /**
     * Indicates whether or not this node and its descendants are isomorphic to the node
     * given in parameter and its descendants (which must not be null).
     * This test fails fast.
     */
    default boolean isIsomorphicTo(Tree tree) {
        if (!hasSameTypeAndLabel(tree))
            return false;

        if (getChildren().size() != tree.getChildren().size())
            return false;

        for (int i = 0; i < getChildren().size(); i++)  {
            boolean isChildrenIsomophic = getChild(i).isIsomorphicTo(tree.getChild(i));
            if (!isChildrenIsomophic)
                return false;
        }

        return true;
    }

    /**
     * Indicates whether or not this node and its descendants are isostructural (isomorphism without labels) to the node
     * given in parameter and its descendants (which must not be null).
     * This test fails fast.
     */
    default boolean isIsoStructuralTo(Tree tree) {
        if (this.getType() != tree.getType())
            return false;

        if (getChildren().size() != tree.getChildren().size())
            return false;

        for (int i = 0; i < getChildren().size(); i++)  {
            boolean isChildrenStructural = getChild(i).isIsoStructuralTo(tree.getChild(i));
            if (!isChildrenStructural)
                return false;
        }

        return true;
    }

    /**
     * Returns a string description of the node as well as its descendants.
     */
    String toTreeString();

    /*
     * Returns the metrics object computed for this node. This object is lazily computed
     * when first requested. When metrics have been computed, the tree must remain unchanged.
     */
    TreeMetrics getMetrics();

    /**
     * Sets the metric object for this node.
     *
     */
    void setMetrics(TreeMetrics metrics);

    /**
     * Returns the metadata with the given key for this node.
     */
    Object getMetadata(String key);

    /**
     * Set the metadata with the given key and value for this node.
     */
    Object setMetadata(String key, Object value);

    /**
     * Returns an iterator for all metadata of this node.
     */
    Iterator<Entry<String, Object>> getMetadata();
}
