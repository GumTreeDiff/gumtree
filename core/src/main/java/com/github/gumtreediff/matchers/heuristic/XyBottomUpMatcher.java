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

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeMap;

import java.util.*;


/**
 * Match the nodes using a bottom-up approach. It browse the nodes of the source and destination trees
 * using a post-order traversal, testing if the two selected trees might be mapped. The two trees are mapped 
 * if they are mappable and have a dice coefficient greater than SIM_THRESHOLD. Whenever two trees are mapped
 * a exact ZS algorithm is applied to look to possibly forgotten nodes.
 */
public class XyBottomUpMatcher extends Matcher {

    private static final double SIM_THRESHOLD = Double.parseDouble(System.getProperty("gumtree.match.xy.sim", "0.5"));

    public XyBottomUpMatcher(ITree src, ITree dst, MappingStore store) {
        super(src, dst, store);
    }

    public void match() {
        for (ITree src: this.src.postOrder())  {
            if (src.isRoot()) {
                addMapping(src, this.dst);
                lastChanceMatch(src, this.dst);
            } else if (!(mappings.hasSrc(src) || src.isLeaf())) {
                Set<ITree> candidates = getDstCandidates(src);
                ITree best = null;
                double max = -1D;

                for (ITree cand: candidates ) {
                    double sim = jaccardSimilarity(src, cand);
                    if (sim > max && sim >= SIM_THRESHOLD) {
                        max = sim;
                        best = cand;
                    }
                }

                if (best != null) {
                    lastChanceMatch(src, best);
                    addMapping(src, best);
                }
            }
        }
    }

    private Set<ITree> getDstCandidates(ITree src) {
        Set<ITree> seeds = new HashSet<>();
        for (ITree c: src.getDescendants()) {
            ITree m = mappings.getDst(c);
            if (m != null) seeds.add(m);
        }
        Set<ITree> candidates = new HashSet<>();
        Set<ITree> visited = new HashSet<>();
        for (ITree seed: seeds) {
            while (seed.getParent() != null) {
                ITree parent = seed.getParent();
                if (visited.contains(parent))
                    break;
                visited.add(parent);
                if (parent.getType() == src.getType() && !mappings.hasDst(parent))
                    candidates.add(parent);
                seed = parent;
            }
        }

        return candidates;
    }

    private void lastChanceMatch(ITree src, ITree dst) {
        Map<Integer,List<ITree>> srcKinds = new HashMap<>();
        Map<Integer,List<ITree>> dstKinds = new HashMap<>();
        for (ITree c: src.getChildren()) {
            if (!srcKinds.containsKey(c.getType())) srcKinds.put(c.getType(), new ArrayList<>());
            srcKinds.get(c.getType()).add(c);
        }
        for (ITree c: dst.getChildren()) {
            if (!dstKinds.containsKey(c.getType())) dstKinds.put(c.getType(), new ArrayList<>());
            dstKinds.get(c.getType()).add(c);
        }

        for (int t: srcKinds.keySet())
            if (dstKinds.get(t) != null && srcKinds.get(t).size() == dstKinds.get(t).size()
                    && srcKinds.get(t).size() == 1)
                addMapping(srcKinds.get(t).get(0), dstKinds.get(t).get(0));
    }
}
