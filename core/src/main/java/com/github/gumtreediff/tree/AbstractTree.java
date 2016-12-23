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

import com.github.gumtreediff.tree.hash.HashUtils;

import java.util.*;

public abstract class AbstractTree implements ITree {

    protected int id;

    protected ITree parent;

    protected List<ITree> children;

    protected int height;

    protected int size;

    protected int depth;

    protected int hash;

    @Override
    public int getChildPosition(ITree child) {
        return getChildren().indexOf(child);
    }

    @Override
    public ITree getChild(int position) {
        return getChildren().get(position);
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public List<ITree> getDescendants() {
        List<ITree> trees = TreeUtils.preOrder(this);
        trees.remove(0);
        return trees;
    }

    @Override
    public int getHash() {
        return hash;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public boolean hasLabel() {
        return !NO_LABEL.equals(getLabel());
    }

    @Override
    public ITree getParent() {
        return parent;
    }

    @Override
    public void setParent(ITree parent) {
        this.parent = parent;
    }

    @Override
    public List<ITree> getParents() {
        List<ITree> parents = new ArrayList<>();
        if (getParent() == null)
            return parents;
        else {
            parents.add(getParent());
            parents.addAll(getParent().getParents());
        }
        return parents;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public List<ITree> getTrees() {
        return TreeUtils.preOrder(this);
    }

    private String indent(ITree t) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < t.getDepth(); i++)
            b.append("\t");
        return b.toString();
    }

    @Override
    public boolean isIsomorphicTo(ITree tree) {
        if (this.getHash() != tree.getHash())
            return false;
        else
            return this.toStaticHashString().equals(tree.toStaticHashString());
    }

    @Override
    public boolean hasSameType(ITree t) {
        return getType() == t.getType();
    }

    @Override
    public boolean isLeaf() {
        return getChildren().size() == 0;
    }

    @Override
    public boolean isRoot() {
        return getParent() == null;
    }

    @Override
    public boolean hasSameTypeAndLabel(ITree t) {
        if (!hasSameType(t))
            return false;
        else if (!getLabel().equals(t.getLabel()))
            return false;
        return true;
    }

    @Override
    public Iterable<ITree> preOrder() {
        return new Iterable<ITree>() {
            @Override
            public Iterator<ITree> iterator() {
                return TreeUtils.preOrderIterator(AbstractTree.this);
            }
        };
    }

    @Override
    public Iterable<ITree> postOrder() {
        return new Iterable<ITree>() {
            @Override
            public Iterator<ITree> iterator() {
                return TreeUtils.postOrderIterator(AbstractTree.this);
            }
        };
    }

    @Override
    public Iterable<ITree> breadthFirst() {
        return new Iterable<ITree>() {
            @Override
            public Iterator<ITree> iterator() {
                return TreeUtils.breadthFirstIterator(AbstractTree.this);
            }
        };
    }

    @Override
    public int positionInParent() {
        ITree p = getParent();
        if (p == null)
            return -1;
        else
            return p.getChildren().indexOf(this);
    }

    @Override
    public void refresh() {
        TreeUtils.computeSize(this);
        TreeUtils.computeDepth(this);
        TreeUtils.computeHeight(this);
        HashUtils.DEFAULT_HASH_GENERATOR.hash(this);
    }

    @Override
    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public void setHash(int digest) {
        this.hash = digest;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public String toStaticHashString() {
        StringBuilder b = new StringBuilder();
        b.append(OPEN_SYMBOL);
        b.append(this.toShortString());
        for (ITree c: this.getChildren())
            b.append(c.toStaticHashString());
        b.append(CLOSE_SYMBOL);
        return b.toString();
    }

    @Override
    public String toString() {
        System.err.println("This method should currently not be used (please use toShortString())");
        return toShortString();
    }

    @Override
    public String toShortString() {
        return String.format("%d%s%s", getType(), SEPARATE_SYMBOL, getLabel());
    }

    @Override
    public String toTreeString() {
        StringBuilder b = new StringBuilder();
        for (ITree t : TreeUtils.preOrder(this))
            b.append(indent(t) + t.toShortString() + "\n");
        return b.toString();
    }

    @Override
    public String toPrettyString(TreeContext ctx) {
        if (hasLabel())
            return ctx.getTypeLabel(this) + ": " + getLabel();
        else
            return ctx.getTypeLabel(this);
    }

    public static class FakeTree extends AbstractTree {
        public FakeTree(ITree... trees) {
            children = new ArrayList<>(trees.length);
            children.addAll(Arrays.asList(trees));
        }

        private RuntimeException unsupportedOperation() {
            return new UnsupportedOperationException("This method should not be called on a fake tree");
        }

        @Override
        public void addChild(ITree t) {
            throw unsupportedOperation();
        }

        @Override
        public void insertChild(ITree t, int position) {
            throw unsupportedOperation();
        }

        @Override
        public ITree deepCopy() {
            throw unsupportedOperation();
        }

        @Override
        public List<ITree> getChildren() {
            return children;
        }

        @Override
        public String getLabel() {
            return NO_LABEL;
        }

        @Override
        public int getLength() {
            return getEndPos() - getPos();
        }

        @Override
        public int getPos() {
            return Collections.min(children, (t1, t2) -> t2.getPos() - t1.getPos()).getPos();
        }

        @Override
        public int getEndPos() {
            return Collections.max(children, (t1, t2) -> t2.getPos() - t1.getPos()).getEndPos();
        }

        @Override
        public int getType() {
            return -1;
        }

        @Override
        public void setChildren(List<ITree> children) {
            throw unsupportedOperation();
        }

        @Override
        public void setLabel(String label) {
            throw unsupportedOperation();
        }

        @Override
        public void setLength(int length) {
            throw unsupportedOperation();
        }

        @Override
        public void setParentAndUpdateChildren(ITree parent) {
            throw unsupportedOperation();
        }

        @Override
        public void setPos(int pos) {
            throw unsupportedOperation();
        }

        @Override
        public void setType(int type) {
            throw unsupportedOperation();
        }

        @Override
        public String toPrettyString(TreeContext ctx) {
            return "FakeTree";
        }

        /**
         * fake nodes have no metadata
         */
        @Override
        public Object getMetadata(String key) {
            return null;
        }

        /**
         * fake node store no metadata
         */
        @Override
        public Object setMetadata(String key, Object value) {
            return null;
        }

        /**
         * Since they have no metadata they do not iterate on nothing
         */
        @Override
        public Iterator<Map.Entry<String, Object>> getMetadata() {
            return new EmptyEntryIterator();
        }
    }

    protected static class EmptyEntryIterator implements Iterator<Map.Entry<String, Object>> {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Map.Entry<String, Object> next() {
            throw new NoSuchElementException();
        }
    }
}
