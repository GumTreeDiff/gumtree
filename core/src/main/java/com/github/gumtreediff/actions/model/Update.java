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

import com.github.gumtreediff.tree.Tree;

public class Update extends Action {
    private String value;

    public Update(Tree node, String value) {
        super(node);
        this.value = value;
    }

    @Override
    public String getName() {
        return "update-node";
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return String.format("===\n%s\n---\n%s\nreplace %s by %s",
                getName(),
                node.toString(),
                node.getLabel(),
                getValue());
    }

    public boolean equals(Object o) {
        if (!(super.equals(o)))
            return false;

        Update a = (Update) o;
        return value.equals(a.value);
    }
}
