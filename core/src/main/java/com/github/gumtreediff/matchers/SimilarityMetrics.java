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

import com.github.gumtreediff.tree.Tree;

import java.util.HashSet;
import java.util.Set;

public class SimilarityMetrics {
    private SimilarityMetrics() {}

    public static double chawatheSimilarity(Tree src, Tree dst, MappingStore mappings) {
        int max = Math.max(src.getDescendants().size(), dst.getDescendants().size());
        return (double) numberOfMappedDescendants(src, dst, mappings) / (double) max;
    }

    public static double overlapSimilarity(Tree src, Tree dst, MappingStore mappings) {
        int min = Math.min(src.getDescendants().size(), dst.getDescendants().size());
        return (double) numberOfMappedDescendants(src, dst, mappings) / (double) min;
    }

    public static double diceSimilarity(Tree src, Tree dst, MappingStore mappings) {
        return diceCoefficient(numberOfMappedDescendants(src, dst, mappings),
                src.getDescendants().size(), dst.getDescendants().size());
    }

    public static double jaccardSimilarity(Tree src, Tree dst, MappingStore mappings) {
        return jaccardIndex(numberOfMappedDescendants(src, dst, mappings),
                src.getDescendants().size(), dst.getDescendants().size());
    }

    public static double diceCoefficient(int commonElementsNb, int leftElementsNb, int rightElementsNb) {
        return 2D * commonElementsNb / (leftElementsNb + rightElementsNb);
    }

    public static double jaccardIndex(int commonElementsNb, int leftElementsNb, int rightElementsNb) {
        double denominator = (leftElementsNb + rightElementsNb - commonElementsNb);
        double res = commonElementsNb / denominator;
        return res;
    }

    private static int numberOfMappedDescendants(Tree src, Tree dst, MappingStore mappings) {
        Set<Tree> dstDescendants = new HashSet<>(dst.getDescendants());
        int mappedDescendants = 0;

        for (var srcDescendant : src.getDescendants()) {
            if (mappings.isSrcMapped(srcDescendant)) {
                var dstForSrcDescendant = mappings.getDstForSrc(srcDescendant);
                if (dstDescendants.contains(dstForSrcDescendant))
                    mappedDescendants++;
            }
        }

        return mappedDescendants;
    }
}
