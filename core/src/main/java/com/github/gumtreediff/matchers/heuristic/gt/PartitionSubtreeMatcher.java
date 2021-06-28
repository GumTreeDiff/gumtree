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

import java.util.*;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MultiMappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Subtree matcher that selects one mapping for each distinct subtree hash.
 */
public class PartitionSubtreeMatcher extends AbstractSubtreeMatcher {
    @Override
    public void filterMappings(MultiMappingStore multiMappings) {
        Int2ObjectOpenHashMap<Pair<List<Tree>, List<Tree>>> partitions = new Int2ObjectOpenHashMap<>();
        for (Mapping m : multiMappings) {
            int hash = m.first.getMetrics().hash;
            if (!partitions.containsKey(hash))
                partitions.put(hash, new Pair<>(new ArrayList<>(), new ArrayList<>()));
            partitions.get(hash).first.add(m.first);
            partitions.get(hash).second.add(m.second);
        }

        List<Pair<List<Tree>, List<Tree>>> ambiguousPartitions = new ArrayList<>();
        for (int hash : partitions.keySet()) {
            var partition = partitions.get(hash);
            if (partition.first.size() == 1 && partition.second.size() == 1)
                mappings.addMappingRecursively(partition.first.get(0), partition.second.get(0));
            else
                ambiguousPartitions.add(partition);
        }

        Collections.sort(ambiguousPartitions, new PartitionComparator());
        for (var partition : ambiguousPartitions) {
            List<Mapping> ambiguousMappingsForHash = fromPartition(partition);
            Collections.sort(ambiguousMappingsForHash, new MappingComparators.FullMappingComparator(mappings));
            retainBestMapping(ambiguousMappingsForHash, new HashSet<>(), new HashSet<>());
        }
    }

    private List<Mapping> fromPartition(Pair<List<Tree>, List<Tree>> partition) {
        List<Mapping> partitionAsMappings = new ArrayList<>();
        for (Tree src : partition.first)
            for (Tree dst : partition.second)
                partitionAsMappings.add(new Mapping(src, dst));
        return partitionAsMappings;
    }

    private class PartitionComparator implements Comparator<Pair<List<Tree>, List<Tree>>> {
        @Override
        public int compare(Pair<List<Tree>, List<Tree>> l1, Pair<List<Tree>, List<Tree>> l2) {
            int minDepth1 = minDepth(l1);
            int minDepth2 = minDepth(l2);
            if (minDepth1 != minDepth2)
                return -1 * Integer.compare(minDepth1, minDepth2);
            else {
                int size1 = size(l1);
                int size2 = size(l2);
                return -1 * Integer.compare(size1, size2);
            }
        }

        private int minDepth(Pair<List<Tree>, List<Tree>> trees) {
            int depth = Integer.MAX_VALUE;
            for (Tree t : trees.first)
                if (depth > t.getMetrics().depth)
                    depth = t.getMetrics().depth;
            for (Tree t : trees.second)
                if (depth > t.getMetrics().depth)
                    depth = t.getMetrics().depth;
            return depth;
        }

        private int size(Pair<List<Tree>, List<Tree>> trees) {
            return trees.first.size() + trees.second.size();
        }
    }
}
