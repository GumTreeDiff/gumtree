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

import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.matchers.optimal.zs.ZsMatcher;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;
import com.github.gumtreediff.tree.Type;
import com.github.gumtreediff.utils.SequenceAlgorithms;
import com.google.common.collect.Sets;

import java.util.*;

public class HybridBottomUpMatcher implements Matcher {
    private static final int DEFAULT_SIZE_THRESHOLD = 20;
    protected int sizeThreshold = DEFAULT_SIZE_THRESHOLD;

    @Override
    public void configure(GumtreeProperties properties) {
        sizeThreshold = properties.tryConfigure(ConfigurationOptions.bu_minsize, sizeThreshold);
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
                var max = -1D;
                var tSize = t.getDescendants().size();

                for (var candidate : candidates) {
                    var threshold = 1D / (1D + Math.log(candidate.getDescendants().size() + tSize));
                    var sim = SimilarityMetrics.chawatheSimilarity(t, candidate, mappings);
                    if (sim > max && sim >= threshold) {
                        max = sim;
                        best = candidate;
                    }
                }

                if (best != null) {
                    lastChanceMatch(mappings, t, best);
                    mappings.addMapping(t, best);
                }
            }
            else if (mappings.isSrcMapped(t) && mappings.hasUnmappedSrcChildren(t)
                       && mappings.hasUnmappedDstChildren(mappings.getDstForSrc(t)))
                lastChanceMatch(mappings, t, mappings.getDstForSrc(t));
        }
        return mappings;
    }

    protected List<Tree> getDstCandidates(MappingStore mappings, Tree src) {
        List<Tree> seeds = new ArrayList<>();
        for (Tree c : src.getDescendants()) {
            Tree m = mappings.getDstForSrc(c);
            if (m != null)
                seeds.add(m);
        }
        List<Tree> candidates = new ArrayList<>();
        Set<Tree> visited = new HashSet<>();
        for (var seed : seeds) {
            while (seed.getParent() != null) {
                var parent = seed.getParent();
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

    protected void lastChanceMatch(MappingStore mappings, Tree src, Tree dst) {
        if (src.getMetrics().size < sizeThreshold || dst.getMetrics().size < sizeThreshold)
            optimalLastChanceMatch(mappings, src, dst);
        else
            simpleLastChanceMatch(mappings, src, dst);
    }

    protected void optimalLastChanceMatch(MappingStore mappings, Tree src, Tree dst) {
        Matcher m = new ZsMatcher();
        MappingStore zsMappings = m.match(src, dst, new MappingStore(src, dst));
        for (Mapping candidate : zsMappings) {
            Tree srcCand = candidate.first;
            Tree dstCand = candidate.second;
            if (mappings.isMappingAllowed(srcCand, dstCand))
                mappings.addMapping(srcCand, dstCand);
        }
    }

    protected void simpleLastChanceMatch(MappingStore mappings, Tree src, Tree dst) {
        lcsEqualMatching(mappings, src, dst);
        lcsStructureMatching(mappings, src, dst);
        if (src.isRoot() && dst.isRoot())
            histogramMatching(mappings, src, dst);
        else if (!(src.isRoot() || dst.isRoot()))
            if (src.getParent().getType() == dst.getParent().getType())
                histogramMatching(mappings, src, dst);
    }

    protected void lcsEqualMatching(MappingStore mappings, Tree src, Tree dst) {
        List<Tree> srcChildren = src.getChildren();
        List<Tree> dstChildren = dst.getChildren();

        List<int[]> lcs = SequenceAlgorithms.longestCommonSubsequenceWithIsomorphism(srcChildren, dstChildren);
        for (int[] x : lcs) {
            var t1 = srcChildren.get(x[0]);
            var t2 = dstChildren.get(x[1]);
            if (mappings.areSrcsUnmapped(TreeUtils.preOrder(t1)) && mappings.areDstsUnmapped(TreeUtils.preOrder(t2)))
                mappings.addMappingRecursively(t1, t2);
        }
    }

    protected void lcsStructureMatching(MappingStore mappings, Tree src, Tree dst) {
        List<Tree> srcChildren = src.getChildren();
        List<Tree> dstChildren = dst.getChildren();

        List<int[]> lcs = SequenceAlgorithms.longestCommonSubsequenceWithIsostructure(srcChildren, dstChildren);
        for (int[] x : lcs) {
            var t1 = srcChildren.get(x[0]);
            var t2 = dstChildren.get(x[1]);
            if (mappings.areSrcsUnmapped(TreeUtils.preOrder(t1)) && mappings.areDstsUnmapped(TreeUtils.preOrder(t2)))
                mappings.addMappingRecursively(t1, t2);
        }
    }

    protected void histogramMatching(MappingStore mappings, Tree src, Tree dst) {
        List<Tree> srcChildren = src.getChildren();
        List<Tree> dstChildren = dst.getChildren();

        Map<Type, List<Tree>> srcHistogram = new HashMap<>();
        for (var c : srcChildren) {
            if (!srcHistogram.containsKey(c.getType()))
                srcHistogram.put(c.getType(), new ArrayList<>());
            srcHistogram.get(c.getType()).add(c);
        }

        Map<Type, List<Tree>> dstHistogram = new HashMap<>();
        for (var c : dstChildren) {
            if (!dstHistogram.containsKey(c.getType()))
                dstHistogram.put(c.getType(), new ArrayList<>());
            dstHistogram.get(c.getType()).add(c);
        }

        for (Type t : srcHistogram.keySet()) {
            if (dstHistogram.containsKey(t) && srcHistogram.get(t).size() == 1 && dstHistogram.get(t).size() == 1) {
                var t1 = srcHistogram.get(t).get(0);
                var t2 = dstHistogram.get(t).get(0);
                if (mappings.areBothUnmapped(t1, t2)) {
                    mappings.addMapping(t1, t2);
                    lastChanceMatch(mappings, t1, t2);
                }
            }
        }
    }

    @Override
    public Set<ConfigurationOptions> getApplicableOptions() {
        return Sets.newHashSet(ConfigurationOptions.bu_minsize);
    }
}
