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
import com.github.gumtreediff.tree.Tree;

import java.util.*;

public final class SiblingsMappingComparator extends AbstractMappingComparator {

    private Map<Tree, List<Tree>> srcDescendants = new HashMap<>();

    private Map<Tree, Set<Tree>> dstDescendants = new HashMap<>();

    public SiblingsMappingComparator(List<Mapping> ambiguousMappings, MappingStore mappings,
                                     int maxTreeSize) {
        super(ambiguousMappings, mappings, maxTreeSize);
        for (Mapping ambiguousMapping: ambiguousMappings)
            similarities.put(ambiguousMapping, similarity(ambiguousMapping.first, ambiguousMapping.second));
    }

    @Override
    protected double similarity(Tree src, Tree dst) {
        return 100D * siblingsJaccardSimilarity(src.getParent(), dst.getParent())
                +  10D * posInParentSimilarity(src, dst) + numberingSimilarity(src , dst);
    }

    protected double siblingsJaccardSimilarity(Tree src, Tree dst) {
        double num = (double) numberOfCommonDescendants(src, dst);
        double den = (double) srcDescendants.get(src).size() + (double) dstDescendants.get(dst).size() - num;
        return num / den;
    }

    protected int numberOfCommonDescendants(Tree src, Tree dst) {
        if (!srcDescendants.containsKey(src))
            srcDescendants.put(src, src.getDescendants());
        if (!dstDescendants.containsKey(dst))
            dstDescendants.put(dst, new HashSet<>(dst.getDescendants()));

        int common = 0;

        for (Tree t: srcDescendants.get(src)) {
            Tree m = mappings.getDstForSrc(t);
            if (m != null && dstDescendants.get(dst).contains(m))
                common++;
        }

        return common;
    }

}