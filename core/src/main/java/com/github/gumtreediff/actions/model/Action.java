package com.github.gumtreediff.actions.model;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.ITree;

public abstract class Action {

    protected ITree node;

    public Action(ITree node) {
        this.node = node;
    }

    public ITree getNode() {
        return node;
    }

    public void setNode(ITree node) {
        this.node = node;
    }

    protected abstract String getName();

    public abstract String toString();

}
