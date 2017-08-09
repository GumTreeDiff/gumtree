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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.github.gumtreediff.utils.Pair;

public final class TreeUtils {

    private TreeUtils() {
    }

    /**
     * Compute the depth of every node of the tree. The size is set
     * directly on the nodes and is then accessible using {@link Tree#getSize()}.
     * @param tree a Tree
     */
    public static void computeSize(ITree tree) {
        for (ITree t: tree.postOrder()) {
            int size = 1;
            if (!t.isLeaf())
                for (ITree c: t.getChildren())
                    size += c.getSize();
            t.setSize(size);
        }
    }

    /**
     * Compute the depth of every node of the tree. The depth is set
     * directly on the nodes and is then accessible using {@link Tree#getDepth()}.
     * @param tree a Tree
     */
    public static void computeDepth(ITree tree) {
        List<ITree> trees = preOrder(tree);
        for (ITree t: trees) {
            int depth = 0;
            if (!t.isRoot()) depth = t.getParent().getDepth() + 1;
            t.setDepth(depth);
        }
    }

    /**
     * Compute the height of every node of the tree. The height is set
     * directly on the nodes and is then accessible using {@link Tree#getHeight()}.
     * @param tree a Tree.
     */
    public static void computeHeight(ITree tree) {
        for (ITree t: tree.postOrder()) {
            int height = 0;
            if (!t.isLeaf()) {
                for (ITree c: t.getChildren()) {
                    int cHeight = c.getHeight();
                    if (cHeight > height) height = cHeight;
                }
                height++;
            }
            t.setHeight(height);
        }
    }

    /**
     * Returns a list of every subtrees and the tree ordered using a pre-order.
     * @param tree a Tree.
     */
    public static List<ITree> preOrder(ITree tree) {
        List<ITree> trees = new ArrayList<>();
        preOrder(tree, trees);
        return trees;
    }

    private static void preOrder(ITree tree, List<ITree> trees) {
        trees.add(tree);
        if (!tree.isLeaf())
            for (ITree c: tree.getChildren())
                preOrder(c, trees);
    }

    public static void preOrderNumbering(ITree tree) {
        numbering(tree.preOrder());
    }

    /**
     * Returns a list of every subtrees and the tree ordered using a breadth-first order.
     * @param tree a Tree.
     */
    public static List<ITree> breadthFirst(ITree tree) {
        List<ITree> trees = new ArrayList<>();
        List<ITree> currents = new ArrayList<>();
        currents.add(tree);
        while (currents.size() > 0) {
            ITree c = currents.remove(0);
            trees.add(c);
            currents.addAll(c.getChildren());
        }
        return trees;
    }

    public static Iterator<ITree> breadthFirstIterator(final ITree tree) {
        return new Iterator<ITree>() {
            Deque<Iterator<ITree>> fifo = new ArrayDeque<>();

            {
                addLasts(new AbstractTree.FakeTree(tree));
            }

            @Override
            public boolean hasNext() {
                return !fifo.isEmpty();
            }

            @Override
            public ITree next() {
                while (!fifo.isEmpty()) {
                    Iterator<ITree> it = fifo.getFirst();
                    if (it.hasNext()) {
                        ITree item = it.next();
                        if (!it.hasNext())
                            fifo.removeFirst();
                        addLasts(item);
                        return item;
                    }
                }
                throw new NoSuchElementException();
            }

            private void addLasts(ITree item) {
                List<ITree> children = item.getChildren();
                if (!children.isEmpty())
                    fifo.addLast(children.iterator());
            }

            @Override
            public void remove() {
                throw new RuntimeException("Not yet implemented implemented.");
            }
        };
    }

    public static void breadthFirstNumbering(ITree tree) {
        numbering(tree.breadthFirst());
    }

    public static void numbering(Iterable<ITree> iterable) {
        int i = 0;
        for (ITree t: iterable)
            t.setId(i++);
    }

    /**
     * Returns a list of every subtrees and the tree ordered using a post-order.
     * @param tree a Tree.
     */
    public static List<ITree> postOrder(ITree tree) {
        List<ITree> trees = new ArrayList<>();
        postOrder(tree, trees);
        return trees;
    }

    private static void postOrder(ITree tree, List<ITree> trees) {
        if (!tree.isLeaf())
            for (ITree c: tree.getChildren())
                postOrder(c, trees);
        trees.add(tree);
    }

    public static Iterator<ITree> postOrderIterator(final ITree tree) {
        return new Iterator<ITree>() {
            Deque<Pair<ITree, Iterator<ITree>>> stack = new ArrayDeque<>();
            {
                push(tree);
            }

            @Override
            public boolean hasNext() {
                return stack.size() > 0;
            }

            @Override
            public ITree next() {
                if (stack.isEmpty())
                    throw new NoSuchElementException();
                return selectNextChild(stack.peek().getSecond());
            }

            ITree selectNextChild(Iterator<ITree> it) {
                if (!it.hasNext())
                    return stack.pop().getFirst();
                ITree item = it.next();
                if (item.isLeaf())
                    return item;
                return selectNextChild(push(item));
            }

            private Iterator<ITree> push(ITree item) {
                Iterator<ITree> it = item.getChildren().iterator();
                stack.push(new Pair<>(item, it));
                return it;
            }

            @Override
            public void remove() {
                throw new RuntimeException("Not yet implemented implemented.");
            }
        };
    }

    public static void visitTree(ITree root, TreeVisitor visitor) {
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

    public interface TreeVisitor {
        void startTree(ITree tree);

        void endTree(ITree tree);
    }

    public static Iterator<ITree> preOrderIterator(ITree tree) {
        return new Iterator<ITree>() {
            Deque<Iterator<ITree>> stack = new ArrayDeque<>();
            {
                push(new AbstractTree.FakeTree(tree));
            }

            @Override
            public boolean hasNext() {
                return stack.size() > 0;
            }

            @Override
            public ITree next() {
                Iterator<ITree> it = stack.peek();
                if (it == null)
                    throw new NoSuchElementException();
                ITree t = it.next();
                while (it != null && !it.hasNext()) {
                    stack.pop();
                    it = stack.peek();
                }
                push(t);
                return t;
            }

            private void push(ITree tree) {
                if (!tree.isLeaf())
                    stack.push(tree.getChildren().iterator());
            }

            @Override
            public void remove() {
                throw new RuntimeException("Not yet implemented implemented.");
            }
        };
    }

    public static Iterator<ITree> leafIterator(final Iterator<ITree> it) {
        return new Iterator<ITree>() {
            ITree current = it.hasNext() ? it.next() : null;
            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public ITree next() {
                ITree val = current;
                while (it.hasNext()) {
                    current = it.next();
                    if (current.isLeaf())
                        break;
                }
                if (!it.hasNext()) {
                    current = null;
                }
                return val;
            }

            @Override
            public void remove() {
                throw new RuntimeException("Not yet implemented implemented.");
            }
        };
    }

    public static void postOrderNumbering(ITree tree) {
        numbering(tree.postOrder());
    }
}
