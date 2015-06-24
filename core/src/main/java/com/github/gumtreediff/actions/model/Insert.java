package com.github.gumtreediff.actions.model;

        import com.github.gumtreediff.tree.ITree;
        import com.github.gumtreediff.tree.ITree;

public class Insert extends Addition {

    public Insert(ITree node, ITree parent, int pos) {
        super(node, parent, pos);
    }

    @Override
    protected String getName() {
        return "INS";
    }


}
