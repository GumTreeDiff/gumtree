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

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public final class TreeMap {

    private TIntObjectMap<ITree> trees;

    public TreeMap(ITree tree) {
        this();
        putTrees(tree);
    }

    public TreeMap() {
        trees = new TIntObjectHashMap<>();
    }

    public ITree getTree(int id) {
        return trees.get(id);
    }

    public boolean contains(ITree tree) {
        return contains(tree.getId());
    }

    public boolean contains(int id) {
        return trees.containsKey(id);
    }

    public void putTrees(ITree tree) {
        for (ITree t: tree.getTrees())
            trees.put(t.getId(), t);
    }

    public void putTree(ITree t) {
        trees.put(t.getId(), t);
    }
}
