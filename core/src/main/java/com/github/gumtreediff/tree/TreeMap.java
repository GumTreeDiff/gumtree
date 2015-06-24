package com.github.gumtreediff.tree;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class TreeMap {

    private TIntObjectMap<ITree> trees;

    public TreeMap(ITree tree) {
        trees = new TIntObjectHashMap<>();
        for (ITree t: tree.getTrees())
            trees.put(t.getId(), t);
    }

    public ITree getTree(int id) {
        return trees.get(id);
    }

}
