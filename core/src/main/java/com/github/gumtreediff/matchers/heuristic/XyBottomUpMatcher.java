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

package com.github.gumtreediff.matchers.heuristic;

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
import com.github.gumtreediff.tree.Type;
import com.google.common.collect.Sets;

/**
 * Match the nodes using a bottom-up approach. It browse the nodes of the source
 * and destination trees using a post-order traversal, testing if the two
 * selected trees might be mapped. The two trees are mapped if they are mappable
 * and have a dice coefficient greater than SIM_THRESHOLD. Whenever two trees
 * are mapped a exact ZS algorithm is applied to look to possibly forgotten
 * nodes.
 */
public class XyBottomUpMatcher implements Matcher, Configurable {

    private static final double DEFAULT_SIM_THRESHOLD = 0.5;

    protected double simThreshold = DEFAULT_SIM_THRESHOLD;

    public XyBottomUpMatcher() {

    }

    @Override
    public void configure(GumTreeProperties properties) {
        simThreshold = properties.tryConfigure(ConfigurationOptions.GT_XYM_SIM, DEFAULT_SIM_THRESHOLD);
    }

    @Override
    public MappingStore match(ITree src, ITree dst, MappingStore mappings) {

        for (ITree iSrc : src.postOrder()) {
            if (iSrc.isRoot()) {
                mappings.addMapping(iSrc, dst);
                lastChanceMatch(mappings, iSrc, dst);
            } else if (!(mappings.isSrcMapped(iSrc) || iSrc.isLeaf())) {
                Set<ITree> candidates = getDstCandidates(mappings, iSrc);
                ITree best = null;
                double max = -1D;

                for (ITree cand : candidates) {
                    double sim = SimilarityMetrics.jaccardSimilarity(iSrc, cand, mappings);
                    if (sim > max && sim >= simThreshold) {
                        max = sim;
                        best = cand;
                    }
                }

                if (best != null) {
                    lastChanceMatch(mappings, iSrc, best);
                    mappings.addMapping(iSrc, best);
                }
            }
        }
        return mappings;
    }

    private Set<ITree> getDstCandidates(MappingStore mappings, ITree src) {
        Set<ITree> seeds = new HashSet<>();
        for (ITree c : src.getDescendants()) {
            ITree m = mappings.getDstForSrc(c);
            if (m != null)
                seeds.add(m);
        }
        Set<ITree> candidates = new HashSet<>();
        Set<ITree> visited = new HashSet<>();
        for (ITree seed : seeds) {
            while (seed.getParent() != null) {
                ITree parent = seed.getParent();
                if (visited.contains(parent))
                    break;
                visited.add(parent);
                if (parent.getType() == src.getType() && !mappings.isDstMapped(parent))
                    candidates.add(parent);
                seed = parent;
            }
        }

        return candidates;
    }

    private void lastChanceMatch(MappingStore mappings, ITree src, ITree dst) {
        Map<Type, List<ITree>> srcKinds = new HashMap<>();
        Map<Type, List<ITree>> dstKinds = new HashMap<>();
        for (ITree c : src.getChildren()) {
            if (!srcKinds.containsKey(c.getType()))
                srcKinds.put(c.getType(), new ArrayList<>());
            srcKinds.get(c.getType()).add(c);
        }
        for (ITree c : dst.getChildren()) {
            if (!dstKinds.containsKey(c.getType()))
                dstKinds.put(c.getType(), new ArrayList<>());
            dstKinds.get(c.getType()).add(c);
        }

        for (Type t : srcKinds.keySet())
            if (dstKinds.get(t) != null && srcKinds.get(t).size() == dstKinds.get(t).size()
                    && srcKinds.get(t).size() == 1)
                mappings.addMapping(srcKinds.get(t).get(0), dstKinds.get(t).get(0));
    }

    public double getSim_threshold() {
        return simThreshold;
    }

    public void setSim_threshold(double simThreshold) {
        this.simThreshold = simThreshold;
    }

    @Override
    public Set<ConfigurationOptions> getApplicableOptions() {

        return Sets.newHashSet(ConfigurationOptions.GT_XYM_SIM);
    }
}
