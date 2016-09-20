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
import com.github.gumtreediff.matchers.heuristic.gt.CompleteBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.XyBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.CliqueSubtreeMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.GreedyBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.GreedySubtreeMatcher;
import com.github.gumtreediff.tree.ITree;

public class CompositeMatchers {

    @Register(id = "gumtree", defaultMatcher = true, priority = Registry.Priority.HIGH)
    public static class ClassicGumtree extends CompositeMatcher {

        public ClassicGumtree(ITree src, ITree dst, MappingStore store) {
            super(src, dst, store, new Matcher[]{
                    new GreedySubtreeMatcher(src, dst, store),
                    new GreedyBottomUpMatcher(src, dst, store)
            });
        }
    }

    @Register(id = "gumtree-complete")
    public static class CompleteGumtreeMatcher extends CompositeMatcher {

        public CompleteGumtreeMatcher(ITree src, ITree dst, MappingStore store) {
            super(src, dst, store, new Matcher[]{
                    new CliqueSubtreeMatcher(src, dst, store),
                    new CompleteBottomUpMatcher(src, dst, store)
            });
        }
    }

    @Register(id = "change-distiller")
    public static class ChangeDistiller extends CompositeMatcher {

        public ChangeDistiller(ITree src, ITree dst, MappingStore store) {
            super(src, dst, store, new Matcher[]{
                    new ChangeDistillerLeavesMatcher(src, dst, store),
                    new ChangeDistillerBottomUpMatcher(src, dst, store)
            });
        }
    }

    @Register(id = "xy")
    public static class XyMatcher extends CompositeMatcher {

        public XyMatcher(ITree src, ITree dst, MappingStore store) {
            super(src, dst, store, new Matcher[]{
                    new GreedySubtreeMatcher(src, dst, store),
                    new XyBottomUpMatcher(src, dst, store)
            });
        }
    }
}