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

package com.github.gumtreediff.actions.model;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

public abstract class Addition extends Action {

    protected ITree parent;

    protected int pos;

    public Addition(ITree node, ITree parent, int pos) {
        super(node);
        this.parent = parent;
        this.pos = pos;
    }

    public ITree getParent() {
        return parent;
    }

    public int getPosition() {
        return pos;
    }

    @Override
    public String toString() {
        return getName() + " " + node.toShortString() + " to " + parent.toShortString() + " at " + pos;
    }

    @Override
    public String format(TreeContext ctx) {
        return getName() + " " + node.toPrettyString(ctx) + " to " + parent.toPrettyString(ctx) + " at " + pos;
    }

}
