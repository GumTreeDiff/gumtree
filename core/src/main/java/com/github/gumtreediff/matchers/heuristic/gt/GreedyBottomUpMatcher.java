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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.matchers.optimal.zs.ZsMatcher;
import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.Sets;

/**
 * Match the nodes using a bottom-up approach. It browses the nodes of the source
 * and destination trees using a post-order traversal, testing if two
 * selected nodes might be mapped. The two nodes are mapped if they are mappable
 * and have a similarity greater than SIM_THRESHOLD. Whenever two trees
 * are mapped, an optimal TED algorithm is applied to look for possibly forgotten
 * nodes.
 */
public class GreedyBottomUpMatcher implements Matcher {
    private static final int DEFAULT_SIZE_THRESHOLD = 1000;
    private static final double DEFAULT_SIM_THRESHOLD = 0.5;

    protected int sizeThreshold = DEFAULT_SIZE_THRESHOLD;
    protected double simThreshold = DEFAULT_SIM_THRESHOLD;

    @Override
    public void configure(GumtreeProperties properties) {
        sizeThreshold = properties.tryConfigure(ConfigurationOptions.bu_minsize, sizeThreshold);
        simThreshold = properties.tryConfigure(ConfigurationOptions.bu_minsim, simThreshold);
    }

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        for (Tree t : src.postOrder()) {
            if (t.isRoot()) {
                mappings.addMapping(t, dst);
                lastChanceMatch(mappings, t, dst);
                break;
            }
            else if (!(mappings.isSrcMapped(t) || t.isLeaf())) {
                List<Tree> candidates = getDstCandidates(mappings, t);
                Tree best = null;
                double max = -1D;
                for (Tree cand : candidates) {
                    double sim = SimilarityMetrics.diceSimilarity(t, cand, mappings);
                    if (sim > max && sim >= simThreshold) {
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

    protected List<Tree> getDstCandidates(MappingStore mappings, Tree src) {
        List<Tree> seeds = new ArrayList<>();
        for (Tree c : src.getDescendants()) {
            if (mappings.isSrcMapped(c))
                seeds.add(mappings.getDstForSrc(c));
        }
        List<Tree> candidates = new ArrayList<>();
        Set<Tree> visited = new HashSet<>();
        for (Tree seed : seeds) {
            while (seed.getParent() != null) {
                Tree parent = seed.getParent();
                if (visited.contains(parent))
                    break;
                visited.add(parent);
                if (parent.getType() == src.getType() && !(mappings.isDstMapped(parent) || parent.isRoot()))
                    candidates.add(parent);
                seed = parent;
            }
        }

        return candidates;
    }

    protected void lastChanceMatch(MappingStore mappings, Tree src, Tree dst) {
        if (src.getMetrics().size < sizeThreshold || dst.getMetrics().size < sizeThreshold) {
            Matcher m = new ZsMatcher();
            MappingStore zsMappings = m.match(src, dst, new MappingStore(src, dst));
            for (Mapping candidate : zsMappings) {
                Tree srcCand = candidate.first;
                Tree dstCand = candidate.second;
                if (mappings.isMappingAllowed(srcCand, dstCand))
                    mappings.addMapping(srcCand, dstCand);
            }
        }
    }

    public int getSizeThreshold() {
        return sizeThreshold;
    }

    public void setSizeThreshold(int sizeThreshold) {
        this.sizeThreshold = sizeThreshold;
    }

    public double getSimThreshold() {
        return simThreshold;
    }

    public void setSimThreshold(double simThreshold) {
        this.simThreshold = simThreshold;
    }

    @Override
    public Set<ConfigurationOptions> getApplicableOptions() {
        return Sets.newHashSet(ConfigurationOptions.bu_minsize, ConfigurationOptions.bu_minsim);
    }
}
