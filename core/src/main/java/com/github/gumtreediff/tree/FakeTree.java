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

public class FakeTree extends AbstractTree {
    public FakeTree(Tree... trees) {
        children = new ArrayList<>(trees.length);
        children.addAll(Arrays.asList(trees));
    }

    private RuntimeException unsupportedOperation() {
        return new UnsupportedOperationException("This method should not be called on a fake tree");
    }

    @Override
    public Tree deepCopy() {
        Tree copy = new FakeTree();
        for (Tree child : getChildren())
            copy.addChild(child.deepCopy());
        return copy;
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
    public void setLabel(String label) {
        throw unsupportedOperation();
    }

    @Override
    public void setLength(int length) {
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
        throw unsupportedOperation();
    }

    /**
     * Since they have no metadata they do not iterate on nothing
     */
    @Override
    public Iterator<Map.Entry<String, Object>> getMetadata() {
        return new EmptyEntryIterator();
    }
}
