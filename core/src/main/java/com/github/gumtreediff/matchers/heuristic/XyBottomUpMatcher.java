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

import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.Type;
import com.google.common.collect.Sets;

/**
 * Match the nodes using a bottom-up approach. It browse the nodes of the source
 * and destination trees using a post-order traversal, testing if the two
 * selected trees might be mapped. The two trees are mapped if they are mappable
 * and have a dice coefficient greater than SIM_THRESHOLD.
 */
public class XyBottomUpMatcher implements Matcher {
    private static final double DEFAULT_SIM_THRESHOLD = 0.5;
    protected double simThreshold = DEFAULT_SIM_THRESHOLD;

    public XyBottomUpMatcher() {
    }

    @Override
    public void configure(GumtreeProperties properties) {
        simThreshold = properties.tryConfigure(ConfigurationOptions.xy_minsim, DEFAULT_SIM_THRESHOLD);
    }

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        for (var currentSrc : src.postOrder()) {
            if (currentSrc.isRoot()) {
                mappings.addMapping(currentSrc, dst);
                lastChanceMatch(mappings, currentSrc, dst);
            } else if (!(mappings.isSrcMapped(currentSrc) || currentSrc.isLeaf())) {
                var dstCandidates = getDstCandidates(mappings, currentSrc);
                Tree best = null;
                var max = -1D;

                for (var dstCandidate : dstCandidates) {
                    var sim = SimilarityMetrics.jaccardSimilarity(currentSrc, dstCandidate, mappings);
                    if (sim > max && sim >= simThreshold) {
                        max = sim;
                        best = dstCandidate;
                    }
                }

                if (best != null) {
                    lastChanceMatch(mappings, currentSrc, best);
                    mappings.addMapping(currentSrc, best);
                }
            }
        }
        return mappings;
    }

    private Set<Tree> getDstCandidates(MappingStore mappings, Tree src) {
        Set<Tree> mappedSrcDescendantsInDst = new HashSet<>();
        for (var srcDescendant : src.getDescendants()) {
            var dstMappedToSrcDescendant = mappings.getDstForSrc(srcDescendant);
            if (dstMappedToSrcDescendant != null)
                mappedSrcDescendantsInDst.add(dstMappedToSrcDescendant);
        }
        Set<Tree> dstCandidates = new HashSet<>();
        Set<Tree> visitedDsts = new HashSet<>();
        for (var mappedDescendant : mappedSrcDescendantsInDst) {
            while (mappedDescendant.getParent() != null) {
                var parent = mappedDescendant.getParent();
                if (visitedDsts.contains(parent))
                    break;
                visitedDsts.add(parent);
                if (parent.getType() == src.getType() && !mappings.isDstMapped(parent))
                    dstCandidates.add(parent);
                mappedDescendant = parent;
            }
        }

        return dstCandidates;
    }

    private void lastChanceMatch(MappingStore mappings, Tree src, Tree dst) {
        Map<Type, List<Tree>> srcTypes = new HashMap<>();
        Map<Type, List<Tree>> dstTypes = new HashMap<>();
        for (var srcChild : src.getChildren()) {
            if (!srcTypes.containsKey(srcChild.getType()))
                srcTypes.put(srcChild.getType(), new ArrayList<>());
            srcTypes.get(srcChild.getType()).add(srcChild);
        }
        for (var dstChild : dst.getChildren()) {
            if (!dstTypes.containsKey(dstChild.getType()))
                dstTypes.put(dstChild.getType(), new ArrayList<>());
            dstTypes.get(dstChild.getType()).add(dstChild);
        }

        for (var type : srcTypes.keySet())
            if (srcTypes.get(type).size() == 1 && dstTypes.get(type) != null && dstTypes.get(type).size() == 1)
                mappings.addMapping(srcTypes.get(type).get(0), dstTypes.get(type).get(0));
    }

    public double getSimThreshold() {
        return simThreshold;
    }

    public void setSimThreshold(double simThreshold) {
        this.simThreshold = simThreshold;
    }

    @Override
    public Set<ConfigurationOptions> getApplicableOptions() {
        return Sets.newHashSet(ConfigurationOptions.xy_minsim);
    }
}
