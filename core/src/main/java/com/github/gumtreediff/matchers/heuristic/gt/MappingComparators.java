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
 * Copyright 2020 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.matchers.heuristic.gt;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.SimilarityMetrics;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.SequenceAlgorithms;

import java.util.*;

public class MappingComparators {
    public static class FullMappingComparator implements Comparator<Mapping> {
        private SiblingsSimilarityMappingComparator siblingsComparator;
        private ParentsSimilarityMappingComparator parentsComparator;
        private PositionInParentSimilarityMappingComparator parentsPositionComparator;
        private AbsolutePositionDistanceMappingComparator positionComparator;

        public FullMappingComparator(MappingStore ms) {
            siblingsComparator = new SiblingsSimilarityMappingComparator(ms);
            parentsComparator = new ParentsSimilarityMappingComparator();
            parentsPositionComparator = new PositionInParentSimilarityMappingComparator();
            positionComparator = new AbsolutePositionDistanceMappingComparator();
        }

        @Override
        public int compare(Mapping m1, Mapping m2) {
            //System.out.println("compare with siblings.");
            int result = siblingsComparator.compare(m1, m2);
            if (result != 0)
                return result;

            //System.out.println("compare with parents.");
            result = parentsComparator.compare(m1, m2);
            if (result != 0)
                return result;

            //System.out.println("compare with relative pos.");
            result = parentsPositionComparator.compare(m1, m2);
            if (result != 0)
                return result;

            //System.out.println("compare with absolute pos.");
            return positionComparator.compare(m1, m2);
        }
    }

    public static class SiblingsSimilarityMappingComparator implements Comparator<Mapping> {
        private MappingStore ms;

        private Map<Tree, Set<Tree>> srcDescendants = new HashMap<>();
        private Map<Tree, Set<Tree>> dstDescendants = new HashMap<>();
        private Map<Mapping, Double> cachedSimilarities = new HashMap<>();

        public SiblingsSimilarityMappingComparator(MappingStore ms) {
            this.ms = ms;
        }

        @Override
        public int compare(Mapping m1, Mapping m2) {
            if (m1.first.getParent() == m2.first.getParent()
                    && m1.second.getParent() == m2.second.getParent())
                return 0;

            if (!cachedSimilarities.containsKey(m1))
                cachedSimilarities.put(m1, SimilarityMetrics.diceCoefficient(commonDescendantsNb(m1.first.getParent(),
                        m1.second.getParent()),
                        srcDescendants.get(m1.first.getParent()).size(),
                        dstDescendants.get(m1.second.getParent()).size()));

            if (!cachedSimilarities.containsKey(m2))
                cachedSimilarities.put(m2, SimilarityMetrics.diceCoefficient(commonDescendantsNb(m2.first.getParent(),
                        m2.second.getParent()),
                        srcDescendants.get(m2.first.getParent()).size(),
                        dstDescendants.get(m2.second.getParent()).size()));

            return Double.compare(cachedSimilarities.get(m2), cachedSimilarities.get(m1));
        }

        private int commonDescendantsNb(Tree src, Tree dst) {
            if (!srcDescendants.containsKey(src))
                srcDescendants.put(src, new HashSet<>(src.getDescendants()));
            if (!dstDescendants.containsKey(dst))
                dstDescendants.put(dst, new HashSet<>(dst.getDescendants()));

            int common = 0;

            for (Tree t: srcDescendants.get(src)) {
                Tree m = ms.getDstForSrc(t);
                if (m != null && dstDescendants.get(dst).contains(m))
                    common++;
            }

            return common;
        }
    }

    public static class ParentsSimilarityMappingComparator implements Comparator<Mapping> {
        private Map<Mapping, Double> cachedSimilarities = new HashMap<>();

        @Override
        public int compare(Mapping m1, Mapping m2) {
            if (m1.first.getParent() == m2.first.getParent()
                    && m1.second.getParent() == m2.second.getParent())
                return 0;

            if (!cachedSimilarities.containsKey(m1)) {
                int commonParentsNbInM1 = SequenceAlgorithms.longestCommonSubsequenceWithTypeAndLabel(
                        m1.first.getParents(), m1.second.getParents()).size();
                double m1Sim = SimilarityMetrics.diceCoefficient(commonParentsNbInM1, m1.first.getParents().size(),
                        m1.second.getParents().size());
                cachedSimilarities.put(m1, m1Sim);
            }

            if (!cachedSimilarities.containsKey(m2)) {
                int commonParentsNbInM2 = SequenceAlgorithms.longestCommonSubsequenceWithTypeAndLabel(
                        m2.first.getParents(), m2.second.getParents()).size();
                double m2Sim = SimilarityMetrics.diceCoefficient(commonParentsNbInM2, m2.first.getParents().size(),
                        m2.second.getParents().size());
                cachedSimilarities.put(m2, m2Sim);
            }

            return Double.compare(cachedSimilarities.get(m2), cachedSimilarities.get(m1));
        }
    }

    public static class PositionInParentSimilarityMappingComparator implements Comparator<Mapping> {
        @Override
        public int compare(Mapping m1, Mapping m2) {
            double m1PosSim = positionInParentSimilarity(m1.first, m1.second);
            double m2PosSim = positionInParentSimilarity(m2.first, m2.second);
            return Double.compare(m2PosSim, m1PosSim);
        }

        private double positionInParentSimilarity(Tree src, Tree dst) {
            double posSrc = (src.isRoot()) ? 1D :
                    src.getParent().getChildPosition(src) + 1 / (double) src.getParent().getChildren().size();
            double posDst = (dst.isRoot()) ? 1D :
                    dst.getParent().getChildPosition(dst) + 1 / (double) dst.getParent().getChildren().size();

            return 1D - Math.abs(posSrc - posDst);
        }
    }

    public static class AbsolutePositionDistanceMappingComparator implements Comparator<Mapping> {
        @Override
        public int compare(Mapping m1, Mapping m2) {
            int m1PosDist = absolutePositionDistance(m1.first, m1.second);
            int m2PosDist = absolutePositionDistance(m2.first, m2.second);
            return Integer.compare(m1PosDist, m2PosDist);
        }

        private int absolutePositionDistance(Tree src, Tree dst) {
            return Math.abs(src.getMetrics().position - dst.getMetrics().position);
        }
    }
}
