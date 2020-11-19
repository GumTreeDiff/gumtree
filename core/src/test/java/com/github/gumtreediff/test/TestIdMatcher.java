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

package com.github.gumtreediff.test;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.heuristic.IdMatcher;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestIdMatcher {
    @Test
    public void testIdMatcher() {
        Pair<TreeContext, TreeContext> trees = TreeLoader.getGumtreePair();
        Tree t1 = trees.first.getRoot();
        Tree t2 = trees.second.getRoot();
        t1.setMetadata("id", "id1");
        t2.setMetadata("id", "id1");

        t1.getChild(0).setMetadata("id", "id2");
        t1.getChild(1).setMetadata("id", "id2");
        t2.getChild(0).setMetadata("id", "id2");

        t1.getChild(2).setMetadata("id", "id3");
        t2.getChild(1).setMetadata("id", "id3");
        t2.getChild(2).setMetadata("id", "id3");

        Matcher matcher = new IdMatcher();
        MappingStore ms = matcher.match(t1, t2);

        assertEquals(1, ms.size());
        assertTrue(ms.has(t1, t2));
    }
}
