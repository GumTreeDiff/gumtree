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

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.Sets;

public abstract class AbstractSubtreeMatcher implements Matcher {
    private static final int DEFAULT_MIN_PRIORITY = 2;
    protected int minPriority = DEFAULT_MIN_PRIORITY;

    private static final String DEFAULT_PRIORITY_CALCULATOR = "height";
    protected Function<Tree, Integer> priorityCalculator = PriorityTreeQueue.getPriorityCalculator(
            DEFAULT_PRIORITY_CALCULATOR);

    protected Tree src;
    protected Tree dst;
    protected MappingStore mappings;

    public AbstractSubtreeMatcher() {
    }

    @Override
    public void configure(GumTreeProperties properties) {
        this.minPriority = properties.tryConfigure(ConfigurationOptions.st_minprio, minPriority);
        this.priorityCalculator = PriorityTreeQueue.getPriorityCalculator(
                properties.tryConfigure(ConfigurationOptions.st_priocalc, DEFAULT_PRIORITY_CALCULATOR));
    }

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        this.src = src;
        this.dst = dst;
        this.mappings = mappings;

        var multiMappings = new MultiMappingStore();
        PriorityTreeQueue srcTrees = new DefaultPriorityTreeQueue(src, this.minPriority,
                this.priorityCalculator);
        PriorityTreeQueue dstTrees = new DefaultPriorityTreeQueue(dst, this.minPriority,
                this.priorityCalculator);

        while (!(srcTrees.isEmpty() || dstTrees.isEmpty())) {
            PriorityTreeQueue.synchronize(srcTrees, dstTrees);
            var currentPrioritySrcTrees = srcTrees.pop();
            var currentPriorityDstTrees = dstTrees.pop();

            for (var currentSrc : currentPrioritySrcTrees)
                for (var currentDst : currentPriorityDstTrees)
                    if (currentSrc.getMetrics().hash == currentDst.getMetrics().hash)
                        if (currentSrc.isIsomorphicTo(currentDst))
                            multiMappings.addMapping(currentSrc, currentDst);

            for (var t : currentPrioritySrcTrees)
                if (!multiMappings.hasSrc(t))
                    srcTrees.open(t);
            for (var t : currentPriorityDstTrees)
                if (!multiMappings.hasDst(t))
                    dstTrees.open(t);
        }

        filterMappings(multiMappings);
        return this.mappings;
    }

    public abstract void filterMappings(MultiMappingStore multiMappings);

    protected double sim(Tree src, Tree dst) {
        var jaccard = SimilarityMetrics.jaccardSimilarity(src.getParent(), dst.getParent(), mappings);
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

    protected void retainBestMapping(List<Mapping> mappingList, Set<Tree> srcIgnored, Set<Tree> dstIgnored) {
        while (mappingList.size() > 0) {
            var mapping = mappingList.remove(0);
            if (!(srcIgnored.contains(mapping.first) || dstIgnored.contains(mapping.second))) {
                mappings.addMappingRecursively(mapping.first, mapping.second);
                srcIgnored.add(mapping.first);
                srcIgnored.addAll(mapping.first.getDescendants());
                dstIgnored.add(mapping.second);
                dstIgnored.addAll(mapping.second.getDescendants());
            }
        }
    }

    public int getMinPriority() {
        return minPriority;
    }

    public void setMinPriority(int minPriority) {
        this.minPriority = minPriority;
    }

    @Override
    public Set<ConfigurationOptions> getApplicableOptions() {
        return Sets.newHashSet(ConfigurationOptions.st_priocalc,
                ConfigurationOptions.st_minprio);
    }

}
