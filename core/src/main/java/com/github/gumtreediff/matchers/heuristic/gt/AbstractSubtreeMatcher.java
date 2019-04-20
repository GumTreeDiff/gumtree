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

package com.github.gumtreediff.matchers.heuristic.gt;

import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.MultiMappingStore;
import com.github.gumtreediff.tree.ITree;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class AbstractSubtreeMatcher {

    public static int MIN_HEIGHT = Integer.parseInt(
            System.getProperty("gt.stm.mh", System.getProperty("gumtree.match.gt.minh", "1"))
    );

    protected abstract static class Implementation {
        protected final ITree src;
        protected final ITree dst;
        protected final MappingStore mappings;

        public Implementation(ITree src, ITree dst, MappingStore mappings) {
            this.src = src;
            this.dst = dst;
            this.mappings = mappings;
        }

        private void popLarger(PriorityTreeList srcTrees, PriorityTreeList dstTrees) {
            if (srcTrees.peekHeight() > dstTrees.peekHeight())
                srcTrees.open();
            else
                dstTrees.open();
        }

        public void match() {
            MultiMappingStore multiMappings = new MultiMappingStore();

            PriorityTreeList srcTrees = new PriorityTreeList(src);
            PriorityTreeList dstTrees = new PriorityTreeList(dst);

            while (srcTrees.peekHeight() != -1 && dstTrees.peekHeight() != -1) {
                while (srcTrees.peekHeight() != dstTrees.peekHeight())
                    popLarger(srcTrees, dstTrees);

                List<ITree> currentHeightSrcTrees = srcTrees.pop();
                List<ITree> currentHeightDstTrees = dstTrees.pop();

                boolean[] marksForSrcTrees = new boolean[currentHeightSrcTrees.size()];
                boolean[] marksForDstTrees = new boolean[currentHeightDstTrees.size()];

                for (int i = 0; i < currentHeightSrcTrees.size(); i++) {
                    for (int j = 0; j < currentHeightDstTrees.size(); j++) {
                        ITree src = currentHeightSrcTrees.get(i);
                        ITree dst = currentHeightDstTrees.get(j);

                        if (src.isIsomorphicTo(dst)) {
                            multiMappings.addMapping(src, dst);
                            marksForSrcTrees[i] = true;
                            marksForDstTrees[j] = true;
                        }
                    }
                }

                for (int i = 0; i < marksForSrcTrees.length; i++)
                    if (marksForSrcTrees[i] == false)
                        srcTrees.open(currentHeightSrcTrees.get(i));
                for (int j = 0; j < marksForDstTrees.length; j++)
                    if (marksForDstTrees[j] == false)
                        dstTrees.open(currentHeightDstTrees.get(j));
                srcTrees.updateHeight();
                dstTrees.updateHeight();
            }

            filterMappings(multiMappings);
        }

        public abstract void filterMappings(MultiMappingStore multiMappings);

        protected double sim(ITree src, ITree dst) {
            double jaccard = SimilarityMetrics.jaccardSimilarity(src.getParent(), dst.getParent(), mappings);
            int posSrc = (src.isRoot()) ? 0 : src.getParent().getChildPosition(src);
            int posDst = (dst.isRoot()) ? 0 : dst.getParent().getChildPosition(dst);
            int maxSrcPos = (src.isRoot()) ? 1 : src.getParent().getChildren().size();
            int maxDstPos = (dst.isRoot()) ? 1 : dst.getParent().getChildren().size();
            int maxPosDiff = Math.max(maxSrcPos, maxDstPos);
            double pos = 1D - ((double) Math.abs(posSrc - posDst) / (double) maxPosDiff);
            double po = 1D - ((double) Math.abs(src.getMetrics().position - dst.getMetrics().position)
                    / (double) this.getMaxTreeSize());
            return 100 * jaccard + 10 * pos + po;
        }

        protected int getMaxTreeSize() {
            return Math.max(src.getMetrics().size, dst.getMetrics().size);
        }

        protected void retainBestMapping(List<Mapping> mappingList, Set<ITree> srcIgnored, Set<ITree> dstIgnored) {
            while (mappingList.size() > 0) {
                Mapping mapping = mappingList.remove(0);
                if (!(srcIgnored.contains(mapping.first) || dstIgnored.contains(mapping.second))) {
                    mappings.addMappingRecursively(mapping.first, mapping.second);
                    srcIgnored.add(mapping.first);
                    dstIgnored.add(mapping.second);
                }
            }
        }

        private static class PriorityTreeList {
            private List<ITree>[] trees;

            private int maxHeight;

            private int currentIdx;

            @SuppressWarnings("unchecked")
            public PriorityTreeList(ITree tree) {
                int listSize = tree.getMetrics().height - MIN_HEIGHT + 1;
                if (listSize < 0)
                    listSize = 0;
                if (listSize == 0)
                    currentIdx = -1;
                trees = (List<ITree>[]) new ArrayList[listSize];
                maxHeight = tree.getMetrics().height;
                addTree(tree);
            }

            private int idx(ITree tree) {
                return idx(tree.getMetrics().height);
            }

            private int idx(int height) {
                return maxHeight - height;
            }

            private int height(int idx) {
                return maxHeight - idx;
            }

            private void addTree(ITree tree) {
                if (tree.getMetrics().height >= MIN_HEIGHT) {
                    int idx = idx(tree);
                    if (trees[idx] == null) trees[idx] = new ArrayList<>();
                    trees[idx].add(tree);
                }
            }

            public List<ITree> open() {
                List<ITree> pop = pop();
                if (pop != null) {
                    for (ITree tree : pop) open(tree);
                    updateHeight();
                    return pop;
                } else return null;
            }

            public List<ITree> pop() {
                if (currentIdx == -1)
                    return null;
                else {
                    List<ITree> pop = trees[currentIdx];
                    trees[currentIdx] = null;
                    return pop;
                }
            }

            public void open(ITree tree) {
                for (ITree c : tree.getChildren()) addTree(c);
            }

            public int peekHeight() {
                return (currentIdx == -1) ? -1 : height(currentIdx);
            }

            public void updateHeight() {
                currentIdx = -1;
                for (int i = 0; i < trees.length; i++) {
                    if (trees[i] != null) {
                        currentIdx = i;
                        break;
                    }
                }
            }
        }
    }
}
