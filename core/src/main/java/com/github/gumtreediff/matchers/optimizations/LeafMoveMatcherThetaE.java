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
import java.util.LinkedList;
import java.util.List;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;

/**
 * This implements the unmapped leaves optimization (Theta C), the inner node
 * repair optimization (Theta D) and the leaf move optimization (Theta E).
 */
public class LeafMoveMatcherThetaE implements Matcher {

    private Tree src;
    private Tree dst;
    private MappingStore mappings;

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        this.src = src;
        this.dst = dst;
        this.mappings = mappings;
        thetaE();
        return mappings;
    }

    private class MappingComparator implements Comparator<Mapping> {

        @Override
        public int compare(Mapping o1, Mapping o2) {

            int posO1 = o1.first.getMetrics().position;
            int posO2 = o2.first.getMetrics().position;
            if (posO1 != posO2) {
                return Integer.compare(posO1, posO2);
            }

            return Integer.compare(o1.second.getMetrics().position, o2.second.getMetrics().position);
        }

    }

    private void thetaE() {
        LinkedList<Mapping> workList = new LinkedList<>();
        LinkedList<Mapping> workListTmp = null;
        LinkedList<Mapping> changeMap = new LinkedList<>();

        for (Mapping pair : mappings.asSet()) {
            if (pair.first.isLeaf() && pair.second.isLeaf()) {
                if (!pair.first.getLabel().equals(pair.second.getLabel())) {
                    workList.add(pair);
                }
            }

        }
        while (!workList.isEmpty()) {
            Collections.sort(workList, new MappingComparator());
            workListTmp = new LinkedList<>();
            for (Mapping pair : workList) {
                Tree firstParent = pair.first.getParent();
                if (!mappings.isDstMapped(firstParent)) {
                    continue;
                }
                Tree secondParent = mappings.getDstForSrc(pair.first.getParent());
                reevaluateLeaves(firstParent, secondParent, pair, changeMap);
            }
            Collections.sort(changeMap, new MappingComparator());
            for (Mapping entry : changeMap) {
                if (mappings.areBothUnmapped(entry.first, entry.second)) {
                    mappings.addMapping(entry.first, entry.second);
                }
                if (!entry.first.getLabel().equals(entry.second.getLabel()) && entry.first.isLeaf()
                        && entry.second.isLeaf()) {
                    workListTmp.add(new Mapping(entry.first, entry.second));
                }
            }
            changeMap.clear();
            workList = workListTmp;
        }

        workList = new LinkedList<>();
        workListTmp = null;

        for (Mapping pair : mappings.asSet()) {
            if (pair.first.isLeaf() && pair.second.isLeaf()) {
                if (!pair.first.getLabel().equals(pair.second.getLabel())) {
                    workList.add(pair);
                }
            }

        }
        while (!workList.isEmpty()) {
            Collections.sort(workList, new MappingComparator());
            workListTmp = new LinkedList<>();
            for (Mapping pair : workList) {
                Tree firstParent = pair.first.getParent();
                Tree secondParent = pair.second.getParent();
                reevaluateLeaves(firstParent, secondParent, pair, changeMap);
            }
            Collections.sort(changeMap, new MappingComparator());
            for (Mapping entry : changeMap) {
                if (mappings.areBothUnmapped(entry.first, entry.second)) {
                    mappings.addMapping(entry.first, entry.second);
                }
                if (!entry.first.getLabel().equals(entry.second.getLabel()) && entry.first.isLeaf()
                        && entry.second.isLeaf()) {
                    workListTmp.add(new Mapping(entry.first, entry.second));
                }
            }
            changeMap.clear();
            workList = workListTmp;
        }
    }

    private void reevaluateLeaves(Tree firstParent, Tree secondParent, Mapping pair, List<Mapping> changeMap) {

        int count = 0;
        Tree foundDstNode = null;
        Tree foundPosDstNode = null;
        int pos = firstParent.getChildren().indexOf(pair.first);

        for (int i = 0; i < secondParent.getChildren().size(); i++) {
            Tree child = secondParent.getChildren().get(i);
            if (child.getType() == pair.first.getType() && child.getLabel().equals(pair.first.getLabel())) {
                count++;
                foundDstNode = child;
                if (i == pos) {
                    foundPosDstNode = child;
                }
            }
        }
        Mapping addedMappingKey = null;

        if ((count == 1 && foundDstNode != null) || foundPosDstNode != null) {
            if (count != 1 && foundPosDstNode != null) {
                foundDstNode = foundPosDstNode;
            }
            if (mappings.isDstMapped(foundDstNode)) {

                Tree foundSrc = mappings.getSrcForDst(foundDstNode);
                if (!foundSrc.getLabel().equals(foundDstNode.getLabel())) {
                    mappings.removeMapping(pair.first, pair.second);
                    mappings.removeMapping(foundSrc, foundDstNode);
                    changeMap.add(new Mapping(pair.first, foundDstNode));
                    addedMappingKey = new Mapping(foundSrc, foundDstNode);
                    if (foundDstNode != pair.second && foundSrc != pair.first) {
                        changeMap.add(new Mapping(foundSrc, pair.second));
                    }
                }
            } else {

                mappings.removeMapping(pair.first, pair.second);
                if (pair.first.getLabel().equals(foundDstNode.getLabel())) {
                    LinkedList<Mapping> toRemove = new LinkedList<>();
                    for (Mapping mapPair : changeMap) {
                        if (mapPair.first == pair.first) {
                            if (!mapPair.first.getLabel().equals(mapPair.second.getLabel())) {
                                toRemove.add(mapPair);
                            }
                        } else if (mapPair.second == foundDstNode) {
                            if (!mapPair.first.getLabel().equals(mapPair.second.getLabel())) {
                                toRemove.add(mapPair);
                            }
                        }
                    }
                    changeMap.removeAll(toRemove);
                }
                changeMap.add(new Mapping(pair.first, foundDstNode));
                for (Tree child : firstParent.getChildren()) {
                    if (child.isLeaf() && !mappings.isDstMapped(child) && child.getType() == pair.second.getType()
                            && child.getLabel().equals(pair.second.getLabel())) {
                        mappings.addMapping(child, pair.second);
                        break;
                    }
                }
            }
        }
        Tree foundSrcNode = null;
        Tree foundPosSrcNode = null;
        pos = secondParent.getChildren().indexOf(pair.second);
        for (int i = 0; i < firstParent.getChildren().size(); i++) {
            Tree child = firstParent.getChildren().get(i);
            if (child.getType() == pair.second.getType() && child.getLabel().equals(pair.second.getLabel())) {
                count++;
                foundSrcNode = child;
                if (i == pos) {
                    foundPosSrcNode = child;
                }
            }
        }
        if ((count == 1 && foundSrcNode != null) || foundPosSrcNode != null) {
            if (count != 1 && foundPosSrcNode != null) {
                foundSrcNode = foundPosSrcNode;
            } else if (foundSrcNode == null) {
                foundSrcNode = foundPosSrcNode;
            }
            if (addedMappingKey != null) {
                changeMap.remove(addedMappingKey);
            }
            if (mappings.isSrcMapped(foundSrcNode)) {
                Tree foundDst = mappings.getSrcForDst(foundSrcNode);
                if (foundDst != null && foundSrcNode != null && !foundDst.getLabel().equals(foundSrcNode.getLabel())) {
                    mappings.removeMapping(pair.first, pair.second);
                    mappings.removeMapping(foundSrcNode, foundDst);
                    changeMap.add(new Mapping(foundSrcNode, pair.second));
                    if (addedMappingKey == null && foundDst != null) {
                        if (foundSrcNode != pair.first && foundDst != pair.second) {
                            changeMap.add(new Mapping(pair.first, foundDst));
                        }
                    }
                }
            } else {
                mappings.removeMapping(pair.first, pair.second);
                if (foundSrcNode.getLabel().equals(pair.second.getLabel())) {
                    LinkedList<Mapping> toRemove = new LinkedList<>();
                    for (Mapping mapPair : changeMap) {
                        if (mapPair.first == foundSrcNode) {
                            if (!mapPair.first.getLabel().equals(mapPair.second.getLabel())) {
                                toRemove.add(mapPair);
                            }
                        } else if (mapPair.second == pair.second) {
                            if (!mapPair.first.getLabel().equals(mapPair.second.getLabel())) {
                                toRemove.add(mapPair);
                            }
                        }
                    }
                    changeMap.removeAll(toRemove);
                }
                changeMap.add(new Mapping(foundSrcNode, pair.second));
                for (Tree child : secondParent.getChildren()) {
                    if (child.isLeaf() && !mappings.isSrcMapped(child) && child.getType() == pair.first.getType()
                            && child.getLabel().equals(pair.first.getLabel())) {
                        mappings.addMapping(pair.first, child);
                        break;
                    }
                }
            }
        }
    }

}
