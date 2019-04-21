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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class Tree extends AbstractTree implements ITree {

    private Type type;

    private String label;

    // Begin position of the tree in terms of absolute character index and length
    private int pos;
    private int length;
    // End position

    private AssociationMap metadata;

    /**
     * Constructs a new node. If you need type labels corresponding to the integer
     * @see TreeContext#createTree(Type, String)
     */
    public Tree(Type type) {
        this(type, NO_LABEL);
    }

    /**
     * Constructs a new node. If you need type labels corresponding to the integer
     * @see TreeContext#createTree(Type, String)
     */
    public Tree(Type type, String label) {
        this.type = type;
        this.label = (label == null) ? NO_LABEL : label.intern();
        this.children = new ArrayList<>();
    }

    /**
     * Copy constructor for copy.
     */
    protected Tree(ITree other) {
        this.type = other.getType();
        this.label = other.getLabel();
        this.pos = other.getPos();
        this.length = other.getLength();
        this.children = new ArrayList<>();
    }

    @Override
    public ITree deepCopy() {
        ITree copy = new Tree(this);
        for (ITree child : getChildren())
            copy.addChild(child.deepCopy());
        return copy;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public int getPos() {
        return pos;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public void setPos(int pos) {
        this.pos = pos;
    }

    @Override
    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public Object getMetadata(String key) {
        if (metadata == null)
            return null;
        return metadata.get(key);
    }

    @Override
    public Object setMetadata(String key, Object value) {
        if (value == null) {
            if (metadata == null)
                return null;
            else
                return metadata.remove(key);
        }
        if (metadata == null)
            metadata = new AssociationMap();
        return metadata.set(key, value);
    }

    @Override
    public Iterator<Entry<String, Object>> getMetadata() {
        if (metadata == null)
            return new EmptyEntryIterator();
        return metadata.iterator();
    }
}
