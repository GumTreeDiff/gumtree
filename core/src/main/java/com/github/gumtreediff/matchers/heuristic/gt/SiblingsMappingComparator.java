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
 */

package com.github.gumtreediff.matchers.heuristic.gt;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;

import java.util.*;

public final class SiblingsMappingComparator extends AbstractMappingComparator {

    private Map<ITree, List<ITree>> srcDescendants = new HashMap<>();

    private Map<ITree, Set<ITree>> dstDescendants = new HashMap<>();

    public SiblingsMappingComparator(List<Mapping> ambiguousMappings, MappingStore mappings, int maxTreeSize) {
        super(ambiguousMappings, mappings, maxTreeSize);
        for (Mapping ambiguousMapping: ambiguousMappings)
            similarities.put(ambiguousMapping, similarity(ambiguousMapping.getFirst(), ambiguousMapping.getSecond()));
    }

    protected double similarity(ITree src, ITree dst) {
        return 100D * siblingsJaccardSimilarity(src.getParent(), dst.getParent())
                +  10D * posInParentSimilarity(src, dst) + numberingSimilarity(src , dst);
    }

    protected double siblingsJaccardSimilarity(ITree src, ITree dst) {
        double num = (double) numberOfCommonDescendants(src, dst);
        double den = (double) srcDescendants.get(src).size() + (double) dstDescendants.get(dst).size() - num;
        return num / den;
    }

    protected int numberOfCommonDescendants(ITree src, ITree dst) {
        if (!srcDescendants.containsKey(src))
            srcDescendants.put(src, src.getDescendants());
        if (!dstDescendants.containsKey(dst))
            dstDescendants.put(dst, new HashSet<>(dst.getDescendants()));

        int common = 0;

        for (ITree t: srcDescendants.get(src)) {
            ITree m = mappings.getDst(t);
            if (m != null && dstDescendants.get(dst).contains(m))
                common++;
        }

        return common;
    }

}