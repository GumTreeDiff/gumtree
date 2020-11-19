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

import com.github.gumtreediff.matchers.ConfigurableMatcher;
import com.github.gumtreediff.matchers.ConfigurationOptions;
import com.github.gumtreediff.matchers.GumTreeProperties;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.optimal.zs.ZsMatcher;
import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.Sets;

public abstract class AbstractBottomUpMatcher implements Matcher {
    private static final int DEFAULT_SIZE_THRESHOLD = 1000;
    private static final double DEFAULT_SIM_THRESHOLD = 0.5;

    protected int sizeThreshold = DEFAULT_SIZE_THRESHOLD;
    protected double simThreshold = DEFAULT_SIM_THRESHOLD;

    public AbstractBottomUpMatcher() {

    }

    @Override
    public void configure(GumTreeProperties properties) {
        sizeThreshold = properties.tryConfigure(ConfigurationOptions.bu_minsize, sizeThreshold);
        simThreshold = properties.tryConfigure(ConfigurationOptions.bu_minsim, simThreshold);
    }

    protected List<Tree> getDstCandidates(MappingStore mappings, Tree src) {
        List<Tree> seeds = new ArrayList<>();
        for (Tree c : src.getDescendants()) {
            if (mappings.isSrcMapped(c))
                seeds.add(mappings.getDstForSrc(c));
        }
        List<Tree> candidates = new ArrayList<>();
        Set<Tree> visited = new HashSet<>();
        for (Tree seed : seeds) {
            while (seed.getParent() != null) {
                Tree parent = seed.getParent();
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

    protected void lastChanceMatch(MappingStore mappings, Tree src, Tree dst) {
        if (src.getMetrics().size < sizeThreshold || dst.getMetrics().size < sizeThreshold) {
            Matcher m = new ZsMatcher();
            MappingStore zsMappings = m.match(src, dst, new MappingStore(src, dst));
            for (Mapping candidate : zsMappings) {
                Tree srcCand = candidate.first;
                Tree dstCand = candidate.second;
                if (mappings.isMappingAllowed(srcCand, dstCand))
                    mappings.addMapping(srcCand, dstCand);
            }
        }
    }

    public int getSizeThreshold() {
        return sizeThreshold;
    }

    public void setSizeThreshold(int sizeThreshold) {
        this.sizeThreshold = sizeThreshold;
    }

    public double getSimThreshold() {
        return simThreshold;
    }

    public void setSimThreshold(double simThreshold) {
        this.simThreshold = simThreshold;
    }

    @Override
    public Set<ConfigurationOptions> getApplicableOptions() {
        return Sets.newHashSet(ConfigurationOptions.bu_minsize, ConfigurationOptions.bu_minsim);
    }
}