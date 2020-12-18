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

package com.github.gumtreediff.matchers.heuristic.gt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.MultiMappingStore;
import com.github.gumtreediff.matchers.SimilarityMetrics;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.HungarianAlgorithm;

public class HungarianSubtreeMatcher extends AbstractSubtreeMatcher {
    @Override
    public void filterMappings(MultiMappingStore multiMappings) {
        List<MultiMappingStore> ambiguousList = new ArrayList<>();
        Set<Tree> ignored = new HashSet<>();
        for (Tree src : multiMappings.allMappedSrcs())
            if (multiMappings.isSrcUnique(src))
                mappings.addMappingRecursively(src, multiMappings.getDsts(src).iterator().next());
            else if (!ignored.contains(src)) {
                MultiMappingStore ambiguous = new MultiMappingStore();
                Set<Tree> adsts = multiMappings.getDsts(src);
                Set<Tree> asrcs = multiMappings.getSrcs(multiMappings.getDsts(src).iterator().next());
                for (Tree asrc : asrcs)
                    for (Tree adst : adsts)
                        ambiguous.addMapping(asrc, adst);
                ambiguousList.add(ambiguous);
                ignored.addAll(asrcs);
            }

        Collections.sort(ambiguousList, new MultiMappingComparator());

        for (MultiMappingStore ambiguous : ambiguousList) {
            List<Tree> lstSrcs = new ArrayList<>(ambiguous.allMappedSrcs());
            List<Tree> lstDsts = new ArrayList<>(ambiguous.allMappedDsts());
            double[][] matrix = new double[lstSrcs.size()][lstDsts.size()];
            for (int i = 0; i < lstSrcs.size(); i++)
                for (int j = 0; j < lstDsts.size(); j++)
                    matrix[i][j] = cost(lstSrcs.get(i), lstDsts.get(j));

            HungarianAlgorithm hgAlg = new HungarianAlgorithm(matrix);
            int[] solutions = hgAlg.execute();
            for (int i = 0; i < solutions.length; i++) {
                int dstIdx = solutions[i];
                if (dstIdx != -1)
                    mappings.addMappingRecursively(lstSrcs.get(i), lstDsts.get(dstIdx));
            }
        }
    }

    private double cost(Tree src, Tree dst) {
        return 111D - sim(src, dst);
    }

    protected double sim(Tree src, Tree dst) {
        var jaccard = SimilarityMetrics.jaccardSimilarity(src.getParent(), dst.getParent(), mappings);
        int posSrc = (src.isRoot()) ? 0 : src.getParent().getChildPosition(src);
        int posDst = (dst.isRoot()) ? 0 : dst.getParent().getChildPosition(dst);
        int maxSrcPos = (src.isRoot()) ? 1 : src.getParent().getChildren().size();
        int maxDstPos = (dst.isRoot()) ? 1 : dst.getParent().getChildren().size();
        int maxPosDiff = Math.max(maxSrcPos, maxDstPos);
        double pos = 1D - ((double) Math.abs(posSrc - posDst) / (double) maxPosDiff);
        double po = 1D - ((double) Math.abs(src.getMetrics().position - dst.getMetrics().position)
                / (double) this.getMaxTreeSize());
        return 100 * jaccard + 10 * pos + po;
    }

    private static class MultiMappingComparator implements Comparator<MultiMappingStore> {
        @Override
        public int compare(MultiMappingStore m1, MultiMappingStore m2) {
            return Integer.compare(impact(m1), impact(m2));
        }

        public int impact(MultiMappingStore m) {
            int impact = 0;
            for (Tree src : m.allMappedSrcs()) {
                int pSize = src.getParents().size();
                if (pSize > impact)
                    impact = pSize;
            }

            for (Tree src : m.allMappedDsts()) {
                int pSize = src.getParents().size();
                if (pSize > impact)
                    impact = pSize;
            }

            return impact;
        }
    }
}
