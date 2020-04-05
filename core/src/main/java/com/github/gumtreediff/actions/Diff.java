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

import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.TreeContext;

import java.io.IOException;

public class Diff {
    public final TreeContext src;
    public final TreeContext dst;
    public final MappingStore mappings;
    public final EditScript editScript;

    public Diff(TreeContext src, TreeContext dst,
                MappingStore mappings, EditScript editScript) {
        this.src = src;
        this.dst = dst;
        this.mappings = mappings;
        this.editScript = editScript;
    }

    public static Diff compute(String srcFile, String dstFile,
                               String treeGenerator, String matcher) throws IOException {
        TreeContext src = TreeGenerators.getInstance().getTree(srcFile, treeGenerator);
        TreeContext dst = TreeGenerators.getInstance().getTree(dstFile, treeGenerator);
        MappingStore mappings = Matchers.getInstance()
                .getMatcherWithFallback(matcher).match(src.getRoot(), dst.getRoot());
        EditScript editScript = new SimplifiedChawatheScriptGenerator().computeActions(mappings);
        return new Diff(src, dst, mappings, editScript);
    }

    public static Diff compute(String srcFile, String dstFile) throws IOException {
        return compute(srcFile, dstFile, null, null);
    }

    public ITreeClassifier createAllNodeClassifier() {
        return new AllNodesClassifier(this);
    }

    public ITreeClassifier createRootNodesClassifier() {
        return new OnlyRootsClassifier(this);
    }
}
