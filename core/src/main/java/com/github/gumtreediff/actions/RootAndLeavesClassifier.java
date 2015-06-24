package com.github.gumtreediff.actions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

public class RootAndLeavesClassifier extends TreeClassifier {

    public RootAndLeavesClassifier(TreeContext src, TreeContext dst, Set<Mapping> rawMappings, List<Action> actions) {
        super(src, dst, rawMappings, actions);
    }

    public RootAndLeavesClassifier(TreeContext src, TreeContext dst, Matcher m) {
        super(src, dst, m);
    }

    @Override
    public void classify() {
        for (Action a: actions) {
            if (a instanceof Insert) {
                dstAddTrees.add(a.getNode());
            } else if (a instanceof Delete) {
                srcDelTrees.add(a.getNode());
            } else if (a instanceof Update) {
                srcUpdTrees.add(a.getNode());
                dstUpdTrees.add(mappings.getDst(a.getNode()));
            } else if (a instanceof Move) {
                srcMvTrees.add(a.getNode());
                dstMvTrees.add(mappings.getDst(a.getNode()));
            }
        }

        Set<ITree> fDstAddTrees = new HashSet<>();
        for (ITree t: dstAddTrees)
            if (!dstAddTrees.contains(t.getParent()))
                fDstAddTrees.add(t);
        dstAddTrees = fDstAddTrees;

        Set<ITree> fSrcDelTrees = new HashSet<>();
        for (ITree t: srcDelTrees) {
            if (!srcDelTrees.contains(t.getParent()))
                fSrcDelTrees.add(t);
        }
        srcDelTrees = fSrcDelTrees;

        Set<ITree> fSrcMvTrees = new HashSet<>(); // FIXME check why it's unused
        for (ITree t: srcDelTrees) {
            if (!srcDelTrees.contains(t.getParent()))
                fSrcDelTrees.add(t);
        }
        srcDelTrees = fSrcDelTrees;
    }

}
