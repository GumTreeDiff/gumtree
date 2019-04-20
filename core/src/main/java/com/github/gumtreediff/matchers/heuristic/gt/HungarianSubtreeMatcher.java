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

import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.utils.HungarianAlgorithm;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.MultiMappingStore;
import com.github.gumtreediff.tree.ITree;

import java.util.*;

public class HungarianSubtreeMatcher extends AbstractSubtreeMatcher implements Matcher {
    @Override
    public MappingStore match(ITree src, ITree dst, MappingStore mappings) {
        HungarianSubtreeMatcher.Implementation impl =  new HungarianSubtreeMatcher.Implementation(src, dst, mappings);
        impl.match();
        return impl.mappings;
    }

    protected static class Implementation extends AbstractSubtreeMatcher.Implementation {
        public Implementation(ITree src, ITree dst, MappingStore mappings) {
            super(src, dst, mappings);
        }

        public void filterMappings(MultiMappingStore multiMappings) {
            List<MultiMappingStore> ambiguousList = new ArrayList<>();
            Set<ITree> ignored = new HashSet<>();
            for (ITree src : multiMappings.allMappedSrcs())
                if (multiMappings.isSrcUnique(src))
                    mappings.addMappingRecursively(src, multiMappings.getDsts(src).iterator().next());
                else if (!ignored.contains(src)) {
                    MultiMappingStore ambiguous = new MultiMappingStore();
                    Set<ITree> adsts = multiMappings.getDsts(src);
                    Set<ITree> asrcs = multiMappings.getSrcs(multiMappings.getDsts(src).iterator().next());
                    for (ITree asrc : asrcs)
                        for (ITree adst : adsts)
                            ambiguous.addMapping(asrc, adst);
                    ambiguousList.add(ambiguous);
                    ignored.addAll(asrcs);
                }

            Collections.sort(ambiguousList, new MultiMappingComparator());

            for (MultiMappingStore ambiguous : ambiguousList) {
                System.out.println("hungarian try.");
                List<ITree> lstSrcs = new ArrayList<>(ambiguous.allMappedSrcs());
                List<ITree> lstDsts = new ArrayList<>(ambiguous.allMappedDsts());
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

        private double cost(ITree src, ITree dst) {
            return 111D - sim(src, dst);
        }

        private static class MultiMappingComparator implements Comparator<MultiMappingStore> {

            @Override
            public int compare(MultiMappingStore m1, MultiMappingStore m2) {
                return Integer.compare(impact(m1), impact(m2));
            }

            public int impact(MultiMappingStore m) {
                int impact = 0;
                for (ITree src : m.allMappedSrcs()) {
                    int pSize = src.getParents().size();
                    if (pSize > impact) impact = pSize;
                }
                for (ITree src : m.allMappedDsts()) {
                    int pSize = src.getParents().size();
                    if (pSize > impact) impact = pSize;
                }
                return impact;
            }
        }
    }
}
