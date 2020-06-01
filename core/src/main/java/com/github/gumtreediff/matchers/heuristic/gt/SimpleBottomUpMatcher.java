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
 * Copyright 2011-2016 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2016 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.matchers.heuristic.gt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.gumtreediff.matchers.Configurable;
import com.github.gumtreediff.matchers.ConfigurationOptions;
import com.github.gumtreediff.matchers.GumTreeProperties;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.SimilarityMetrics;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;
import com.github.gumtreediff.tree.Type;
import com.github.gumtreediff.utils.SequenceAlgorithms;
import com.google.common.collect.Sets;

public class SimpleBottomUpMatcher implements Matcher, Configurable {

    private static final double DEFAULT_SIM_THRESHOLD = 0.4;

    protected double sim_threshold = DEFAULT_SIM_THRESHOLD;

    public SimpleBottomUpMatcher() {

    }

    @Override
    public void configure(GumTreeProperties properties) {
        sim_threshold = properties.tryConfigure(ConfigurationOptions.GT_BUM_SMT_SBUP, sim_threshold);
    }

    @Override
    public MappingStore match(ITree src, ITree dst, MappingStore mappings) {

        for (ITree t : src.postOrder()) {
            if (t.isRoot()) {
                mappings.addMapping(t, dst);
                lastChanceMatch(mappings, t, dst);
                break;
            } else if (!(mappings.isSrcMapped(t) || t.isLeaf())) {
                List<ITree> candidates = getDstCandidates(mappings, t);
                ITree best = null;
                double max = -1D;
                int tSize = t.getDescendants().size();

                for (ITree cand : candidates) {
                    double threshold = 1D / (1D + Math.log(cand.getDescendants().size() + tSize));
                    double sim = SimilarityMetrics.chawatheSimilarity(t, cand, mappings);
                    if (sim > max && sim >= threshold) {
                        max = sim;
                        best = cand;
                    }
                }

                if (best != null) {
                    lastChanceMatch(mappings, t, best);
                    mappings.addMapping(t, best);
                }
            }
        }
        return mappings;
    }

    protected List<ITree> getDstCandidates(MappingStore mappings, ITree src) {
        List<ITree> seeds = new ArrayList<>();
        for (ITree c : src.getDescendants()) {
            ITree m = mappings.getDstForSrc(c);
            if (m != null)
                seeds.add(m);
        }
        List<ITree> candidates = new ArrayList<>();
        Set<ITree> visited = new HashSet<>();
        for (ITree seed : seeds) {
            while (seed.getParent() != null) {
                ITree parent = seed.getParent();
                if (visited.contains(parent))
                    break;
                visited.add(parent);
                if (parent.getType() == src.getType() && !mappings.isDstMapped(parent) && !parent.isRoot())
                    candidates.add(parent);
                seed = parent;
            }
        }

        return candidates;
    }

    protected void lastChanceMatch(MappingStore mappings, ITree src, ITree dst) {
        lcsEqualMatching(mappings, src, dst);
        lcsStructureMatching(mappings, src, dst);
        if (src.isRoot() && dst.isRoot())
            histogramMatching(mappings, src, dst);
        else if (!(src.isRoot() || dst.isRoot()))
            if (src.getParent().getType() == dst.getParent().getType())
                histogramMatching(mappings, src, dst);
    }

    protected void lcsEqualMatching(MappingStore mappings, ITree src, ITree dst) {
        List<ITree> srcChildren = src.getChildren();
        List<ITree> dstChildren = dst.getChildren();

        List<int[]> lcs = SequenceAlgorithms.longestCommonSubsequenceWithIsomorphism(srcChildren, dstChildren);
        for (int[] x : lcs) {
            ITree t1 = srcChildren.get(x[0]);
            ITree t2 = dstChildren.get(x[1]);
            if (mappings.areSrcsUnmapped(TreeUtils.preOrder(t1)) && mappings.areDstsUnmapped(TreeUtils.preOrder(t2)))
                mappings.addMappingRecursively(t1, t2);
        }
    }

    protected void lcsStructureMatching(MappingStore mappings, ITree src, ITree dst) {
        List<ITree> srcChildren = src.getChildren();
        List<ITree> dstChildren = dst.getChildren();

        List<int[]> lcs = SequenceAlgorithms.longestCommonSubsequenceWithIsostructure(srcChildren, dstChildren);
        for (int[] x : lcs) {
            ITree t1 = srcChildren.get(x[0]);
            ITree t2 = dstChildren.get(x[1]);
            if (mappings.areSrcsUnmapped(TreeUtils.preOrder(t1)) && mappings.areDstsUnmapped(TreeUtils.preOrder(t2)))
                mappings.addMappingRecursively(t1, t2);
        }
    }

    protected void histogramMatching(MappingStore mappings, ITree src, ITree dst) {
        List<ITree> srcChildren = src.getChildren();
        List<ITree> dstChildren = dst.getChildren();

        Map<Type, List<ITree>> srcHistogram = new HashMap<>();
        for (ITree c : srcChildren) {
            if (!srcHistogram.containsKey(c.getType()))
                srcHistogram.put(c.getType(), new ArrayList<>());
            srcHistogram.get(c.getType()).add(c);
        }

        Map<Type, List<ITree>> dstHistogram = new HashMap<>();
        for (ITree c : dstChildren) {
            if (!dstHistogram.containsKey(c.getType()))
                dstHistogram.put(c.getType(), new ArrayList<>());
            dstHistogram.get(c.getType()).add(c);
        }

        for (Type t : srcHistogram.keySet()) {
            if (dstHistogram.containsKey(t) && srcHistogram.get(t).size() == 1 && dstHistogram.get(t).size() == 1) {
                ITree t1 = srcHistogram.get(t).get(0);
                ITree t2 = dstHistogram.get(t).get(0);
                if (mappings.areBothUnmapped(t1, t2)) {
                    mappings.addMapping(t1, t2);
                    lastChanceMatch(mappings, t1, t2);
                }
            }
        }
    }

    public double getSim_threshold() {
        return sim_threshold;
    }

    public void setSim_threshold(double simThreshold) {
        this.sim_threshold = simThreshold;
    }

    @Override
    public Set<ConfigurationOptions> getApplicableOptions() {

        return Sets.newHashSet(ConfigurationOptions.GT_BUM_SMT_SBUP);
    }

}
