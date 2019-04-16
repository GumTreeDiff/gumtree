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

import java.util.List;
import java.util.Set;

import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.TreeContext;

public class OnlyRootsClassifier extends TreeClassifier {

    public OnlyRootsClassifier(TreeContext src, TreeContext dst, Set<Mapping> rawMappings, List<Action> actions) {
        super(src, dst, rawMappings, actions);
    }

    public OnlyRootsClassifier(TreeContext src, TreeContext dst, Matcher m) {
        super(src, dst, m);
    }

    @Override
    public void classify() {
        for (Action a: actions) {
            if (a instanceof Delete || a instanceof TreeDelete)
                srcDelTrees.add(a.getNode());
            else if (a instanceof Insert || a instanceof TreeInsert )
                dstAddTrees.add(a.getNode());
            else if (a instanceof Update) {
                srcUpdTrees.add(a.getNode());
                dstUpdTrees.add(mappings.getDstForSrc(a.getNode()));
            }
            else if (a instanceof Move) {
                srcMvTrees.add(a.getNode());
                dstMvTrees.add(mappings.getDstForSrc(a.getNode()));
            }
        }
    }

}
