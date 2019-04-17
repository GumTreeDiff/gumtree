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
 * Copyright 2015-2016 Georg Dotzler <georg.dotzler@fau.de>
 * Copyright 2015-2016 Marius Kamp <marius.kamp@fau.de>
 */

package com.github.gumtreediff.matchers.optimizations;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;


/**
 * This implements the cross move matcher Theta F.
 *
 */
public class CrossMoveMatcherThetaF extends Matcher {

    private class BfsComparator implements Comparator<Mapping> {

        private HashMap<Integer, Integer> positionSrc;
        private HashMap<Integer, Integer> positionDst;

        private HashMap<Integer, Integer> getHashSet(ITree tree) {
            HashMap<Integer, Integer> map = new HashMap<>();
            ArrayList<ITree> list = new ArrayList<>();
            LinkedList<ITree> workList = new LinkedList<>();
            workList.add(tree);
            while (!workList.isEmpty()) {
                ITree node = workList.removeFirst();
                list.add(node);
                workList.addAll(node.getChildren());
            }
            for (int i = 0; i < list.size(); i++) {
                int position = -1;
                if (srcMetrics.get(list.get(i)) != null) //TODO improve this, it checks in both src and dst provider to know where the tree is.
                    map.put(srcMetrics.get(list.get(i)).position, i);
                else
                    map.put(dstMetrics.get(list.get(i)).position, i);
            }
            return map;
        }

        public BfsComparator(ITree src, ITree dst) {
            positionSrc = getHashSet(src);
            positionDst = getHashSet(dst);
        }

        @Override
        public int compare(Mapping o1, Mapping o2) {
            if (srcMetrics.get(o1.first).position != srcMetrics.get(o2.first).position) {
                return Integer.compare(positionSrc.get(srcMetrics.get(o1.first).position),
                        positionSrc.get(srcMetrics.get(o2.first).position));
            }
            return Integer.compare(positionDst.get(dstMetrics.get(o1.second).position),
                    positionDst.get(dstMetrics.get(o2.second).position));
        }

    }

    /**
     * Instantiates a new matcher for Theta F.
     *
     * @param src the src
     * @param dst the dst
     * @param store the store
     */
    public CrossMoveMatcherThetaF(ITree src, ITree dst, MappingStore store) {
        super(src, dst, store);
    }

    /**
     * Match.
     */
    @Override
    public void match() {
        thetaF();
    }

    private void thetaF() {
        LinkedList<Mapping> workList = new LinkedList<>(mappings.asSet());
        Collections.sort(workList, new BfsComparator(src, dst));
        for (Mapping pair : workList) {
            ITree parentOld = pair.first.getParent();
            ITree parentNew = pair.second.getParent();
            if (mappings.isSrcMapped(parentOld) && mappings.getDstForSrc(parentOld) != parentNew) {
                if (mappings.isDstMapped(parentNew) && mappings.getSrcForDst(parentNew) != parentOld) {
                    ITree parentOldOther = mappings.getSrcForDst(parentNew);
                    ITree parentNewOther = mappings.getDstForSrc(parentOld);
                    if (parentOld.getLabel().equals(parentNewOther.getLabel())
                            && parentNew.getLabel().equals(parentOldOther.getLabel())) {
                        boolean done = false;
                        for (ITree childOldOther : parentOldOther.getChildren()) {
                            if (mappings.isSrcMapped(childOldOther)) {
                                ITree childNewOther = mappings.getDstForSrc(childOldOther);
                                if (pair.first.getLabel().equals(childNewOther.getLabel())
                                        && childOldOther.getLabel()
                                                .equals(pair.second.getLabel())
                                        || !(pair.first.getLabel()
                                                .equals(pair.second.getLabel())
                                                || childOldOther.getLabel()
                                                        .equals(childNewOther.getLabel()))) {
                                    if (childNewOther.getParent() == parentNewOther) {
                                        if (childOldOther.getType() == pair.first.getType()) {
                                            mappings.removeMapping(pair.first, pair.second);
                                            mappings.removeMapping(childOldOther, childNewOther);
                                            mappings.addMapping(pair.first, childNewOther);
                                            mappings.addMapping(childOldOther, pair.second);
                                            // done = true;
                                        }
                                    }
                                }
                            }
                        }
                        if (!done) {
                            for (ITree childNewOther : parentNewOther.getChildren()) {
                                if (mappings.isDstMapped(childNewOther)) {
                                    ITree childOldOther = mappings.getSrcForDst(childNewOther);
                                    if (childOldOther.getParent() == parentOldOther) {
                                        if (childNewOther.getType() == pair.second.getType()) {
                                            if (pair.first.getLabel()
                                                    .equals(childNewOther.getLabel())
                                                    && childOldOther.getLabel()
                                                            .equals(pair.second.getLabel())
                                                    || !(pair.first.getLabel()
                                                            .equals(pair.second.getLabel())
                                                            || childOldOther.getLabel().equals(
                                                                    childNewOther.getLabel()))) {
                                                mappings.removeMapping(pair.first, pair.second);
                                                mappings.removeMapping(childOldOther, childNewOther);
                                                mappings.addMapping(childOldOther, pair.second);
                                                mappings.addMapping(pair.first, childNewOther);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
