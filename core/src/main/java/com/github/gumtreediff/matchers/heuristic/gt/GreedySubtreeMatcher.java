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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MultiMappingStore;
import com.github.gumtreediff.tree.Tree;

public class GreedySubtreeMatcher extends AbstractSubtreeMatcher {
    public GreedySubtreeMatcher() {
    }

    @Override
    public void filterMappings(MultiMappingStore multiMappings) {
        // Select unique mappings first and extract ambiguous mappings.
        List<Mapping> ambiguousList = new ArrayList<>();
        Set<Tree> ignored = new HashSet<>();
        for (var src : multiMappings.allMappedSrcs()) {
            var isMappingUnique = false;
            if (multiMappings.isSrcUnique(src)) {
                var dst = multiMappings.getDsts(src).stream().findAny().get();
                if (multiMappings.isDstUnique(dst)) {
                    mappings.addMappingRecursively(src, dst);
                    isMappingUnique = true;
                }
            }

            if (!(ignored.contains(src) || isMappingUnique)) {
                var adsts = multiMappings.getDsts(src);
                var asrcs = multiMappings.getSrcs(multiMappings.getDsts(src).iterator().next());
                for (Tree asrc : asrcs)
                    for (Tree adst : adsts)
                        ambiguousList.add(new Mapping(asrc, adst));
                ignored.addAll(asrcs);
            }
        }

        // Rank the mappings by score.
        Set<Tree> srcIgnored = new HashSet<>();
        Set<Tree> dstIgnored = new HashSet<>();
        Collections.sort(ambiguousList, new MappingComparators.FullMappingComparator(mappings));

        // Select the best ambiguous mappings
        retainBestMapping(ambiguousList, srcIgnored, dstIgnored);
    }

}
