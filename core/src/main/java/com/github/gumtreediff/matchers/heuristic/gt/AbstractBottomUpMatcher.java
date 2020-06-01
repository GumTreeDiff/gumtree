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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.gumtreediff.matchers.Configurable;
import com.github.gumtreediff.matchers.ConfigurationOptions;
import com.github.gumtreediff.matchers.GumTreeProperties;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.optimal.zs.ZsMatcher;
import com.github.gumtreediff.tree.ITree;
import com.google.common.collect.Sets;

public abstract class AbstractBottomUpMatcher implements Matcher, Configurable {

    private static final int DEFAULT_SIZE_THRESHOLD = 1000;
    private static final double DEFAULT_SIM_THRESHOLD = 0.5;

    protected int size_threshold = DEFAULT_SIZE_THRESHOLD;
    protected double sim_threshold = DEFAULT_SIM_THRESHOLD;

    public AbstractBottomUpMatcher() {

    }

    @Override
    public void configure(GumTreeProperties properties) {
        size_threshold = properties.tryConfigure(ConfigurationOptions.GT_BUM_SZT, size_threshold);
        sim_threshold = properties.tryConfigure(ConfigurationOptions.GT_BUM_SMT, sim_threshold);
    }

    protected List<ITree> getDstCandidates(MappingStore mappings, ITree src) {
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

    protected void lastChanceMatch(MappingStore mappings, ITree src, ITree dst) {
        if (src.getMetrics().size < size_threshold || dst.getMetrics().size < size_threshold) {
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

    public int getSize_threshold() {
        return size_threshold;
    }

    public void setSize_threshold(int sizeThreshold) {
        this.size_threshold = sizeThreshold;
    }

    public double getSim_threshold() {
        return sim_threshold;
    }

    public void setSim_threshold(double simThreshold) {
        this.sim_threshold = simThreshold;
    }

    @Override
    public Set<ConfigurationOptions> getApplicableOptions() {

        return Sets.newHashSet(ConfigurationOptions.GT_BUM_SZT, ConfigurationOptions.GT_BUM_SMT);
    }
}