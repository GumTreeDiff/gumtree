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

import com.github.gumtreediff.utils.StringAlgorithms;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Register;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;

import java.util.List;

@Register(id = "lcs")
public class LcsMatcher extends Matcher {

    public LcsMatcher(ITree src, ITree dst, MappingStore store) {
        super(src, dst, store);
    }

    @Override
    public void match() {
        List<ITree> srcSeq = TreeUtils.preOrder(src);
        List<ITree> dstSeq = TreeUtils.preOrder(dst);
        List<int[]> lcs = StringAlgorithms.lcss(srcSeq, dstSeq);
        System.out.println(lcs.size());
        for (int[] x: lcs) {

            ITree t1 = srcSeq.get(x[0]);
            ITree t2 = dstSeq.get(x[1]);
            addMapping(t1, t2);
        }
    }
}
