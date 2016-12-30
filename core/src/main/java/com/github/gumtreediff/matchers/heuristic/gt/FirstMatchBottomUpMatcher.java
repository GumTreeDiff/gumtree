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

/**
 * Match the nodes using a bottom-up approach. It browse the nodes of the source and destination trees
 * using a post-order traversal, testing if the two selected trees might be mapped. The two trees are mapped 
 * if they are mappable and have a dice coefficient greater than SIM_THRESHOLD. Whenever two trees are mapped
 * a exact ZS algorithm is applied to look to possibly forgotten nodes.
 */
public class FirstMatchBottomUpMatcher extends AbstractBottomUpMatcher {

    public FirstMatchBottomUpMatcher(ITree src, ITree dst, MappingStore store) {
        super(src, dst, store);
    }

    public void match() {
        match(removeMatched(src, true), removeMatched(dst, false));
    }

    private void match(ITree src, ITree dst) {
        for (ITree s: src.postOrder())  {
            for (ITree d: dst.postOrder()) {
                if (isMappingAllowed(s, d) && !(s.isLeaf() || d.isLeaf())) {
                    double sim = jaccardSimilarity(s, d);
                    if (sim >= SIM_THRESHOLD || (s.isRoot() && d.isRoot()) ) {
                        if (!(areDescendantsMatched(s, true) || areDescendantsMatched(d, false)))
                            lastChanceMatch(s, d);
                        addMapping(s, d);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Indicate whether or not all the descendants of the trees are already mapped.
     */
    public boolean areDescendantsMatched(ITree tree, boolean isSrc) {
        for (ITree c: tree.getDescendants())
            if (!((isSrc && isSrcMatched(c)) || (!isSrc && isDstMatched(tree)))) // FIXME ugly but this class is unused
                return false;
        return true;
    }

}
