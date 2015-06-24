package com.github.gumtreediff.actions.model;

import com.github.gumtreediff.tree.ITree;

public class Move extends Addition {

    public Move(ITree node, ITree parent, int pos) {
        super(node, parent, pos);
    }

    @Override
    protected String getName() {
        return "MOV";
    }

}
