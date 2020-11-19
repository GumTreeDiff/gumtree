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
 * Copyright 2020 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.matchers.heuristic;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IdMatcher implements Matcher {
    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        Map<String, Set<Tree>> srcCandidateMappings = new HashMap<>();
        for (Tree t: TreeUtils.preOrder(src)) {
            String id = (String) t.getMetadata("id");
            if (id != null) {
                if (!srcCandidateMappings.containsKey(id))
                    srcCandidateMappings.put(id, new HashSet<>());
                srcCandidateMappings.get(id).add(t);
            }
        }

        Map<String, Set<Tree>> dstCandidateMappings = new HashMap<>();
        for (Tree t: TreeUtils.preOrder(dst)) {
            String id = (String) t.getMetadata("id");
            if (id != null) {
                if (!dstCandidateMappings.containsKey(id))
                    dstCandidateMappings.put(id, new HashSet<>());
                dstCandidateMappings.get(id).add(t);
            }
        }

        for (String id: srcCandidateMappings.keySet()) {
            if (srcCandidateMappings.get(id).size() == 1 && dstCandidateMappings.containsKey(id)
                    && dstCandidateMappings.get(id).size() == 1)
                mappings.addMapping(srcCandidateMappings.get(id).iterator().next(),
                        dstCandidateMappings.get(id).iterator().next());
        }

        return mappings;
    }
}
