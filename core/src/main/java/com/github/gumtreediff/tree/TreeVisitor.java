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

public interface TreeVisitor {
    static void visitTree(ITree root, TreeVisitor visitor) {
        Deque<Pair<ITree, Iterator<ITree>>> stack = new ArrayDeque<>();
        stack.push(new Pair<>(root, root.getChildren().iterator()));
        visitor.startTree(root);
        while (!stack.isEmpty()) {
            Pair<ITree, Iterator<ITree>> it = stack.peek();

            if (!it.second.hasNext()) {
                visitor.endTree(it.first);
                stack.pop();
            } else {
                ITree child = it.second.next();
                stack.push(new Pair<>(child, child.getChildren().iterator()));
                visitor.startTree(child);
            }
        }
    }

    void startTree(ITree tree);

    void endTree(ITree tree);

    class DefaultTreeVisitor implements TreeVisitor {
        @Override
        public void startTree(ITree tree) {

        }

        @Override
        public void endTree(ITree tree) {

        }
    }

    class InnerNodesAndLeavesVisitor implements TreeVisitor {
        @Override
        public final void startTree(ITree tree) {
            if (tree.isLeaf())
                visitLeave(tree);
            else
                startInnerNode(tree);
        }

        public void startInnerNode(ITree tree) {

        }

        public void visitLeave(ITree tree) {
        }

        @Override
        public final void endTree(ITree tree) {
            if (!tree.isLeaf())
                endInnerNode(tree);
        }

        public void endInnerNode(ITree tree) {

        }
    }
}
