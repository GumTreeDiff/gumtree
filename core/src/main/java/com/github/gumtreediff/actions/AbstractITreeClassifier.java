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

import com.github.gumtreediff.tree.ITree;

import java.util.HashSet;

import java.util.Set;

public abstract class AbstractITreeClassifier implements ITreeClassifier {
    protected final Diff diff;

    protected final Set<ITree> srcUpdTrees = new HashSet<>();

    protected final Set<ITree> dstUpdTrees = new HashSet<>();

    protected final Set<ITree> srcMvTrees = new HashSet<>();

    protected final Set<ITree> dstMvTrees = new HashSet<>();

    protected final Set<ITree> srcDelTrees = new HashSet<>();

    protected final Set<ITree> dstAddTrees = new HashSet<>();

    public AbstractITreeClassifier(Diff diff) {
        this.diff = diff;
        classify();
    }

    protected abstract void classify();

    public Set<ITree> getUpdatedSrcs() {
        return srcUpdTrees;
    }

    public Set<ITree> getUpdatedDsts() {
        return dstUpdTrees;
    }

    public Set<ITree> getMovedSrcs() {
        return srcMvTrees;
    }

    public Set<ITree> getMovedDsts() {
        return dstMvTrees;
    }

    public Set<ITree> getDeletedSrcs() {
        return srcDelTrees;
    }

    public Set<ITree> getInsertedDsts() {
        return dstAddTrees;
    }
}
