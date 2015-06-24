package com.github.gumtreediff.actions.model;

import com.github.gumtreediff.tree.ITree;

public class Delete extends Action {

    public Delete(ITree node) {
        super(node);
    }

    @Override
    protected String getName() {
        return "DEL";
    }

    @Override
    public String toString() {
        return getName() + " " + node.toShortString();
    }

}
