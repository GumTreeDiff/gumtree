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
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.utils.Pair;

import gnu.trove.map.hash.TIntObjectHashMap;

public class CliqueSubtreeMatcher extends AbstractSubtreeMatcher {

    @Override
    public void filterMappings(MultiMappingStore multiMappings) {
        TIntObjectHashMap<Pair<List<ITree>, List<ITree>>> cliques = new TIntObjectHashMap<>();
        for (Mapping m : multiMappings) {
            int hash = m.first.getMetrics().hash;
            if (!cliques.containsKey(hash))
                cliques.put(hash, new Pair<>(new ArrayList<>(), new ArrayList<>()));
            cliques.get(hash).first.add(m.first);
            cliques.get(hash).second.add(m.second);
        }

        List<Pair<List<ITree>, List<ITree>>> ccliques = new ArrayList<>();

        for (int hash : cliques.keys()) {
            Pair<List<ITree>, List<ITree>> clique = cliques.get(hash);
            if (clique.first.size() == 1 && clique.second.size() == 1) {
                mappings.addMappingRecursively(clique.first.get(0), clique.second.get(0));
                cliques.remove(hash);
            } else
                ccliques.add(clique);
        }

        Collections.sort(ccliques, new CliqueComparator());

        for (Pair<List<ITree>, List<ITree>> clique : ccliques) {
            List<Mapping> cliqueAsMappings = fromClique(clique);
            Collections.sort(cliqueAsMappings, new MappingComparator(cliqueAsMappings));
            Set<ITree> srcIgnored = new HashSet<>();
            Set<ITree> dstIgnored = new HashSet<>();
            retainBestMapping(cliqueAsMappings, srcIgnored, dstIgnored);
        }
    }

    private List<Mapping> fromClique(Pair<List<ITree>, List<ITree>> clique) {
        List<Mapping> cliqueAsMappings = new ArrayList<Mapping>();
        for (ITree src : clique.first)
            for (ITree dst : clique.first)
                cliqueAsMappings.add(new Mapping(src, dst));
        return cliqueAsMappings;
    }

    private class CliqueComparator implements Comparator<Pair<List<ITree>, List<ITree>>> {

        @Override
        public int compare(Pair<List<ITree>, List<ITree>> l1, Pair<List<ITree>, List<ITree>> l2) {
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

        private int minDepth(Pair<List<ITree>, List<ITree>> trees) {
            int depth = Integer.MAX_VALUE;
            for (ITree t : trees.first)
                if (depth > t.getMetrics().depth)
                    depth = t.getMetrics().depth;
            for (ITree t : trees.second)
                if (depth > t.getMetrics().depth)
                    depth = t.getMetrics().depth;
            return depth;
        }

        private int size(Pair<List<ITree>, List<ITree>> trees) {
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
                    return -1 * Double.compare(sims2[i], sims2[i]);
            }
            return 0;
        }

        private Map<ITree, List<ITree>> srcDescendants = new HashMap<>();

        private Map<ITree, Set<ITree>> dstDescendants = new HashMap<>();

        protected int numberOfCommonDescendants(ITree src, ITree dst) {
            if (!srcDescendants.containsKey(src))
                srcDescendants.put(src, src.getDescendants());
            if (!dstDescendants.containsKey(dst))
                dstDescendants.put(dst, new HashSet<>(dst.getDescendants()));

            int common = 0;

            for (ITree t : srcDescendants.get(src)) {
                ITree m = mappings.getDstForSrc(t);
                if (m != null && dstDescendants.get(dst).contains(m))
                    common++;
            }

            return common;
        }

        protected double[] sims(ITree src, ITree dst) {
            double[] sims = new double[4];
            sims[0] = jaccardSimilarity(src.getParent(), dst.getParent());
            sims[1] = src.positionInParent() - dst.positionInParent();
            sims[2] = src.getMetrics().position - dst.getMetrics().position;
            sims[3] = src.getMetrics().position;
            return sims;
        }

        protected double jaccardSimilarity(ITree src, ITree dst) {
            double num = (double) numberOfCommonDescendants(src, dst);
            double den = (double) srcDescendants.get(src).size() + (double) dstDescendants.get(dst).size() - num;
            return num / den;
        }
    }

}
