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

import com.github.gumtreediff.matchers.ConfigurableMatcher;
import com.github.gumtreediff.matchers.ConfigurationOptions;
import com.github.gumtreediff.matchers.GumtreeProperties;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;
import com.google.common.collect.Sets;

public class ChangeDistillerLeavesMatcher implements ConfigurableMatcher {
    private static final double DEFAULT_LABEL_SIM_THRESHOLD = 0.5;

    protected double labelSimThreshold = DEFAULT_LABEL_SIM_THRESHOLD;

    public ChangeDistillerLeavesMatcher() {

    }

    @Override
    public void configure(GumtreeProperties properties) {
        labelSimThreshold = properties.tryConfigure(ConfigurationOptions.cd_labsim, labelSimThreshold);
    }

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {

        List<Mapping> leavesMappings = new ArrayList<>();
        List<Tree> dstLeaves = retainLeaves(TreeUtils.postOrder(dst));
        for (Iterator<Tree> srcLeaves = TreeUtils.leafIterator(TreeUtils.postOrderIterator(src)); srcLeaves
                .hasNext(); ) {
            Tree srcLeaf = srcLeaves.next();
            for (Tree dstLeaf : dstLeaves) {
                if (mappings.isMappingAllowed(srcLeaf, dstLeaf)) {
                    double sim = StringMetrics.qGramsDistance().compare(srcLeaf.getLabel(), dstLeaf.getLabel());
                    if (sim > labelSimThreshold)
                        leavesMappings.add(new Mapping(srcLeaf, dstLeaf));
                }
            }
        }

        Set<Tree> ignoredSrcTrees = new HashSet<>();
        Set<Tree> ignoredDstTrees = new HashSet<>();
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

    public List<Tree> retainLeaves(List<Tree> trees) {
        Iterator<Tree> treeIterator = trees.iterator();
        while (treeIterator.hasNext()) {
            Tree tree = treeIterator.next();
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

    public double getLabelSimThreshold() {
        return labelSimThreshold;
    }

    public void setLabelSimThreshold(double labelSimThreshold) {
        this.labelSimThreshold = labelSimThreshold;
    }

    @Override
    public Set<ConfigurationOptions> getApplicableOptions() {
        return Sets.newHashSet(ConfigurationOptions.cd_labsim);
    }
}
