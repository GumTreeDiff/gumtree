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
 * Copyright 2016 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2016 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.tree.merge;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.utils.Couple;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.tree.TreeContext;

import java.util.*;

public class PcsMerge {

    TreeContext baseTree;
    TreeContext leftTree;
    TreeContext rightTree;

    Matcher leftMatch;
    Matcher rightMatch;

    public PcsMerge(TreeContext base, TreeContext left, TreeContext right, Matcher matchLeft, Matcher matchRight) {
        this.baseTree = base;
        this.leftTree = left;
        this.rightTree = right;
        this.leftMatch = matchLeft;
        this.rightMatch = matchRight;
    }

    public Set<Couple<Pcs, Pcs>> computeMerge() {
        final TreeContext fakeContext = new TreeContext().merge(baseTree).merge(leftTree).merge(rightTree);

        Set<Pcs> t0 = Pcs.fromTree(baseTree.getRoot());
        Set<Pcs> t1 = Pcs.fromTree(leftTree.getRoot());
        Set<Pcs> t2 = Pcs.fromTree(rightTree.getRoot());

        Map<ITree, ITree> references = buildReferenceTree();

        Set<Pcs> delta = new HashSet<>();
        Set<Pcs> t1Star = star(t1, references);
        Set<Pcs> t2Star = star(t2, references);
        delta.addAll(t0);
        delta.addAll(t1Star);
        delta.addAll(t2Star);
        HashSet<Pcs> deltaT1 = new HashSet<>(t1Star);
        deltaT1.removeAll(t0);
        HashSet<Pcs> deltaT2 = new HashSet<>(t2Star);
        deltaT2.removeAll(t0);
        return getInconsistencies(t0, delta);
    }

    Set<Pcs> star(Set<Pcs> pcses, Map<ITree, ITree> references) {
        Set<Pcs> result = new HashSet<>();
        for (Pcs pcs: pcses) {
            result.add(new Pcs(
                    references.get(pcs.getRoot()),
                    references.get(pcs.getPredecessor()),
                    references.get(pcs.getSuccessor())
            ));
        }
        return result;
    }

    Set<Couple<Pcs, Pcs>> getInconsistencies(Set<Pcs> base, Set<Pcs> all) {
        Set<Couple<Pcs, Pcs>> inconsistent = new HashSet<>();
        Set<Pcs> ignored = new HashSet<>();
        for (Pcs pcs: all) {
            if (ignored.contains(pcs))
                continue;
            Pcs other = null;
            other = pcs.getOtherRoot(all, ignored);
            if (other == null)
                other = pcs.getOtherPredecessor(all, ignored);
            if (other == null )
                other = pcs.getOtherSuccessor(all, ignored);
            if (other == null)
                continue;
            if (base.contains(pcs))
                ignored.add(pcs);
            else if (base.contains(other))
                ignored.add(other);
            else
                inconsistent.add(new Couple<>(pcs, other));
        }
        return inconsistent;
    }

    Map<ITree, ITree> buildReferenceTree() {
        Map<ITree, ITree> result = new HashMap<>();
        for (ITree t: baseTree.getRoot().preOrder())
            result.put(t, t);
        addReferenceTrees(leftTree.getRoot(), leftMatch.getMappings(), result);
        addReferenceTrees(rightTree.getRoot(), rightMatch.getMappings(), result);
        return result;
    }

    private void addReferenceTrees(ITree tree, MappingStore mappings, Map<ITree, ITree> result) {
        for (ITree t: tree.preOrder())
            if (mappings.hasDst(t)) {
                result.put(t, mappings.getSrc(t));
            } else {
                result.put(t, t);
            }
    }
}
