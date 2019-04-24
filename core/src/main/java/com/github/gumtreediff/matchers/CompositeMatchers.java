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

import com.github.gumtreediff.gen.Registry;
import com.github.gumtreediff.matchers.heuristic.cd.ChangeDistillerBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.cd.ChangeDistillerLeavesMatcher;
import com.github.gumtreediff.matchers.heuristic.cd.ChangeDistillerParallelLeavesMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.*;
import com.github.gumtreediff.matchers.heuristic.XyBottomUpMatcher;
import com.github.gumtreediff.matchers.optimal.rted.RtedMatcher;
import com.github.gumtreediff.matchers.optimizations.*;
import com.github.gumtreediff.tree.ITree;

public class CompositeMatchers {
    public static class CompositeMatcher implements Matcher {
        protected final Matcher[] matchers;

        public CompositeMatcher(Matcher... matchers) {
            this.matchers = matchers;
        }

        @Override
        public MappingStore match(ITree src, ITree dst, MappingStore mappings) {
            for (Matcher matcher : matchers)
                mappings = matcher.match(src, dst, mappings);

            return mappings;
        }
    }

    @Register(id = "gumtree", defaultMatcher = true, priority = Registry.Priority.HIGH)
    public static class ClassicGumtree extends CompositeMatcher {

        public ClassicGumtree() {
            super(new GreedySubtreeMatcher(), new GreedyBottomUpMatcher());
        }
    }

    @Register(id = "gumtree-simple", defaultMatcher = true, priority = Registry.Priority.HIGH)
    public static class SimpleGumtree extends CompositeMatcher {

        public SimpleGumtree() {
            super(new GreedySubtreeMatcher(), new SimpleBottomUpMatcher());
        }
    }

    @Register(id = "gumtree-complete")
    public static class CompleteGumtreeMatcher extends CompositeMatcher {
        public CompleteGumtreeMatcher() {
            super(new CliqueSubtreeMatcher(), new CompleteBottomUpMatcher());
        }
    }

    @Register(id = "change-distiller")
    public static class ChangeDistiller extends CompositeMatcher {
        public ChangeDistiller() {
            super(new ChangeDistillerLeavesMatcher(), new ChangeDistillerBottomUpMatcher());
        }
    }

    @Register(id = "xy")
    public static class XyMatcher extends CompositeMatcher {
        public XyMatcher() {
            super(new GreedySubtreeMatcher(), new XyBottomUpMatcher());
        }
    }

    @Register(id = "cdabcdefseq")
    public static class CdabcdefSeq extends CompositeMatcher {
        public CdabcdefSeq() {
            super(new IdenticalSubtreeMatcherThetaA(),
                    new ChangeDistillerLeavesMatcher(),
                    new ChangeDistillerBottomUpMatcher(),
                    new LcsOptMatcherThetaB(),
                    new UnmappedLeavesMatcherThetaC(),
                    new InnerNodesMatcherThetaD(),
                    new LeafMoveMatcherThetaE(),
                    new CrossMoveMatcherThetaF());
        }
    }

    @Register(id = "cdabcdefpar")
    public static class CdabcdefPar extends CompositeMatcher {
        /**
         * Instantiates the parallel ChangeDistiller version with Theta A-F.
         */
        public CdabcdefPar() {
            super(new IdenticalSubtreeMatcherThetaA(),
                    new ChangeDistillerParallelLeavesMatcher(),
                    new ChangeDistillerBottomUpMatcher(),
                    new LcsOptMatcherThetaB(),
                    new UnmappedLeavesMatcherThetaC(),
                    new InnerNodesMatcherThetaD(),
                    new LeafMoveMatcherThetaE(),
                    new CrossMoveMatcherThetaF());
        }
    }

    @Register(id = "gtbcdef")
    public static class Gtbcdef extends CompositeMatcher {
        /**
         * Instantiates GumTree with Theta B-F.
         */
        public Gtbcdef() {
            super(new GreedySubtreeMatcher(),
                    new GreedyBottomUpMatcher(),
                    new LcsOptMatcherThetaB(),
                    new UnmappedLeavesMatcherThetaC(),
                    new InnerNodesMatcherThetaD(),
                    new LeafMoveMatcherThetaE(),
                    new CrossMoveMatcherThetaF());
        }
    }

    @Register(id = "rtedacdef")
    public static class Rtedacdef extends CompositeMatcher {
        /**
         * Instantiates RTED with Theta A-F.
         */
        public Rtedacdef() {
            super(new IdenticalSubtreeMatcherThetaA(),
                    new RtedMatcher(),
                    new LcsOptMatcherThetaB(),
                    new UnmappedLeavesMatcherThetaC(),
                    new InnerNodesMatcherThetaD(),
                    new LeafMoveMatcherThetaE(),
                    new CrossMoveMatcherThetaF());
        }
    }
}