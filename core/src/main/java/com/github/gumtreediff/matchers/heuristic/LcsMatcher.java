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

import java.util.List;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Register;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;
import com.github.gumtreediff.utils.SequenceAlgorithms;

@Register(id = "longestCommonSequence")
public class LcsMatcher implements Matcher {
    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        List<Tree> srcSeq = TreeUtils.preOrder(src);
        List<Tree> dstSeq = TreeUtils.preOrder(dst);
        List<int[]> lcs = SequenceAlgorithms.longestCommonSubsequenceWithTypeAndLabel(srcSeq, dstSeq);
        for (int[] x : lcs) {
            Tree t1 = srcSeq.get(x[0]);
            Tree t2 = dstSeq.get(x[1]);
            mappings.addMapping(t1, t2);
        }
        return mappings;
    }
}
