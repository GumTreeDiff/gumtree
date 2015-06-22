package fr.labri.gumtree.actions.model;

import fr.labri.gumtree.tree.ITree;

public abstract class Addition extends Action {

    protected ITree parent;

    protected int pos;

    public Addition(ITree node, ITree parent, int pos) {
        super(node);
        this.parent = parent;
        this.pos = pos;
    }

    public ITree getParent() {
        return parent;
    }

    public int getPosition() {
        return pos;
    }

    @Override
    public String toString() {
        return getName() + " " + node.toTreeString() + " to " + parent.toShortString() + " at " + pos;
    }

}
