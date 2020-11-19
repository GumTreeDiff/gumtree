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
 * Copyright 2019 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.tree;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

public final class ImmutableTree extends AbstractTree {
    private final String label;
    private final Type type;

    private final int pos;
    private final int length;

    private final AssociationMap metadata;

    public ImmutableTree(Tree t) {
        this(t, null);
    }

    private ImmutableTree(Tree t, Tree parent) {
        children = Collections.unmodifiableList(
                t.getChildren().stream().map(c -> new ImmutableTree(c, this)).collect(Collectors.toList())
        );
        this.parent = parent;
        label = t.getLabel();
        type = t.getType();
        pos = t.getPos();
        length = t.getLength();
        metadata = new AssociationMap();
        metrics = t.getMetrics();
        for (Iterator<Map.Entry<String, Object>> it = t.getMetadata(); it.hasNext(); ) {
            Map.Entry<String, Object> e = it.next();
            this.metadata.set(e.getKey(), e.getValue());
        }
    }

    @Override
    public Tree deepCopy() {
        Tree copy = new DefaultTree(this);
        for (Tree child : getChildren())
            copy.addChild(child.deepCopy());
        return copy;
    }

    @Override
    public void setParent(Tree parent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void setLabel(String label) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getPos() {
        return pos;
    }

    @Override
    public void setPos(int pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public void setLength(int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void setType(Type type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getMetadata(String key) {
        if (metadata == null)
            return null;
        return metadata.get(key);
    }

    @Override
    public Object setMetadata(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Map.Entry<String, Object>> getMetadata() {
        if (metadata == null)
            return new EmptyEntryIterator();
        return metadata.iterator();
    }
}
