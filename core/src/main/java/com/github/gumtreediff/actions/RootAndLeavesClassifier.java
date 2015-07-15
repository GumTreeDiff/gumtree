/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

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
