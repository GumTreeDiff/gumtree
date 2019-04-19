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

import com.github.gumtreediff.io.TreeIoUtils;

import java.util.*;
import java.util.regex.Pattern;

public abstract class AbstractTree implements ITree {
    protected ITree parent;

    protected List<ITree> children;

    protected TreeMetrics metrics;

    @Override
    public ITree getParent() {
        return parent;
    }

    @Override
    public void setParent(ITree parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        if (hasLabel())
            return String.format("%s: %s [%d,%d]",
                    getType(), getLabel(), getPos(), getEndPos());
        else
            return String.format("%s [%d,%d]",
                    getType(), getPos(), getEndPos());
    }

    @Override
    public String toTreeString() {
        return TreeIoUtils.toShortText(this).toString();
    }

    public TreeMetrics getMetrics() {
        if (metrics == null) {
            ITree root = this;
            if (!this.isRoot()) {
                List<ITree> parents = this.getParents();
                root = parents.get(parents.size() - 1);
            }
            TreeVisitor.visitTree(root, new TreeMetricComputer());
        }

        return metrics;
    }

    public void setMetrics(TreeMetrics metrics) {
        this.metrics = metrics;
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
        public Type getType() {
            return Type.NO_TYPE;
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
        public void setType(Type type) {
            throw unsupportedOperation();
        }

        @Override
        public String toString() {
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
