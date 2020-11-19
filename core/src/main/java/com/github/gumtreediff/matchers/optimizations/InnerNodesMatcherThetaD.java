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

import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;

/**
 * This implements the unmapped leaves optimization (Theta C), the inner node
 * repair optimization (Theta D) and the leaf move optimization (Theta E).
 */
public class InnerNodesMatcherThetaD implements Matcher {

    private Tree src;
    private Tree dst;
    private MappingStore mappings;

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        this.src = src;
        this.dst = dst;
        this.mappings = mappings;
        thetaD();
        return mappings;
    }

    private class ChangeMapComparator implements Comparator<Entry<Tree, IdentityHashMap<Tree, Integer>>> {

        @Override
        public int compare(Entry<Tree, IdentityHashMap<Tree, Integer>> o1,
                           Entry<Tree, IdentityHashMap<Tree, Integer>> o2) {

            return Integer.compare(o1.getKey().getMetrics().position, o2.getKey().getMetrics().position);
        }

    }

    private boolean allowedMatching(Tree key, Tree maxNodePartner) {
        while (key != null) {
            if (key == maxNodePartner) {
                return false;
            }
            key = key.getParent();
        }
        return true;
    }
    
    private void thetaD() {
        IdentityHashMap<Tree, IdentityHashMap<Tree, Integer>> parentCount = new IdentityHashMap<>();
        for (Mapping pair : mappings.asSet()) {
            Tree parent = pair.first.getParent();
            Tree parentPartner = pair.second.getParent();
            if (parent != null && parentPartner != null) {
                IdentityHashMap<Tree, Integer> countMap = parentCount.get(parent);
                if (countMap == null) {
                    countMap = new IdentityHashMap<>();
                    parentCount.put(parent, countMap);
                }
                Integer count = countMap.get(parentPartner);
                if (count == null) {
                    count = Integer.valueOf(0);
                }
                countMap.put(parentPartner, count + 1);
            }
        }

        LinkedList<Entry<Tree, IdentityHashMap<Tree, Integer>>> list = new LinkedList<>(parentCount.entrySet());
        Collections.sort(list, new ChangeMapComparator());

        for (Entry<Tree, IdentityHashMap<Tree, Integer>> countEntry : list) {
            int max = Integer.MIN_VALUE;
            int maxCount = 0;
            Tree maxNode = null;
            for (Entry<Tree, Integer> newNodeEntry : countEntry.getValue().entrySet()) {
                if (newNodeEntry.getValue() > max) {
                    max = newNodeEntry.getValue();
                    maxCount = 1;
                    maxNode = newNodeEntry.getKey();
                } else if (newNodeEntry.getValue() == max) {
                    maxCount++;
                }
            }
            if (maxCount == 1) {
                if (mappings.getDstForSrc(countEntry.getKey()) != null && mappings.getSrcForDst(maxNode) != null) {
                    Tree partner = mappings.getDstForSrc(countEntry.getKey());
                    Tree maxNodePartner = mappings.getSrcForDst(maxNode);
                    if (partner != maxNode) {
                        if (max > countEntry.getKey().getChildren().size() / 2
                                || countEntry.getKey().getChildren().size() == 1) {
                            Tree parentPartner = mappings.getDstForSrc(countEntry.getKey().getParent());

                            if (parentPartner != null && parentPartner == partner.getParent()) {
                                continue;
                            }
                            if (allowedMatching(countEntry.getKey(), maxNodePartner)) {
                                if (countEntry.getKey().getType() == maxNode.getType()) {
                                    if (maxNodePartner != null) {
                                        mappings.removeMapping(maxNodePartner, maxNode);
                                    }
                                    if (partner != null) {
                                        mappings.removeMapping(countEntry.getKey(), partner);
                                    }
                                    mappings.addMapping(countEntry.getKey(), maxNode);
                                }
                                if (maxNodePartner != null) {
                                    if (maxNodePartner.getType() == partner.getType()) {
                                        mappings.addMapping(maxNodePartner, partner);
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
