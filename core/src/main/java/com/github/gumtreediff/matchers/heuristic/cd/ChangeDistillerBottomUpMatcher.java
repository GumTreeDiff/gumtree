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

import java.util.List;
import java.util.Set;

import com.github.gumtreediff.matchers.ConfigurableMatcher;
import com.github.gumtreediff.matchers.ConfigurationOptions;
import com.github.gumtreediff.matchers.GumtreeProperties;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.SimilarityMetrics;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;
import com.google.common.collect.Sets;

public class ChangeDistillerBottomUpMatcher implements ConfigurableMatcher {
    private static final double DEFAULT_STRUCT_SIM_THRESHOLD_1 = 0.6;
    protected double structSimThreshold1 = DEFAULT_STRUCT_SIM_THRESHOLD_1;

    private static final double DEFAULT_STRUCT_SIM_THRESHOLD_2 = 0.4;
    protected double structSimThreshold2 = DEFAULT_STRUCT_SIM_THRESHOLD_1;

    private static final int DEFAULT_MAX_NUMBER_OF_LEAVES = 4;
    protected int maxNumberOfLeaves = DEFAULT_MAX_NUMBER_OF_LEAVES;

    public ChangeDistillerBottomUpMatcher() {
    }

    @Override
    public void configure(GumtreeProperties properties) {
        structSimThreshold1 = properties.tryConfigure(ConfigurationOptions.cd_structsim1,
                DEFAULT_STRUCT_SIM_THRESHOLD_1);

        structSimThreshold2 = properties.tryConfigure(ConfigurationOptions.cd_structsim2,
                DEFAULT_STRUCT_SIM_THRESHOLD_2);

        maxNumberOfLeaves = properties.tryConfigure(ConfigurationOptions.cd_maxleaves, DEFAULT_MAX_NUMBER_OF_LEAVES);

    }

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        List<Tree> dstTrees = TreeUtils.postOrder(dst);
        for (Tree currentSrcTree : src.postOrder()) {
            int numberOfLeaves = numberOfLeaves(currentSrcTree);
            for (Tree currentDstTree : dstTrees) {
                if (mappings.isMappingAllowed(currentSrcTree, currentDstTree)
                        && !(currentSrcTree.isLeaf() || currentDstTree.isLeaf())) {
                    double similarity = SimilarityMetrics.chawatheSimilarity(currentSrcTree, currentDstTree, mappings);
                    if ((numberOfLeaves > maxNumberOfLeaves && similarity >= structSimThreshold1)
                            || (numberOfLeaves <= maxNumberOfLeaves && similarity >= structSimThreshold2)) {
                        mappings.addMapping(currentSrcTree, currentDstTree);
                        break;
                    }
                }
            }
        }

        return mappings;
    }

    private int numberOfLeaves(Tree root) {
        int numberOfLeaves = 0;
        for (Tree tree : root.getDescendants())
            if (tree.isLeaf())
                numberOfLeaves++;
        return numberOfLeaves;
    }

    public double getStructSimThreshold1() {
        return structSimThreshold1;
    }

    public void setStructSimThreshold1(double structSimThreshold1) {
        this.structSimThreshold1 = structSimThreshold1;
    }

    public double getStructSimThreshold2() {
        return structSimThreshold2;
    }

    public void setStructSimThreshold2(double structSimThreshold2) {
        this.structSimThreshold2 = structSimThreshold2;
    }

    public int getMaxNumberOfLeaves() {
        return maxNumberOfLeaves;
    }

    public void setMaxNumberOfLeaves(int maxNumberOfLeaves) {
        this.maxNumberOfLeaves = maxNumberOfLeaves;
    }

    @Override
    public Set<ConfigurationOptions> getApplicableOptions() {
        return Sets.newHashSet(ConfigurationOptions.cd_structsim1, ConfigurationOptions.cd_structsim2,
                ConfigurationOptions.cd_maxleaves);
    }
}
