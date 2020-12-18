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
import java.util.stream.Collectors;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.SimilarityMetrics;
import com.github.gumtreediff.tree.Tree;

/**
 * Match the nodes using a bottom-up approach. It browses the nodes of the source
 * and destination trees using a post-order traversal, testing if two
 * selected nodes might be mapped. The two nodes are mapped if they are mappable
 * and have a similarity greater than SIM_THRESHOLD. Whenever two trees
 * are mapped, an optimal TED algorithm is applied to look for possibly forgotten
 * nodes.
 */
public class CompleteBottomUpMatcher extends AbstractBottomUpMatcher {
    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        for (Tree t : src.postOrder()) {
            if (t.isRoot()) {
                mappings.addMapping(t, dst);
                lastChanceMatch(mappings, t, dst);
                break;
            }
            else if (!(mappings.isSrcMapped(t) || t.isLeaf())) {
                List<Tree> srcCandidates = t.getParents().stream().filter(p -> p.getType() == t.getType())
                        .collect(Collectors.toList());
                List<Tree> dstCandidates = getDstCandidates(mappings, t);
                Tree srcBest = null;
                Tree dstBest = null;
                double max = -1D;
                for (Tree srcCand : srcCandidates) {
                    for (Tree dstCand : dstCandidates) {
                        double sim = SimilarityMetrics.jaccardSimilarity(srcCand, dstCand, mappings);
                        if (sim > max && sim >= simThreshold) {
                            max = sim;
                            srcBest = srcCand;
                            dstBest = dstCand;
                        }
                    }
                }

                if (srcBest != null) {
                    lastChanceMatch(mappings, srcBest, dstBest);
                    mappings.addMapping(srcBest, dstBest);
                }
            }
        }
        return mappings;
    }
}
