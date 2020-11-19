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

package com.github.gumtreediff.test;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.optimal.zs.ZsMatcher;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestZsMatcher {
    @Test
    public void testWithCustomExample() {
        Pair<TreeContext, TreeContext> trees = TreeLoader.getZsCustomPair();
        Tree src = trees.first.getRoot();
        Tree dst = trees.second.getRoot();
        MappingStore mappings = new ZsMatcher().match(src, dst);
        assertEquals(6, mappings.size());
        assertTrue(mappings.has(src, dst.getChild(0)));
        assertTrue(mappings.has(src.getChild(0), dst.getChild("0.0")));
        assertTrue(mappings.has(src.getChild(1), dst.getChild("0.1")));
        assertTrue(mappings.has(src.getChild("1.0"), dst.getChild("0.1.0")));
        assertTrue(mappings.has(src.getChild("1.2"), dst.getChild("0.1.2")));
        assertTrue(mappings.has(src.getChild("1.3"), dst.getChild("0.1.3")));
    }

    @Test
    public void testWithSlideExample() {
        Pair<TreeContext, TreeContext> trees = TreeLoader.getZsSlidePair();
        Tree src = trees.first.getRoot();
        Tree dst = trees.second.getRoot();
        Matcher matcher = new ZsMatcher();
        MappingStore mappings = new ZsMatcher().match(src, dst);
        assertEquals(5, mappings.size());
        assertTrue(mappings.has(src, dst));
        assertTrue(mappings.has(src.getChild("0.0"), dst.getChild(0)));
        assertTrue(mappings.has(src.getChild("0.0.0"), dst.getChild("0.0")));
        assertTrue(mappings.has(src.getChild("0.1"), dst.getChild("1.0")));
        assertTrue(mappings.has(src.getChild("0.2"), dst.getChild(2)));
    }
}
