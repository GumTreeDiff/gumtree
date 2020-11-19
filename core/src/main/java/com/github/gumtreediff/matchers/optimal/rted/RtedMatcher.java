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

import java.util.ArrayDeque;
import java.util.List;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;

public class RtedMatcher implements Matcher {

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {

        RtedAlgorithm a = new RtedAlgorithm(1D, 1D, 1D);
        a.init(src, dst);
        a.computeOptimalStrategy();
        a.nonNormalizedTreeDist();
        ArrayDeque<int[]> arrayMappings = a.computeEditMapping();
        List<Tree> srcs = TreeUtils.postOrder(src);
        List<Tree> dsts = TreeUtils.postOrder(dst);
        for (int[] m : arrayMappings) {
            if (m[0] != 0 && m[1] != 0) {
                Tree srcg = srcs.get(m[0] - 1);
                Tree dstg = dsts.get(m[1] - 1);
                if (mappings.isMappingAllowed(srcg, dstg))
                    mappings.addMapping(srcg, dstg);
            }
        }

        return mappings;
    }

}
