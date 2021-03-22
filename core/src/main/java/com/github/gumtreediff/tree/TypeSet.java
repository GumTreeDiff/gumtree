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

import java.util.HashMap;
import java.util.Map;

/**
 * Class dedicated to construct AST types.
 *
 * @see Type
 */
public class TypeSet {
    private static final TypeFactoryImplementation implementation = new TypeFactoryImplementation();

    private TypeSet() {}

    /**
     * Build a type with the provided name. If the provided name is null or
     * the empty string, the empty type will be returned.
     */
    public static Type type(String value) {
        return implementation.makeOrGetType(value);
    }

    private static class TypeFactoryImplementation extends Type.TypeFactory {
        private final Map<String, Type> types = new HashMap<>();

        public Type makeOrGetType(String name) {
//            return types.computeIfAbsent(name == null ? "" : name, (key) -> makeType(key));
            if (name == null)
                name = "";

            Type sym = types.get(name);
            if (sym == null) {
                sym = makeType(name);
                types.put(name, sym);
            }

            return sym;
        }
    }
}
