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

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.TreeContext;

public class Diff {
    public final TreeContext src;

    public final TreeContext dst;

    public final MappingStore mappings;

    public final EditScript editScript;

    public Diff(TreeContext src, TreeContext dst, MappingStore mappings, EditScript editScript) {
        this.src = src;
        this.dst = dst;
        this.mappings = mappings;
        this.editScript = editScript;
    }

    public ITreeClassifier createAllNodeClassifier() {
        return new AllNodesClassifier(this);
    }

    public ITreeClassifier createRootNodesClassifier() {
        return new OnlyRootsClassifier(this);
    }
}
