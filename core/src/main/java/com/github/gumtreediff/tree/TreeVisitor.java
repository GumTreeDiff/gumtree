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

import com.github.gumtreediff.utils.Pair;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Interface for AST visitors.
 */
public interface TreeVisitor {
    /**
     * Start visiting the given tree using the given visitor.
     * The tree is visited in post-order.
     */
    static void visitTree(Tree root, TreeVisitor visitor) {
        Deque<Pair<Tree, Iterator<Tree>>> stack = new ArrayDeque<>();
        stack.push(new Pair<>(root, root.getChildren().iterator()));
        visitor.startTree(root);
        while (!stack.isEmpty()) {
            Pair<Tree, Iterator<Tree>> it = stack.peek();

            if (!it.second.hasNext()) {
                visitor.endTree(it.first);
                stack.pop();
            }
            else {
                Tree child = it.second.next();
                stack.push(new Pair<>(child, child.getChildren().iterator()));
                visitor.startTree(child);
            }
        }
    }

    /**
     * Callback executed when entering a node during the visit. The visited node
     * is provided as a parameter of the callback.
     */
    void startTree(Tree tree);

    /**
     * Callback executed when exiting a node during the visit. The visited node
     * is provided as a parameter of the callback.
     */
    void endTree(Tree tree);

    /**
     * A default does nothing visitor.
     */
    class DefaultTreeVisitor implements TreeVisitor {
        @Override
        public void startTree(Tree tree) {
        }

        @Override
        public void endTree(Tree tree) {
        }
    }

    /**
     * A does nothing visitor that distinguished between visiting an inner node
     * and visiting a leaf node.
     */
    class InnerNodesAndLeavesVisitor implements TreeVisitor {
        /**
         * {@inheritDoc}
         * This method is final, users have to use the following methods to customize
         * their visitor.
         * @see #startInnerNode(Tree)
         * @see #visitLeaf(Tree)
         */
        @Override
        public final void startTree(Tree tree) {
            if (tree.isLeaf())
                visitLeaf(tree);
            else
                startInnerNode(tree);
        }

        /**
         * {@inheritDoc}
         * This method is final, users have to use the following methods to customize
         * their visitor.
         * @see #endInnerNode(Tree)
         * @see #visitLeaf(Tree)
         */
        @Override
        public final void endTree(Tree tree) {
            if (!tree.isLeaf())
                endInnerNode(tree);
        }

        /**
         * Callback executed when entering an inner node during the visit. The visited node
         * is provided as a parameter of the callback.
         */
        public void startInnerNode(Tree tree) {
        }

        /**
         * Callback executed when visiting a leaf node during the visit. The visited node
         * is provided as a parameter of the callback.
         */
        public void visitLeaf(Tree tree) {
        }

        /**
         * Callback executed when entering an inner node during the visit. The visited node
         * is provided as a parameter of the callback.
         */
        public void endInnerNode(Tree tree) {
        }
    }
}
