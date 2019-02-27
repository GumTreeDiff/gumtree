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
                map.put(list.get(i).getId(), i);
            }
            return map;
        }

        public BfsComparator(ITree src, ITree dst) {
            positionSrc = getHashSet(src);
            positionDst = getHashSet(dst);
        }

        @Override
        public int compare(Mapping o1, Mapping o2) {
            if (o1.first.getId() != o2.first.getId()) {
                return Integer.compare(positionSrc.get(o1.first.getId()),
                        positionSrc.get(o2.first.getId()));
            }
            return Integer.compare(positionDst.get(o1.second.getId()),
                    positionDst.get(o2.second.getId()));
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

    @Override
    protected void addMapping(ITree src, ITree dst) {
        assert (src != null);
        assert (dst != null);
        super.addMapping(src, dst);
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
            ITree parentOld = pair.getFirst().getParent();
            ITree parentNew = pair.getSecond().getParent();
            if (mappings.hasSrc(parentOld) && mappings.getDst(parentOld) != parentNew) {
                if (mappings.hasDst(parentNew) && mappings.getSrc(parentNew) != parentOld) {
                    ITree parentOldOther = mappings.getSrc(parentNew);
                    ITree parentNewOther = mappings.getDst(parentOld);
                    if (parentOld.getLabel().equals(parentNewOther.getLabel())
                            && parentNew.getLabel().equals(parentOldOther.getLabel())) {
                        boolean done = false;
                        for (ITree childOldOther : parentOldOther.getChildren()) {
                            if (mappings.hasSrc(childOldOther)) {
                                ITree childNewOther = mappings.getDst(childOldOther);
                                if (pair.getFirst().getLabel().equals(childNewOther.getLabel())
                                        && childOldOther.getLabel()
                                                .equals(pair.getSecond().getLabel())
                                        || !(pair.getFirst().getLabel()
                                                .equals(pair.getSecond().getLabel())
                                                || childOldOther.getLabel()
                                                        .equals(childNewOther.getLabel()))) {
                                    if (childNewOther.getParent() == parentNewOther) {
                                        if (childOldOther.getType() == pair.getFirst().getType()) {
                                            mappings.unlink(pair.getFirst(), pair.getSecond());
                                            mappings.unlink(childOldOther, childNewOther);
                                            addMapping(pair.getFirst(), childNewOther);
                                            addMapping(childOldOther, pair.getSecond());
                                            // done = true;
                                        }
                                    }
                                }
                            }
                        }
                        if (!done) {
                            for (ITree childNewOther : parentNewOther.getChildren()) {
                                if (mappings.hasDst(childNewOther)) {
                                    ITree childOldOther = mappings.getSrc(childNewOther);
                                    if (childOldOther.getParent() == parentOldOther) {
                                        if (childNewOther.getType() == pair.getSecond().getType()) {
                                            if (pair.getFirst().getLabel()
                                                    .equals(childNewOther.getLabel())
                                                    && childOldOther.getLabel()
                                                            .equals(pair.getSecond().getLabel())
                                                    || !(pair.getFirst().getLabel()
                                                            .equals(pair.getSecond().getLabel())
                                                            || childOldOther.getLabel().equals(
                                                                    childNewOther.getLabel()))) {
                                                mappings.unlink(pair.getFirst(), pair.getSecond());
                                                mappings.unlink(childOldOther, childNewOther);
                                                addMapping(childOldOther, pair.getSecond());
                                                addMapping(pair.getFirst(), childNewOther);
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
