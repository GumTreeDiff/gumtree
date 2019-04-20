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

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.MultiMappingStore;
import com.github.gumtreediff.tree.ITree;

import java.util.*;

public class GreedySubtreeMatcher extends AbstractSubtreeMatcher implements Matcher {
    @Override
    public MappingStore match(ITree src, ITree dst, MappingStore mappings) {
        GreedySubtreeMatcher.Implementation impl =  new GreedySubtreeMatcher.Implementation(src, dst, mappings);
        impl.match();
        return impl.mappings;
    }

    protected static class Implementation extends AbstractSubtreeMatcher.Implementation {
        public Implementation(ITree src, ITree dst, MappingStore mappings) {
            super(src, dst, mappings);
        }

        public void filterMappings(MultiMappingStore multiMappings) {
            // Select unique mappings first and extract ambiguous mappings.
            List<Mapping> ambiguousList = new ArrayList<>();
            Set<ITree> ignored = new HashSet<>();
            for (ITree src : multiMappings.allMappedSrcs()) {
                if (multiMappings.isSrcUnique(src))
                    mappings.addMappingRecursively(src, multiMappings.getDsts(src).iterator().next());
                else if (!ignored.contains(src)) {
                    Set<ITree> adsts = multiMappings.getDsts(src);
                    Set<ITree> asrcs = multiMappings.getSrcs(multiMappings.getDsts(src).iterator().next());
                    for (ITree asrc : asrcs)
                        for (ITree adst : adsts)
                            ambiguousList.add(new Mapping(asrc, adst));
                    ignored.addAll(asrcs);
                }
            }

            // Rank the mappings by score.
            Set<ITree> srcIgnored = new HashSet<>();
            Set<ITree> dstIgnored = new HashSet<>();
            Collections.sort(ambiguousList,
                    new SiblingsMappingComparator(ambiguousList, mappings, getMaxTreeSize()));

            // Select the best ambiguous mappings
            retainBestMapping(ambiguousList, srcIgnored, dstIgnored);
        }
    }
}
