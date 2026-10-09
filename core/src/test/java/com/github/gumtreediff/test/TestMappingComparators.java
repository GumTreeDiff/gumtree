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
 * Copyright 2022 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.test;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.heuristic.gt.MappingComparators;
import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TypeSet;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestMappingComparators {
    @Test
    public void testTwinMappings() {
        Tree src = new DefaultTree(TypeSet.type("foo"));
        src.addChild(new DefaultTree(TypeSet.type("foo")));
        src.getChild(0).setPos(0);
        src.getChild(0).setLength(3);
        src.addChild(new DefaultTree(TypeSet.type("foo")));
        src.getChild(1).setPos(3);
        src.getChild(1).setLength(3);
        Tree dst = src.deepCopy();
        MappingStore ms = new MappingStore(src, dst);
        ms.addMapping(src.getChild(0), dst.getChild(1));
        ms.addMapping(src.getChild(1), dst.getChild(0));
        List<Mapping> mappings = new ArrayList<>(ms.asSet());
        MappingComparators.SiblingsSimilarityMappingComparator sc =
                new MappingComparators.SiblingsSimilarityMappingComparator(ms);
        assertEquals(0, sc.compare(mappings.get(0), mappings.get(1)));
        MappingComparators.ParentsSimilarityMappingComparator  pc =
                new MappingComparators.ParentsSimilarityMappingComparator();
        assertEquals(0, pc.compare(mappings.get(0), mappings.get(1)));
        MappingComparators.PositionInParentsSimilarityMappingComparator ppc =
                new MappingComparators.PositionInParentsSimilarityMappingComparator();
        assertEquals(0, ppc.compare(mappings.get(0), mappings.get(1)));
        MappingComparators.TextualPositionDistanceMappingComparator tc =
                new MappingComparators.TextualPositionDistanceMappingComparator();
        assertEquals(0, tc.compare(mappings.get(0), mappings.get(1)));
        MappingComparators.AbsolutePositionDistanceMappingComparator ac =
                new MappingComparators.AbsolutePositionDistanceMappingComparator();
        assertEquals(0, ac.compare(mappings.get(0), mappings.get(1)));
    }
}
