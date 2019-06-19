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

package com.github.gumtreediff.matchers;

import com.github.gumtreediff.tree.ITree;

import java.util.HashSet;
import java.util.Set;

public class SimilarityMetrics {
    private SimilarityMetrics() {}

    public static double chawatheSimilarity(ITree src, ITree dst, MappingStore mappings) {
        int max = Math.max(src.getDescendants().size(), dst.getDescendants().size());
        return (double) numberOfCommonDescendants(src, dst, mappings) / (double) max;
    }

    public static double overlapSimilarity(ITree src, ITree dst, MappingStore mappings) {
        int min = Math.min(src.getDescendants().size(), dst.getDescendants().size());
        return (double) numberOfCommonDescendants(src, dst, mappings) / (double) min;
    }

    public static double diceSimilarity(ITree src, ITree dst, MappingStore mappings) {
        double commonDescendants = (double) numberOfCommonDescendants(src, dst, mappings);
        return (2D * commonDescendants)
                / ((double) src.getDescendants().size() + (double) dst.getDescendants().size());
    }

    public static double jaccardSimilarity(ITree src, ITree dst, MappingStore mappings) {
        double num = (double) numberOfCommonDescendants(src, dst, mappings);
        double den = (double) src.getDescendants().size() + (double) dst.getDescendants().size() - num;
        return num / den;
    }

    private static int numberOfCommonDescendants(ITree src, ITree dst, MappingStore mappings) {
        Set<ITree> dstDescendants = new HashSet<>(dst.getDescendants());
        int common = 0;

        for (ITree t : src.getDescendants()) {
            if (mappings.isSrcMapped(t)) {
                ITree m = mappings.getDstForSrc(t);
                if (dstDescendants.contains(m))
                    common++;
            }
        }

        return common;
    }
}
