package fr.labri.gumtree.actions.model;

import fr.labri.gumtree.tree.ITree;

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
