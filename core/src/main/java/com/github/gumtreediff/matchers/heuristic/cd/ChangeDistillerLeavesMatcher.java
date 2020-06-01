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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.simmetrics.StringMetrics;

import com.github.gumtreediff.matchers.Configurable;
import com.github.gumtreediff.matchers.ConfigurationOptions;
import com.github.gumtreediff.matchers.GumTreeProperties;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;
import com.google.common.collect.Sets;

public class ChangeDistillerLeavesMatcher implements Matcher, Configurable {

    private static final double DEFAULT_LABEL_SIM_THRESHOLD = 0.5;

    protected double label_sim_threshold = DEFAULT_LABEL_SIM_THRESHOLD;

    public ChangeDistillerLeavesMatcher() {

    }

    @Override
    public void configure(GumTreeProperties properties) {
        label_sim_threshold = properties.tryConfigure(ConfigurationOptions.GT_CD_LSIM, label_sim_threshold);

    }

    @Override
    public MappingStore match(ITree src, ITree dst, MappingStore mappings) {

        List<Mapping> leavesMappings = new ArrayList<>();
        List<ITree> dstLeaves = retainLeaves(TreeUtils.postOrder(dst));
        for (Iterator<ITree> srcLeaves = TreeUtils.leafIterator(TreeUtils.postOrderIterator(src)); srcLeaves
                .hasNext(); ) {
            ITree srcLeaf = srcLeaves.next();
            for (ITree dstLeaf : dstLeaves) {
                if (mappings.isMappingAllowed(srcLeaf, dstLeaf)) {
                    double sim = StringMetrics.qGramsDistance().compare(srcLeaf.getLabel(), dstLeaf.getLabel());
                    if (sim > label_sim_threshold)
                        leavesMappings.add(new Mapping(srcLeaf, dstLeaf));
                }
            }
        }

        Set<ITree> ignoredSrcTrees = new HashSet<>();
        Set<ITree> ignoredDstTrees = new HashSet<>();
        Collections.sort(leavesMappings, new LeafMappingComparator());
        while (leavesMappings.size() > 0) {
            Mapping bestMapping = leavesMappings.remove(0);
            if (!(ignoredSrcTrees.contains(bestMapping.first) || ignoredDstTrees.contains(bestMapping.second))) {
                mappings.addMapping(bestMapping.first, bestMapping.second);
                ignoredSrcTrees.add(bestMapping.first);
                ignoredDstTrees.add(bestMapping.second);
            }
        }
        return mappings;
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

    private static class LeafMappingComparator implements Comparator<Mapping> {

        @Override
        public int compare(Mapping m1, Mapping m2) {
            return Double.compare(sim(m1), sim(m2));
        }

        public double sim(Mapping m) {
            return StringMetrics.qGramsDistance().compare(m.first.getLabel(), m.second.getLabel());
        }
    }

    public double getLabel_sim_threshold() {
        return label_sim_threshold;
    }

    public void setLabel_sim_threshold(double labelSimThreshold) {
        this.label_sim_threshold = labelSimThreshold;
    }

    @Override
    public Set<ConfigurationOptions> getApplicableOptions() {

        return Sets.newHashSet(ConfigurationOptions.GT_CD_LSIM);
    }
}
