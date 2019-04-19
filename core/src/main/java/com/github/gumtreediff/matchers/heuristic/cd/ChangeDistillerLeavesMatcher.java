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

package com.github.gumtreediff.matchers.heuristic.cd;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;
import org.simmetrics.StringMetrics;

import java.util.*;

public class ChangeDistillerLeavesMatcher implements Matcher {

    public static final double LABEL_SIM_THRESHOLD = Double.parseDouble(System.getProperty("gt.cd.lsim", "0.5"));

    @Override
    public MappingStore match(ITree src, ITree dst, MappingStore mappings) {
        Implementation impl = new Implementation(src, dst, mappings);
        impl.match();
        return impl.mappings;
    }

    private static class Implementation {
        private final ITree src;
        private final ITree dst;
        private final MappingStore mappings;

        public Implementation(ITree src, ITree dst, MappingStore mappings) {
            this.src = src;
            this.dst = dst;
            this.mappings = mappings;
        }

        public void match() {
            List<Mapping> leavesMappings = new ArrayList<>();
            List<ITree> dstLeaves = retainLeaves(TreeUtils.postOrder(dst));
            for (Iterator<ITree> srcLeaves = TreeUtils.leafIterator(
                    TreeUtils.postOrderIterator(src)); srcLeaves.hasNext(); ) {
                ITree srcLeaf = srcLeaves.next();
                for (ITree dstLeaf : dstLeaves) {
                    if (mappings.isMappingAllowed(srcLeaf, dstLeaf)) {
                        double sim = StringMetrics.qGramsDistance().compare(srcLeaf.getLabel(), dstLeaf.getLabel());
                        if (sim > LABEL_SIM_THRESHOLD)
                            leavesMappings.add(new Mapping(srcLeaf, dstLeaf));
                    }
                }
            }

            Set<ITree> ignoredSrcTrees = new HashSet<>();
            Set<ITree> ignoredDstTrees = new HashSet<>();
            Collections.sort(leavesMappings, new LeafMappingComparator());
            while (leavesMappings.size() > 0) {
                Mapping bestMapping = leavesMappings.remove(0);
                if (!(ignoredSrcTrees.contains(bestMapping.first)
                        || ignoredDstTrees.contains(bestMapping.second))) {
                    mappings.addMapping(bestMapping.first, bestMapping.second);
                    ignoredSrcTrees.add(bestMapping.first);
                    ignoredDstTrees.add(bestMapping.second);
                }
            }
        }

        public List<ITree> retainLeaves(List<ITree> trees) {
            Iterator<ITree> treeIterator = trees.iterator();
            while (treeIterator.hasNext()) {
                ITree tree = treeIterator.next();
                if (!tree.isLeaf())
                    treeIterator.remove();
            }
            return trees;
        }
    }

    private static class LeafMappingComparator implements Comparator<Mapping> {

        @Override
        public int compare(Mapping m1, Mapping m2) {
            return Double.compare(sim(m1), sim(m2));
        }

        public double sim(Mapping m) {
            return StringMetrics.qGramsDistance().compare(m.first.getLabel(), m.second.getLabel());
        }

    }
}
