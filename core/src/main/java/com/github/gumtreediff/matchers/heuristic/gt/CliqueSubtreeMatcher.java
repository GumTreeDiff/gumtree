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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MultiMappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class CliqueSubtreeMatcher extends AbstractSubtreeMatcher {

    @Override
    public void filterMappings(MultiMappingStore multiMappings) {
        Int2ObjectOpenHashMap<Pair<List<Tree>, List<Tree>>> cliques = new Int2ObjectOpenHashMap<>();
        for (Mapping m : multiMappings) {
            int hash = m.first.getMetrics().hash;
            if (!cliques.containsKey(hash))
                cliques.put(hash, new Pair<>(new ArrayList<>(), new ArrayList<>()));
            cliques.get(hash).first.add(m.first);
            cliques.get(hash).second.add(m.second);
        }

        List<Pair<List<Tree>, List<Tree>>> ccliques = new ArrayList<>();

        for (int hash : cliques.keySet()) {
            Pair<List<Tree>, List<Tree>> clique = cliques.get(hash);
            if (clique.first.size() == 1 && clique.second.size() == 1) {
                mappings.addMappingRecursively(clique.first.get(0), clique.second.get(0));
            } else
                ccliques.add(clique);
        }

        Collections.sort(ccliques, new CliqueComparator());

        for (Pair<List<Tree>, List<Tree>> clique : ccliques) {
            List<Mapping> cliqueAsMappings = fromClique(clique);
            // FIXME use FullMappingComparator
            Collections.sort(cliqueAsMappings, new MappingComparator(cliqueAsMappings));
            Set<Tree> srcIgnored = new HashSet<>();
            Set<Tree> dstIgnored = new HashSet<>();
            retainBestMapping(cliqueAsMappings, srcIgnored, dstIgnored);
        }
    }

    private List<Mapping> fromClique(Pair<List<Tree>, List<Tree>> clique) {
        List<Mapping> cliqueAsMappings = new ArrayList<Mapping>();
        for (Tree src : clique.first)
            for (Tree dst : clique.first)
                cliqueAsMappings.add(new Mapping(src, dst));
        return cliqueAsMappings;
    }

    private class CliqueComparator implements Comparator<Pair<List<Tree>, List<Tree>>> {

        @Override
        public int compare(Pair<List<Tree>, List<Tree>> l1, Pair<List<Tree>, List<Tree>> l2) {
            int minDepth1 = minDepth(l1);
            int minDepth2 = minDepth(l2);
            if (minDepth1 != minDepth2)
                return -1 * Integer.compare(minDepth1, minDepth2);
            else {
                int size1 = size(l1);
                int size2 = size(l2);
                return -1 * Integer.compare(size1, size2);
            }
        }

        private int minDepth(Pair<List<Tree>, List<Tree>> trees) {
            int depth = Integer.MAX_VALUE;
            for (Tree t : trees.first)
                if (depth > t.getMetrics().depth)
                    depth = t.getMetrics().depth;
            for (Tree t : trees.second)
                if (depth > t.getMetrics().depth)
                    depth = t.getMetrics().depth;
            return depth;
        }

        private int size(Pair<List<Tree>, List<Tree>> trees) {
            return trees.first.size() + trees.second.size();
        }

    }

    private class MappingComparator implements Comparator<Mapping> {

        private Map<Mapping, double[]> simMap = new HashMap<>();

        public MappingComparator(List<Mapping> mappings) {
            for (Mapping mapping : mappings)
                simMap.put(mapping, sims(mapping.first, mapping.second));
        }

        @Override
        public int compare(Mapping m1, Mapping m2) {
            double[] sims1 = simMap.get(m1);
            double[] sims2 = simMap.get(m2);
            for (int i = 0; i < sims1.length; i++) {
                if (sims1[i] != sims2[i])
                    return -1 * Double.compare(sims1[i], sims2[i]); // FIXME: ensure the order is correct
            }
            return 0;
        }

        private Map<Tree, List<Tree>> srcDescendants = new HashMap<>();

        private Map<Tree, Set<Tree>> dstDescendants = new HashMap<>();

        protected int numberOfCommonDescendants(Tree src, Tree dst) {
            if (!srcDescendants.containsKey(src))
                srcDescendants.put(src, src.getDescendants());
            if (!dstDescendants.containsKey(dst))
                dstDescendants.put(dst, new HashSet<>(dst.getDescendants()));

            int common = 0;

            for (Tree t : srcDescendants.get(src)) {
                Tree m = mappings.getDstForSrc(t);
                if (m != null && dstDescendants.get(dst).contains(m))
                    common++;
            }

            return common;
        }

        protected double[] sims(Tree src, Tree dst) {
            double[] sims = new double[4];
            sims[0] = jaccardSimilarity(src.getParent(), dst.getParent());
            sims[1] = src.positionInParent() - dst.positionInParent();
            sims[2] = src.getMetrics().position - dst.getMetrics().position;
            sims[3] = src.getMetrics().position;
            return sims;
        }

        protected double jaccardSimilarity(Tree src, Tree dst) {
            double num = (double) numberOfCommonDescendants(src, dst);
            double den = (double) srcDescendants.get(src).size() + (double) dstDescendants.get(dst).size() - num;
            return num / den;
        }
    }

}
