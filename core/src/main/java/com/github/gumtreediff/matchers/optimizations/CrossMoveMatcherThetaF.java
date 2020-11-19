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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;

/**
 * This implements the cross move matcher Theta F.
 */
public class CrossMoveMatcherThetaF implements Matcher {

    private Tree src;
    private Tree dst;
    private MappingStore mappings;

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        this.src = src;
        this.dst = dst;
        this.mappings = mappings;
        thetaF();
        return mappings;
    }

    private void thetaF() {
        LinkedList<Mapping> workList = new LinkedList<>(mappings.asSet());
        Collections.sort(workList, new BfsComparator(src, dst));
        for (Mapping pair : workList) {
            Tree parentOld = pair.first.getParent();
            Tree parentNew = pair.second.getParent();
            if (mappings.isSrcMapped(parentOld) && mappings.getDstForSrc(parentOld) != parentNew) {
                if (mappings.isDstMapped(parentNew) && mappings.getSrcForDst(parentNew) != parentOld) {
                    Tree parentOldOther = mappings.getSrcForDst(parentNew);
                    Tree parentNewOther = mappings.getDstForSrc(parentOld);
                    if (parentOld.getLabel().equals(parentNewOther.getLabel())
                            && parentNew.getLabel().equals(parentOldOther.getLabel())) {
                        boolean done = false;
                        for (Tree childOldOther : parentOldOther.getChildren()) {
                            if (mappings.isSrcMapped(childOldOther)) {
                                Tree childNewOther = mappings.getDstForSrc(childOldOther);
                                if (pair.first.getLabel().equals(childNewOther.getLabel())
                                        && childOldOther.getLabel().equals(pair.second.getLabel())
                                        || !(pair.first.getLabel().equals(pair.second.getLabel())
                                        || childOldOther.getLabel().equals(childNewOther.getLabel()))) {
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
                            for (Tree childNewOther : parentNewOther.getChildren()) {
                                if (mappings.isDstMapped(childNewOther)) {
                                    Tree childOldOther = mappings.getSrcForDst(childNewOther);
                                    if (childOldOther.getParent() == parentOldOther) {
                                        if (childNewOther.getType() == pair.second.getType()) {
                                            if (pair.first.getLabel().equals(childNewOther.getLabel())
                                                    && childOldOther.getLabel().equals(pair.second.getLabel())
                                                    || !(pair.first.getLabel().equals(pair.second.getLabel())
                                                    || childOldOther.getLabel()
                                                    .equals(childNewOther.getLabel()))) {
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

    private class BfsComparator implements Comparator<Mapping> {

        private HashMap<Integer, Integer> positionSrc;
        private HashMap<Integer, Integer> positionDst;

        private HashMap<Integer, Integer> getHashSet(Tree tree) {
            HashMap<Integer, Integer> map = new HashMap<>();
            ArrayList<Tree> list = new ArrayList<>();
            LinkedList<Tree> workList = new LinkedList<>();
            workList.add(tree);
            while (!workList.isEmpty()) {
                Tree node = workList.removeFirst();
                list.add(node);
                workList.addAll(node.getChildren());
            }
            for (int i = 0; i < list.size(); i++) {
                int position = -1;
                map.put(list.get(i).getMetrics().position, i);
            }
            return map;
        }

        public BfsComparator(Tree src, Tree dst) {
            positionSrc = getHashSet(src);
            positionDst = getHashSet(dst);
        }

        @Override
        public int compare(Mapping o1, Mapping o2) {
            if (o1.first.getMetrics().position != o2.first.getMetrics().position) {
                return Integer.compare(positionSrc.get(o1.first.getMetrics().position),
                        positionSrc.get(o2.first.getMetrics().position));
            }
            return Integer.compare(positionDst.get(o1.second.getMetrics().position),
                    positionDst.get(o2.second.getMetrics().position));
        }

    }
}
