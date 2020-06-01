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

import java.util.List;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.SimilarityMetrics;
import com.github.gumtreediff.tree.ITree;

/**
 * Match the nodes using a bottom-up approach. It browse the nodes of the source
 * and destination trees using a post-order traversal, testing if the two
 * selected trees might be mapped. The two trees are mapped if they are mappable
 * and have a dice coefficient greater than SIM_THRESHOLD. Whenever two trees
 * are mapped a exact ZS algorithm is applied to look to possibly forgotten
 * nodes.
 */
public class GreedyBottomUpMatcher extends AbstractBottomUpMatcher {
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
                for (ITree cand : candidates) {
                    double sim = SimilarityMetrics.diceSimilarity(t, cand, mappings);
                    if (sim > max && sim >= sim_threshold) {
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
}
