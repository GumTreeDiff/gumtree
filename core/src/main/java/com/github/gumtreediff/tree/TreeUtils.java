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

/**
 * Class providing static utility tree methods.
 * This class is not designed to be instantiated.
 */
public final class TreeUtils {
    private TreeUtils() {
    }

    /**
     * Returns a list of every subtrees and the tree ordered using a pre-order.
     * @param tree a Tree.
     */
    public static List<Tree> preOrder(Tree tree) {
        List<Tree> trees = new ArrayList<>();
        preOrder(tree, trees);
        return trees;
    }

    private static void preOrder(Tree tree, List<Tree> trees) {
        trees.add(tree);
        if (!tree.isLeaf())
            for (Tree c: tree.getChildren())
                preOrder(c, trees);
    }

    /**
     * Returns a list of every subtrees and the tree ordered using a breadth-first order.
     * @param tree a Tree.
     */
    public static List<Tree> breadthFirst(Tree tree) {
        List<Tree> trees = new ArrayList<>();
        List<Tree> currents = new ArrayList<>();
        currents.add(tree);
        while (currents.size() > 0) {
            Tree c = currents.remove(0);
            trees.add(c);
            currents.addAll(c.getChildren());
        }
        return trees;
    }

    /**
     * Return an iterator on the provided tree that will processes the node
     * in a breadth-first fashion.
     */
    public static Iterator<Tree> breadthFirstIterator(final Tree tree) {
        return new Iterator<Tree>() {
            Deque<Iterator<Tree>> fifo = new ArrayDeque<>();

            {
                addLasts(new FakeTree(tree));
            }

            @Override
            public boolean hasNext() {
                return !fifo.isEmpty();
            }

            @Override
            public Tree next() {
                while (!fifo.isEmpty()) {
                    Iterator<Tree> it = fifo.getFirst();
                    if (it.hasNext()) {
                        Tree item = it.next();
                        if (!it.hasNext())
                            fifo.removeFirst();
                        addLasts(item);
                        return item;
                    }
                }
                throw new NoSuchElementException();
            }

            private void addLasts(Tree item) {
                List<Tree> children = item.getChildren();
                if (!children.isEmpty())
                    fifo.addLast(children.iterator());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns a list of every subtrees and the tree ordered using a post-order.
     * @param tree a Tree.
     */
    public static List<Tree> postOrder(Tree tree) {
        List<Tree> trees = new ArrayList<>();
        postOrder(tree, trees);
        return trees;
    }

    private static void postOrder(Tree tree, List<Tree> trees) {
        if (!tree.isLeaf())
            for (Tree c: tree.getChildren())
                postOrder(c, trees);
        trees.add(tree);
    }

    /**
     * Return an iterator on the provided tree that will process the node
     * in a post-order fashion.
     */
    public static Iterator<Tree> postOrderIterator(final Tree tree) {
        return new Iterator<Tree>() {
            Deque<Pair<Tree, Iterator<Tree>>> stack = new ArrayDeque<>();
            {
                push(tree);
            }

            @Override
            public boolean hasNext() {
                return stack.size() > 0;
            }

            @Override
            public Tree next() {
                if (stack.isEmpty())
                    throw new NoSuchElementException();
                return selectNextChild(stack.peek().second);
            }

            Tree selectNextChild(Iterator<Tree> it) {
                if (!it.hasNext())
                    return stack.pop().first;
                Tree item = it.next();
                if (item.isLeaf())
                    return item;
                return selectNextChild(push(item));
            }

            private Iterator<Tree> push(Tree item) {
                Iterator<Tree> it = item.getChildren().iterator();
                stack.push(new Pair<>(item, it));
                return it;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Return an iterator on the provided tree that will process the node
     * in a pre-order fashion.
     */
    public static Iterator<Tree> preOrderIterator(Tree tree) {
        return new Iterator<Tree>() {
            Deque<Iterator<Tree>> stack = new ArrayDeque<>();
            {
                push(new FakeTree(tree));
            }

            @Override
            public boolean hasNext() {
                return stack.size() > 0;
            }

            @Override
            public Tree next() {
                Iterator<Tree> it = stack.peek();
                if (it == null)
                    throw new NoSuchElementException();
                Tree t = it.next();
                while (it != null && !it.hasNext()) {
                    stack.pop();
                    it = stack.peek();
                }
                push(t);
                return t;
            }

            private void push(Tree tree) {
                if (!tree.isLeaf())
                    stack.push(tree.getChildren().iterator());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Iterator<Tree> leafIterator(final Iterator<Tree> it) {
        return new Iterator<Tree>() {
            Tree current = it.hasNext() ? it.next() : null;
            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public Tree next() {
                Tree val = current;
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
                throw new UnsupportedOperationException();
            }
        };
    }
}
