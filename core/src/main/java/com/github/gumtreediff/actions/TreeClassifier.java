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

    protected Set<ITree> srcUpdTrees;

    protected Set<ITree> dstUpdTrees;

    protected Set<ITree> srcMvTrees;

    protected Set<ITree> dstMvTrees;

    protected Set<ITree> srcDelTrees;

    protected Set<ITree> dstAddTrees;

    protected TreeContext src;

    protected TreeContext dst;

    protected MappingStore mappings;

    protected List<Action> actions;

    public TreeClassifier(TreeContext src, TreeContext dst, Set<Mapping> rawMappings, List<Action> actions) {
        this(src, dst, rawMappings);
        this.actions = actions;
        classify();
    }

    public TreeClassifier(TreeContext src, TreeContext dst, Matcher m) {
        this(src, dst, m.getMappingsAsSet());
        ActionGenerator g = new ActionGenerator(src.getRoot(), dst.getRoot(), m.getMappings());
        g.generate();
        this.actions = g.getActions();
        classify();
    }

    private TreeClassifier(TreeContext src, TreeContext dst, Set<Mapping> rawMappings) {
        this.src = src;
        this.dst = dst;
        this.mappings = new MappingStore(rawMappings);
        this.srcDelTrees = new HashSet<>();
        this.srcMvTrees = new HashSet<>();
        this.srcUpdTrees = new HashSet<>();
        this.dstMvTrees = new HashSet<>();
        this.dstAddTrees = new HashSet<>();
        this.dstUpdTrees = new HashSet<>();
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
