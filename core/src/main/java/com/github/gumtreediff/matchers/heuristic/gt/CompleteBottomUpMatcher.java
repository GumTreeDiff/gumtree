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

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Match the nodes using a bottom-up approach. It browse the nodes of the source and destination trees
 * using a post-order traversal, testing if the two selected trees might be mapped. The two trees are mapped 
 * if they are mappable and have a dice coefficient greater than SIM_THRESHOLD. Whenever two trees are mapped
 * a exact ZS algorithm is applied to look to possibly forgotten nodes.
 */
public class CompleteBottomUpMatcher extends AbstractBottomUpMatcher {

    public CompleteBottomUpMatcher(ITree src, ITree dst, MappingStore store) {
        super(src, dst, store);
    }

    public void match() {
        for (ITree t: src.postOrder())  {
            if (t.isRoot()) {
                addMapping(t, this.dst);
                lastChanceMatch(t, this.dst);
                break;
            } else if (!(isSrcMatched(t) || t.isLeaf())) {
                List<ITree> srcCandidates = t.getParents().stream()
                        .filter(p -> p.getType() == t.getType())
                        .collect(Collectors.toList());

                List<ITree> dstCandidates = getDstCandidates(t);
                ITree srcBest = null;
                ITree dstBest = null;
                double max = -1D;
                for (ITree srcCand: srcCandidates) {
                    for (ITree dstCand: dstCandidates) {

                        double sim = jaccardSimilarity(srcCand, dstCand);
                        if (sim > max && sim >= SIM_THRESHOLD) {
                            max = sim;
                            srcBest = srcCand;
                            dstBest = dstCand;
                        }
                    }
                }

                if (srcBest != null) {
                    lastChanceMatch(srcBest, dstBest);
                    addMapping(srcBest, dstBest);
                }
            }
        }
    }
}
