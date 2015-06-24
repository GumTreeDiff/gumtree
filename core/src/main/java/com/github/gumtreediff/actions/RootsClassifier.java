package com.github.gumtreediff.actions;

import java.util.List;
import java.util.Set;

import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.TreeContext;

public class RootsClassifier extends TreeClassifier {

    public RootsClassifier(TreeContext src, TreeContext dst, Set<Mapping> rawMappings, List<Action> script) {
        super(src, dst, rawMappings, script);
    }

    public RootsClassifier(TreeContext src, TreeContext dst, Matcher m) {
        super(src, dst, m);
    }

    public void classify() {
        for (Action a: actions) {
            if (a instanceof Delete) srcDelTrees.add(a.getNode());
            else if (a instanceof Insert)
                dstAddTrees.add(a.getNode());
            else if (a instanceof Update) {
                srcUpdTrees.add(a.getNode());
                dstUpdTrees.add(mappings.getDst(a.getNode()));
            } else if (a instanceof Move) {
                srcMvTrees.add(a.getNode());
                dstMvTrees.add(mappings.getDst(a.getNode()));
            }
        }
    }


}
