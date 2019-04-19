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

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

public abstract class TreeClassifier {

    protected final Set<ITree> srcUpdTrees = new HashSet<>();

    protected final Set<ITree> dstUpdTrees = new HashSet<>();

    protected final Set<ITree> srcMvTrees = new HashSet<>();

    protected final Set<ITree> dstMvTrees = new HashSet<>();

    protected final Set<ITree> srcDelTrees = new HashSet<>();

    protected final Set<ITree> dstAddTrees = new HashSet<>();

    protected final MappingStore mappings;

    protected final List<Action> actions;

    public TreeClassifier(MappingStore mappings) {
        this.mappings = new MappingStore(mappings); // FIXME Why a copy ?
        ActionGenerator g = new ActionGenerator(mappings);
        g.generate();
        this.actions = g.getActions();
        classify();
    }

    public abstract void classify();

    public Set<ITree> getSrcUpdTrees() {
        return srcUpdTrees;
    }

    public Set<ITree> getDstUpdTrees() {
        return dstUpdTrees;
    }

    public Set<ITree> getSrcMvTrees() {
        return srcMvTrees;
    }

    public Set<ITree> getDstMvTrees() {
        return dstMvTrees;
    }

    public Set<ITree> getSrcDelTrees() {
        return srcDelTrees;
    }

    public Set<ITree> getDstAddTrees() {
        return dstAddTrees;
    }

}
