package fr.labri.gumtree.actions.model;

import fr.labri.gumtree.tree.ITree;

public class Move extends Addition {

    public Move(ITree node, ITree parent, int pos) {
        super(node, parent, pos);
    }

    @Override
    protected String getName() {
        return "MOV";
    }

}
