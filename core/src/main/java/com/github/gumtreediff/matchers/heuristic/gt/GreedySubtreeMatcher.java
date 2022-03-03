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
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;

public class GreedySubtreeMatcher extends AbstractSubtreeMatcher {
    public GreedySubtreeMatcher() {
    }

    @Override
    public void handleAmbiguousMappings(List<Pair<Set<Tree>, Set<Tree>>> ambiguousMappings) {
        MappingComparators.FullMappingComparator comparator = new MappingComparators.FullMappingComparator(mappings);
        ambiguousMappings.sort(new AmbiguousMappingsComparator());
        ambiguousMappings.forEach((pair) -> {
            List<Mapping> candidates = convertToMappings(pair);
            candidates.sort(comparator);
            candidates.forEach(mapping -> {
                if (mappings.areBothUnmapped(mapping.first, mapping.second))
                    mappings.addMappingRecursively(mapping.first, mapping.second);
            });
        });
    }

    public static final List<Mapping> convertToMappings(Pair<Set<Tree>, Set<Tree>> ambiguousMapping) {
        List<Mapping> mappings = new ArrayList<>();
        for (Tree src : ambiguousMapping.first)
            for (Tree dst : ambiguousMapping.second)
                mappings.add(new Mapping(src, dst));
        return mappings;
    }

    public static class AmbiguousMappingsComparator implements Comparator<Pair<Set<Tree>, Set<Tree>>> {
        @Override
        public int compare(Pair<Set<Tree>, Set<Tree>> m1, Pair<Set<Tree>, Set<Tree>> m2) {
            int s1 = m1.first.stream().max(Comparator.comparingInt(t -> t.getMetrics().size)).get().getMetrics().size;
            int s2 = m1.first.stream().max(Comparator.comparingInt(t -> t.getMetrics().size)).get().getMetrics().size;
            return Integer.compare(s2, s1);
        }
    }
}
