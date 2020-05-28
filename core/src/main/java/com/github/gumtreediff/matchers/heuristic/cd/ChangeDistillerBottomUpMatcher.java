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

import com.github.gumtreediff.matchers.Configurable;
import com.github.gumtreediff.matchers.GumTreeProperties;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.SimilarityMetrics;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;

public class ChangeDistillerBottomUpMatcher implements Matcher, Configurable {

    protected double struct_sim_threshold_1;

    protected double struct_sim_threshold_2;

    protected int max_number_of_leaves;

    public ChangeDistillerBottomUpMatcher() {
        configure(GumTreeProperties.getGlobalProperties());
    }

    @Override
    public void configure(GumTreeProperties properties) {
        struct_sim_threshold_1 = properties.getPropertyDouble("gt.cd.ssim1");

        struct_sim_threshold_2 = properties.getPropertyDouble("gt.cd.ssim2");

        max_number_of_leaves = properties.getPropertyInteger("gt.cd.ml");

    }

    @Override
    public MappingStore match(ITree src, ITree dst, MappingStore mappings) {
        List<ITree> dstTrees = TreeUtils.postOrder(dst);
        for (ITree currentSrcTree : src.postOrder()) {
            int numberOfLeaves = numberOfLeaves(currentSrcTree);
            for (ITree currentDstTree : dstTrees) {
                if (mappings.isMappingAllowed(currentSrcTree, currentDstTree)
                        && !(currentSrcTree.isLeaf() || currentDstTree.isLeaf())) {
                    double similarity = SimilarityMetrics.chawatheSimilarity(currentSrcTree, currentDstTree, mappings);
                    if ((numberOfLeaves > max_number_of_leaves && similarity >= struct_sim_threshold_1)
                            || (numberOfLeaves <= max_number_of_leaves && similarity >= struct_sim_threshold_2)) {
                        mappings.addMapping(currentSrcTree, currentDstTree);
                        break;
                    }
                }
            }
        }

        return mappings;
    }

    private int numberOfLeaves(ITree root) {
        int numberOfLeaves = 0;
        for (ITree tree : root.getDescendants())
            if (tree.isLeaf())
                numberOfLeaves++;
        return numberOfLeaves;
    }

    public double getStruct_sim_threshold_1() {
        return struct_sim_threshold_1;
    }

    public void setStruct_sim_threshold_1(double structSimThreshold1) {
        this.struct_sim_threshold_1 = structSimThreshold1;
    }

    public double getStruct_sim_threshold_2() {
        return struct_sim_threshold_2;
    }

    public void setStruct_sim_threshold_2(double structSimThreshold2) {
        this.struct_sim_threshold_2 = structSimThreshold2;
    }

    public int getMax_number_of_leaves() {
        return max_number_of_leaves;
    }

    public void setMax_number_of_leaves(int maxNumberOfLeaves) {
        this.max_number_of_leaves = maxNumberOfLeaves;
    }

}
