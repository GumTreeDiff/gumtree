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

package com.github.gumtreediff.matchers;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.github.gumtreediff.utils.Registry;
import com.github.gumtreediff.matchers.heuristic.IdMatcher;
import com.github.gumtreediff.matchers.heuristic.XyBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.cd.ChangeDistillerBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.cd.ChangeDistillerLeavesMatcher;
import com.github.gumtreediff.matchers.heuristic.cd.ChangeDistillerParallelLeavesMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.*;
import com.github.gumtreediff.matchers.optimal.rted.RtedMatcher;
import com.github.gumtreediff.matchers.optimizations.CrossMoveMatcherThetaF;
import com.github.gumtreediff.matchers.optimizations.IdenticalSubtreeMatcherThetaA;
import com.github.gumtreediff.matchers.optimizations.InnerNodesMatcherThetaD;
import com.github.gumtreediff.matchers.optimizations.LcsOptMatcherThetaB;
import com.github.gumtreediff.matchers.optimizations.LeafMoveMatcherThetaE;
import com.github.gumtreediff.matchers.optimizations.UnmappedLeavesMatcherThetaC;
import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.Sets;

/**
 * A class defining the CompositeMatcher class, which is a pipeline of matchers.
 * Using this class, several matchers are then defined.
 */
public class CompositeMatchers {
    public static class CompositeMatcher implements ConfigurableMatcher {
        protected final Matcher[] matchers;

        public CompositeMatcher(Matcher... matchers) {
            this.matchers = matchers;
        }

        @Override
        public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
            for (Matcher matcher : matchers)
                mappings = matcher.match(src, dst, mappings);

            return mappings;
        }

        @Override
        public void configure(GumtreeProperties properties) {
            for (Matcher matcher : matchers)
                    matcher.configure(properties);
        }

        public List<Matcher> matchers() {
            return Arrays.asList(matchers);
        }

        @Override
        public Set<ConfigurationOptions> getApplicableOptions() {
            Set<ConfigurationOptions> allOptions = Sets.newHashSet();
            for (Matcher matcher : matchers)
                allOptions.addAll(matcher.getApplicableOptions());

            return allOptions;
        }
    }

    @Register(id = "gumtree", priority = Registry.Priority.MAXIMUM)
    public static class ClassicGumtree extends CompositeMatcher {
        public ClassicGumtree() {
            super(new GreedySubtreeMatcher(), new GreedyBottomUpMatcher());
        }
    }

    @Register(id = "gumtree-simple", priority = Registry.Priority.HIGH)
    public static class SimpleGumtree extends CompositeMatcher {
        public SimpleGumtree() {
            super(new GreedySubtreeMatcher(), new SimpleBottomUpMatcher());
        }
    }

    @Register(id = "gumtree-simple-id", priority = Registry.Priority.HIGH)
    public static class SimpleIdGumtree extends CompositeMatcher {
        public SimpleIdGumtree() {
            super(new IdMatcher(), new GreedySubtreeMatcher(),
                    new SimpleBottomUpMatcher());
        }
    }

    @Register(id = "gumtree-hybrid")
    public static class HybridGumtree extends CompositeMatcher {
        public HybridGumtree() {
            super(new GreedySubtreeMatcher(),
                    new HybridBottomUpMatcher());
        }
    }

    @Register(id = "gumtree-hybrid-id")
    public static class HybridIdGumtree extends CompositeMatcher {
        public HybridIdGumtree() {
            super(new IdMatcher(), new GreedySubtreeMatcher(),
                    new HybridBottomUpMatcher());
        }
    }

    @Register(id = "change-distiller")
    public static class ChangeDistiller extends CompositeMatcher {
        public ChangeDistiller() {
            super(new ChangeDistillerLeavesMatcher(),
                    new ChangeDistillerBottomUpMatcher());
        }
    }

    @Register(id = "xy")
    public static class XyMatcher extends CompositeMatcher {
        public XyMatcher() {
            super(new GreedySubtreeMatcher(),
                    new XyBottomUpMatcher());
        }
    }

    @Register(id = "theta")
    public static class Theta extends CompositeMatcher {
        public Theta() {
            super(new IdenticalSubtreeMatcherThetaA(), new ChangeDistillerLeavesMatcher(),
                    new ChangeDistillerBottomUpMatcher(), new LcsOptMatcherThetaB(), new UnmappedLeavesMatcherThetaC(),
                    new InnerNodesMatcherThetaD(), new LeafMoveMatcherThetaE(), new CrossMoveMatcherThetaF());
        }
    }

    @Register(id = "change-distiller-theta")
    public static class ChangeDistillerTheta extends CompositeMatcher {
        /**
         * Instantiates the parallel ChangeDistiller version with Theta A-F.
         */
        public ChangeDistillerTheta() {
            super(new IdenticalSubtreeMatcherThetaA(), new ChangeDistillerParallelLeavesMatcher(),
                    new ChangeDistillerBottomUpMatcher(), new LcsOptMatcherThetaB(), new UnmappedLeavesMatcherThetaC(),
                    new InnerNodesMatcherThetaD(), new LeafMoveMatcherThetaE(), new CrossMoveMatcherThetaF());
        }
    }

    @Register(id = "classic-gumtree-theta")
    public static class ClassicGumtreeTheta extends CompositeMatcher {
        /**
         * Instantiates GumTree with Theta B-F.
         */
        public ClassicGumtreeTheta() {
            super(new GreedySubtreeMatcher(), new GreedyBottomUpMatcher(), new LcsOptMatcherThetaB(),
                    new UnmappedLeavesMatcherThetaC(), new InnerNodesMatcherThetaD(), new LeafMoveMatcherThetaE(),
                    new CrossMoveMatcherThetaF());
        }
    }

    @Register(id = "gumtree-simple-id-theta")
    public static class SimpleIdGumtreeTheta extends CompositeMatcher {
        /**
         * Instantiates GumTree with Theta B-F.
         */
        public SimpleIdGumtreeTheta() {
            super(new IdMatcher(), new GreedySubtreeMatcher(), new SimpleBottomUpMatcher(), new LcsOptMatcherThetaB(),
                    new UnmappedLeavesMatcherThetaC(), new InnerNodesMatcherThetaD(), new LeafMoveMatcherThetaE(),
                    new CrossMoveMatcherThetaF());
        }
    }

    @Register(id = "rted-theta")
    public static class RtedTheta extends CompositeMatcher {
        /**
         * Instantiates RTED with Theta A-F.
         */
        public RtedTheta() {
            super(new IdenticalSubtreeMatcherThetaA(), new RtedMatcher(), new LcsOptMatcherThetaB(),
                    new UnmappedLeavesMatcherThetaC(), new InnerNodesMatcherThetaD(), new LeafMoveMatcherThetaE(),
                    new CrossMoveMatcherThetaF());
        }
    }
}
