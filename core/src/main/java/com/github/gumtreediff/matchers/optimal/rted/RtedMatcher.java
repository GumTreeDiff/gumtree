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

package com.github.gumtreediff.matchers.optimal.rted;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;

import java.util.ArrayDeque;
import java.util.List;

public class RtedMatcher extends Matcher {

    public RtedMatcher(ITree src, ITree dst, MappingStore store) {
        super(src, dst, store);
    }

    @Override
    public void match() {
        RtedAlgorithm a = new RtedAlgorithm(1D, 1D, 1D);
        a.init(src, dst);
        a.computeOptimalStrategy();
        a.nonNormalizedTreeDist();
        ArrayDeque<int[]> arrayMappings = a.computeEditMapping();
        List<ITree> srcs = TreeUtils.postOrder(src);
        List<ITree> dsts = TreeUtils.postOrder(dst);
        for (int[] m: arrayMappings) {
            if (m[0] != 0 && m[1] != 0) {
                ITree src = srcs.get(m[0] - 1);
                ITree dst = dsts.get(m[1] - 1);
                if (isMappingAllowed(src, dst))
                    addMapping(src, dst);
            }
        }
    }
}
