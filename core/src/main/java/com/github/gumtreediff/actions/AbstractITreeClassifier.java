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
 * Copyright 2019 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.actions;

import com.github.gumtreediff.tree.Tree;

import java.util.HashSet;

import java.util.Set;

public abstract class AbstractITreeClassifier implements TreeClassifier {
    protected final Diff diff;

    protected final Set<Tree> srcUpdTrees = new HashSet<>();

    protected final Set<Tree> dstUpdTrees = new HashSet<>();

    protected final Set<Tree> srcMvTrees = new HashSet<>();

    protected final Set<Tree> dstMvTrees = new HashSet<>();

    protected final Set<Tree> srcDelTrees = new HashSet<>();

    protected final Set<Tree> dstAddTrees = new HashSet<>();

    public AbstractITreeClassifier(Diff diff) {
        this.diff = diff;
        classify();
    }

    protected abstract void classify();

    public Set<Tree> getUpdatedSrcs() {
        return srcUpdTrees;
    }

    public Set<Tree> getUpdatedDsts() {
        return dstUpdTrees;
    }

    public Set<Tree> getMovedSrcs() {
        return srcMvTrees;
    }

    public Set<Tree> getMovedDsts() {
        return dstMvTrees;
    }

    public Set<Tree> getDeletedSrcs() {
        return srcDelTrees;
    }

    public Set<Tree> getInsertedDsts() {
        return dstAddTrees;
    }
}
