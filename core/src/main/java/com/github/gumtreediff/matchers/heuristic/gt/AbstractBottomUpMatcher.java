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
 * Copyright 2011-2016 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2016 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.matchers.heuristic.gt;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.optimal.zs.ZsMatcher;
import com.github.gumtreediff.tree.ITree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractBottomUpMatcher {
    public static int SIZE_THRESHOLD =
            Integer.parseInt(System.getProperty("gt.bum.szt", "1000"));
    public static double SIM_THRESHOLD =
            Double.parseDouble(System.getProperty("gt.bum.smt", "0.5"));

    protected abstract static class Implementation {
        protected final ITree src;
        protected final ITree dst;
        protected final MappingStore mappings;

        public Implementation(ITree src, ITree dst, MappingStore mappings) {
            this.src = src;
            this.dst = dst;
            this.mappings = mappings;
        }

        protected List<ITree> getDstCandidates(ITree src) {
            List<ITree> seeds = new ArrayList<>();
            for (ITree c : src.getDescendants()) {
                if (mappings.isSrcMapped(c))
                    seeds.add(mappings.getDstForSrc(c));
            }
            List<ITree> candidates = new ArrayList<>();
            Set<ITree> visited = new HashSet<>();
            for (ITree seed : seeds) {
                while (seed.getParent() != null) {
                    ITree parent = seed.getParent();
                    if (visited.contains(parent))
                        break;
                    visited.add(parent);
                    if (parent.getType() == src.getType() && !(mappings.isDstMapped(parent) || parent.isRoot()))
                        candidates.add(parent);
                    seed = parent;
                }
            }

            return candidates;
        }

        protected void lastChanceMatch(ITree src, ITree dst) {
            if (src.getMetrics().size < AbstractBottomUpMatcher.SIZE_THRESHOLD
                    || dst.getMetrics().size < AbstractBottomUpMatcher.SIZE_THRESHOLD) {
                Matcher m = new ZsMatcher();
                MappingStore zsMappings = m.match(src, dst, new MappingStore(src, dst));
                for (Mapping candidate : zsMappings) {
                    ITree srcCand = candidate.first;
                    ITree dstCand = candidate.second;
                    if (mappings.isMappingAllowed(srcCand, dstCand))
                        mappings.addMapping(srcCand, dstCand);
                }
            }
        }
    }
}