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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.tree.TypeSet;
import org.junit.jupiter.api.Test;

import com.github.gumtreediff.matchers.GumtreeProperties;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.heuristic.gt.GreedyBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.GreedySubtreeMatcher;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;

public class TestGumtreeMatcher {
    @Test
    public void testMinHeightThreshold() {
        Pair<TreeContext, TreeContext> trees = TreeLoader.getGumtreePair();
        Tree t1 = trees.first.getRoot();
        Tree t2 = trees.second.getRoot();

        GreedySubtreeMatcher matcher = new GreedySubtreeMatcher();
        matcher.setMinPriority(0);
        MappingStore ms1 = matcher.match(t1, t2);
        assertEquals(4, ms1.size());
        assertTrue(ms1.has(t1.getChild(1), t2.getChild(0)));
        assertTrue(ms1.has(t1.getChild("1.0"), t2.getChild("0.0")));
        assertTrue(ms1.has(t1.getChild("1.1"), t2.getChild("0.1")));
        assertTrue(ms1.has(t1.getChild(2), t2.getChild(2)));

        matcher.setMinPriority(1);
        MappingStore ms2 = matcher.match(t1, t2);
        assertEquals(3, ms2.size());
        assertTrue(ms1.has(t1.getChild(1), t2.getChild(0)));
        assertTrue(ms1.has(t1.getChild("1.0"), t2.getChild("0.0")));
        assertTrue(ms1.has(t1.getChild("1.1"), t2.getChild("0.1")));
    }

    @Test
    public void testSiblingsMappingComparatorPosInParent() {
        Tree t1 = new DefaultTree(TypeSet.type("root"));
        Tree a11 = new DefaultTree(TypeSet.type("a"));
        t1.addChild(a11);
        Tree a12 = new DefaultTree(TypeSet.type("a"));
        t1.addChild(a12);
        Tree a13 = new DefaultTree(TypeSet.type("a"));
        t1.addChild(a13);
        // root
        //     a
        //     a
        //     a

        Tree t2 = new DefaultTree(TypeSet.type("root"));
        Tree a21 = new DefaultTree(TypeSet.type("a"));
        t2.addChild(a21);
        Tree a22 = new DefaultTree(TypeSet.type("a"));
        t2.addChild(a22);
        // root
        //     a
        //     a

        GreedySubtreeMatcher matcher = new GreedySubtreeMatcher();
        matcher.setMinPriority(0);
        MappingStore ms = matcher.match(t1, t2);
        assertTrue(ms.has(a11, a21));
        assertTrue(ms.has(a13, a22));
    }

    @Test
    public void testSiblingsMappingComparatorPosInTree() {
        Tree t1 = new DefaultTree(TypeSet.type("root"));
        Tree a11 = new DefaultTree(TypeSet.type("a"));
        t1.addChild(a11);
        Tree b11 = new DefaultTree(TypeSet.type("b"));
        a11.addChild(b11);
        Tree a12 = new DefaultTree(TypeSet.type("a"));
        t1.addChild(a12);
        Tree b12 = new DefaultTree(TypeSet.type("b"));
        a12.addChild(b12);
        // root
        //     a
        //       b
        //     a
        //       b

        Tree t2 = new DefaultTree(TypeSet.type("root"));
        Tree a21 = new DefaultTree(TypeSet.type("c"));
        t2.addChild(a21);
        Tree b21 = new DefaultTree(TypeSet.type("b"));
        a21.addChild(b21);
        Tree a22 = new DefaultTree(TypeSet.type("c"));
        t2.addChild(a22);
        Tree b22 = new DefaultTree(TypeSet.type("b"));
        a22.addChild(b22);
        // root
        //     c
        //       b
        //     c
        //       b

        GreedySubtreeMatcher matcher = new GreedySubtreeMatcher();
        matcher.setMinPriority(0);
        MappingStore ms = matcher.match(t1, t2);
        assertTrue(ms.has(b11, b21));
        assertTrue(ms.has(b12, b22));
    }

    @Test
    public void testSimAndSizeThreshold() {
        Pair<Tree, Tree> trees = TreeLoader.getBottomUpPair();
        Tree t1 = trees.first;
        Tree t2 = trees.second;
        MappingStore ms = new MappingStore(t1, t2);
        ms.addMapping(t1.getChild("0.2.0"), t2.getChild("0.2.0"));
        ms.addMapping(t1.getChild("0.2.1"), t2.getChild("0.2.1"));
        ms.addMapping(t1.getChild("0.2.2"), t2.getChild("0.2.2"));
        ms.addMapping(t1.getChild("0.2.3"), t2.getChild("0.2.3"));

        GreedyBottomUpMatcher matcher = new GreedyBottomUpMatcher();
        GumtreeProperties properties = new GumtreeProperties();

        matcher.setSimThreshold(1.0);
        matcher.setSizeThreshold(0);

        MappingStore ms1 = matcher.match(t1, t2, new MappingStore(ms));

        assertEquals(5, ms1.size());
        for (Mapping m : ms)
            assertTrue(ms1.has(m.first, m.second));
        assertTrue(ms1.has(t1, t2));

        matcher.setSimThreshold(0.5);
        matcher.setSizeThreshold(0);

        MappingStore ms2 = matcher.match(t1, t2, new MappingStore(ms));
        assertEquals(7, ms2.size());
        for (Mapping m : ms)
            assertTrue(ms2.has(m.first, m.second));
        assertTrue(ms2.has(t1, t2));
        assertTrue(ms2.has(t1.getChild(0), t2.getChild(0)));
        assertTrue(ms2.has(t1.getChild("0.2"), t2.getChild("0.2")));

        matcher.setSimThreshold(0.5);
        matcher.setSizeThreshold(10);

        MappingStore ms3 = matcher.match(t1, t2, new MappingStore(ms));
        assertEquals(9, ms3.size());
        for (Mapping m : ms)
            assertTrue(ms3.has(m.first, m.second));
        assertTrue(ms3.has(t1, t2));
        assertTrue(ms3.has(t1.getChild(0), t2.getChild(0)));
        assertTrue(ms3.has(t1.getChild("0.0"), t2.getChild("0.0")));
        assertTrue(ms3.has(t1.getChild("0.1"), t2.getChild("0.1")));
        assertTrue(ms3.has(t1.getChild("0.2"), t2.getChild("0.2")));
    }
}
