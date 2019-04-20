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
import com.github.gumtreediff.tree.TreeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * This implements the longestCommonSequence optimization Theta B.
 *
 */

public class LcsOptMatcherThetaB implements Matcher {

    @Override
    public MappingStore match(ITree src, ITree dst, MappingStore mappings) {
        Implementation impl = new Implementation(src, dst, mappings);
        impl.match();
        return impl.mappings;
    }

    private static class Implementation {
        private final ITree src;
        private final ITree dst;
        private final MappingStore mappings;

        public Implementation(ITree src, ITree dst, MappingStore mappings) {
            this.src = src;
            this.dst = dst;
            this.mappings = mappings;
        }

        private void advancedLcsMatching() {
            List<ITree> allNodesSrc = TreeUtils.preOrder(src);
            List<ITree> allNodesDst = TreeUtils.preOrder(dst);
            Set<ITree> unmatchedNodes1 = new HashSet<>();
            Set<ITree> unmatchedNodes2 = new HashSet<>();
            for (ITree node : allNodesSrc) {
                if (!mappings.isSrcMapped(node)) {
                    unmatchedNodes1.add(node);
                }
            }
            for (ITree node : allNodesDst) {
                if (!mappings.isDstMapped(node)) {
                    unmatchedNodes2.add(node);
                }
            }
            if (unmatchedNodes1.size() > 0 && unmatchedNodes2.size() > 0) {
                ArrayList<ITree> workList = new ArrayList<>();
                getUnmatchedNodeListInPostOrder(src, workList);
                HashSet<ITree> checkedParent = new HashSet<>();
                for (ITree node : workList) {
                    if (!unmatchedNodes1.contains(node)) {
                        continue;
                    }
                    ITree parent = node.getParent();
                    if (parent == null) {
                        continue;
                    }

                    ITree partner = null;
                    if (parent == src) {
                        partner = dst;
                    } else {
                        partner = mappings.getDstForSrc(parent);
                    }

                    while (parent != null && partner == null) {
                        parent = parent.getParent();
                        partner = mappings.getDstForSrc(parent);
                    }
                    if (parent != null && partner != null) {
                        if (checkedParent.contains(parent)) {
                            // System.out.println("continue checked");
                            continue;
                        }
                        checkedParent.add(parent);
                        ArrayList<ITree> list1 = new ArrayList<>();
                        ArrayList<ITree> list2 = new ArrayList<>();
                        getNodeListInPostOrder(parent, list1);
                        getNodeListInPostOrder(partner, list2);
                        List<Mapping> lcsMatch = lcs(list1, list2, unmatchedNodes1, unmatchedNodes2);
                        for (Mapping match : lcsMatch) {
                            if (!mappings.isSrcMapped(match.first) && !mappings.isDstMapped(match.second)) {
                                mappings.addMapping(match.first, match.second);
                                unmatchedNodes1.remove(match.first);
                                unmatchedNodes2.remove(match.second);
                            }
                        }
                    }
                }
            }
        }

        private void backtrack(ArrayList<ITree> list1, ArrayList<ITree> list2,
                               LinkedList<Mapping> resultList, int[][] matrix, int ipar, int jpar,
                               Set<ITree> unmatchedNodes1, Set<ITree> unmatchedNodes2) {
            assert (ipar >= 0);
            assert (jpar >= 0);
            while (ipar > 0 && jpar > 0) {
                if (testCondition(list1.get(ipar - 1), list2.get(jpar - 1), unmatchedNodes1,
                        unmatchedNodes2)) {
                    if (!mappings.isSrcMapped(list1.get(ipar - 1))) {
                        resultList.add(new Mapping(list1.get(ipar - 1), list2.get(jpar - 1)));
                    }
                }
                if (matrix[ipar][jpar - 1] > matrix[ipar - 1][jpar]) {
                    jpar--;
                } else {
                    ipar--;
                }
            }
        }

        private void getNodeListInPostOrder(ITree tree, ArrayList<ITree> nodes) {
            if (tree != null) {
                for (ITree child : tree.getChildren()) {
                    getNodeListInPostOrder(child, nodes);
                }
                nodes.add(tree);
            }
        }

        private void getUnmatchedNodeListInPostOrder(ITree tree, ArrayList<ITree> nodes) {
            if (tree != null) {
                for (ITree child : tree.getChildren()) {
                    getNodeListInPostOrder(child, nodes);
                }
                if (!mappings.isSrcMapped(tree) && !mappings.isDstMapped(tree)) {
                    nodes.add(tree);
                }
            }
        }

        private List<Mapping> lcs(ArrayList<ITree> list1, ArrayList<ITree> list2,
                                  Set<ITree> unmatchedNodes1, Set<ITree> unmatchedNodes2) {
            int[][] matrix = new int[list1.size() + 1][list2.size() + 1];
            for (int i = 1; i < list1.size() + 1; i++) {
                for (int j = 1; j < list2.size() + 1; j++) {
                    if (testCondition(list1.get(i - 1), list2.get(j - 1), unmatchedNodes1,
                            unmatchedNodes2)) {
                        matrix[i][j] = matrix[i - 1][j - 1] + 1;
                    } else {
                        matrix[i][j] = Math.max(matrix[i][j - 1], matrix[i - 1][j]);
                    }
                }
            }
            LinkedList<Mapping> resultList = new LinkedList<>();
            backtrack(list1, list2, resultList, matrix, list1.size(), list2.size(), unmatchedNodes1,
                    unmatchedNodes2);
            return resultList;
        }

        public void match() {
            advancedLcsMatching();
        }

        /**
         * Compare two nodes to test longestCommonSequence condition.
         *
         * @param node1           the node1
         * @param node2           the node2
         * @param unmatchedNodes1 the unmatched nodes1
         * @param unmatchedNodes2 the unmatched nodes2
         * @return true, if successful
         */
        public boolean testCondition(ITree node1, ITree node2, Set<ITree> unmatchedNodes1,
                                     Set<ITree> unmatchedNodes2) {
            if (node1.getType() != node2.getType()) {
                return false;
            }
            if (mappings.isSrcMapped(node1) && mappings.getDstForSrc(node1) == node2) {
                return true;
            }
            return unmatchedNodes1.contains(node1) && unmatchedNodes2.contains(node2);
        }
    }
}
