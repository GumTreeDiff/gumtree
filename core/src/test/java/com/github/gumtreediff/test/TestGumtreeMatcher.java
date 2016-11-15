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

package com.github.gumtreediff.test;

import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.heuristic.gt.AbstractBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.GreedySubtreeMatcher;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestGumtreeMatcher {

    @Test
    public void testMinHeightThreshold() {
        Pair<TreeContext, TreeContext> trees = TreeLoader.getGumtreePair();
        GreedySubtreeMatcher.MIN_HEIGHT = 0;
        AbstractBottomUpMatcher.SIZE_THRESHOLD = 0;
        Matcher m = new CompositeMatchers.ClassicGumtree(
                trees.getFirst().getRoot(), trees.getSecond().getRoot(), new MappingStore());
        m.match();
        assertEquals(5, m.getMappingSet().size());
        GreedySubtreeMatcher.MIN_HEIGHT = 1;
        AbstractBottomUpMatcher.SIZE_THRESHOLD = 0;
        m = new CompositeMatchers.ClassicGumtree(
                trees.getFirst().getRoot(), trees.getSecond().getRoot(), new MappingStore());
        m.match();
        assertEquals(4, m.getMappingSet().size());
    }

    @Test
    public void testSizeThreshold() {
        Pair<TreeContext, TreeContext> trees = TreeLoader.getGumtreePair();
        GreedySubtreeMatcher.MIN_HEIGHT = 0;
        AbstractBottomUpMatcher.SIZE_THRESHOLD = 0;
        Matcher m = new CompositeMatchers.ClassicGumtree(
                trees.getFirst().getRoot(), trees.getSecond().getRoot(), new MappingStore());
        m.match();
        assertEquals(5, m.getMappingSet().size());
        GreedySubtreeMatcher.MIN_HEIGHT = 0;
        AbstractBottomUpMatcher.SIZE_THRESHOLD = 5;
        m = new CompositeMatchers.ClassicGumtree(
                trees.getFirst().getRoot(), trees.getSecond().getRoot(), new MappingStore());
        m.match();
        assertEquals(6, m.getMappingSet().size());
    }

}
