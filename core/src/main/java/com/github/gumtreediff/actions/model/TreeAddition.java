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

package com.github.gumtreediff.actions.model;

import com.github.gumtreediff.tree.Tree;

public abstract class TreeAddition extends TreeAction {
    protected Tree parent;

    protected int pos;

    public TreeAddition(Tree node, Tree parent, int pos) {
        super(node);
        this.parent = parent;
        this.pos = pos;
    }

    public Tree getParent() {
        return parent;
    }

    public int getPosition() {
        return pos;
    }

    @Override
    public String toString() {
        return String.format("===\n%s\n---\n%s\nto\n%s\nat %d",
                getName(),
                node.toTreeString(),
                (parent != null) ? parent.toString() : "root",
                pos);
    }

    public boolean equals(Object o) {
        if (!(super.equals(o)))
            return false;

        TreeAddition a = (TreeAddition) o;
        return parent == a.parent && pos == a.pos;
    }
}
