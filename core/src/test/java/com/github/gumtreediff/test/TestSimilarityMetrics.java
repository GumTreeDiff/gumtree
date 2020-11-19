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
 * Copyright 2019 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.test;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.SimilarityMetrics;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.tree.TypeSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestSimilarityMetrics {
    @Test
    public void testDiceSimilarity() {
        MappingStore ms = getTestData();
        assertEquals(2D / 3D, SimilarityMetrics.diceSimilarity(ms.src, ms.dst, ms));
    }

    @Test
    public void testJaccardSimilarity() {
        MappingStore ms = getTestData();
        assertEquals(0.5D, SimilarityMetrics.jaccardSimilarity(ms.src, ms.dst, ms));
    }

    @Test
    public void testChawatheSimilarity() {
        MappingStore ms = getTestData();
        assertEquals(0.6D, SimilarityMetrics.chawatheSimilarity(ms.src, ms.dst, ms));
    }

    @Test
    public void testOverlapSimilarity() {
        MappingStore ms = getTestData();
        assertEquals(0.75D, SimilarityMetrics.overlapSimilarity(ms.src, ms.dst, ms));
    }

    private static MappingStore getTestData() {
        Tree t1 = TreeLoader.getDummySrc();
        Tree t2 = TreeLoader.getDummySrc();
        t2.addChild(new DefaultTree(TypeSet.type("x"), "x"));
        MappingStore ms = new MappingStore(t1, t2);
        ms.addMappingRecursively(t1.getChild(0), t2.getChild(0));
        return ms;
    }
}
