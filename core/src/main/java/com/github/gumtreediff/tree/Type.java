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
 * Copyright 2015-2017 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.tree;

import static com.github.gumtreediff.tree.TypeSet.type;

public final class Type {

    public final String name;

    public static final Type NO_TYPE = type("");

    private Type(String value) {
        name = value;
    }

    public boolean isEmpty() {
        return this == NO_TYPE;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    static class TypeFactory {
        protected TypeFactory() {}

        protected Type makeType(String name) {
            return new Type(name);
        }
    }
}

