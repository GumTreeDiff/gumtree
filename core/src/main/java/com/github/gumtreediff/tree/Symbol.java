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

public class Symbol {
    private static final Map<String, Symbol> symbols = new HashMap<>();

    public final String name;

    public static final Symbol NO_SYMBOL = symbol("");

    private Symbol(String value) {
        name = value;
    }

    public static Symbol symbol(String value) {
        return symbols.computeIfAbsent(value == null ? "" : value, (key) -> new Symbol(key));
    }

    public boolean isEmpty() {
        return this == NO_SYMBOL;
    }

    @Override
    public String toString() {
        return name;
    }
}

