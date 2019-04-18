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
import com.github.gumtreediff.tree.ITree;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestSimilarityMetrics {
    @Test
    public void testDiceSimilarity() {
        ITree t1 = TreeLoader.getDummySrc();
        ITree t2 = TreeLoader.getDummySrc();
        MappingStore ms = new MappingStore(t1, t2);
        ms.addMappingRecursively(t1.getChild(0), t2.getChild(0));
        double expectedDice = (2.0 * 3.0) / (4.0 + 4.0);
        assertEquals(expectedDice, SimilarityMetrics.diceSimilarity(t1, t2, ms));
    }

    @Test
    public void testJaccardSimilarity() {
        ITree t1 = TreeLoader.getDummySrc();
        ITree t2 = TreeLoader.getDummySrc();
        MappingStore ms = new MappingStore(t1, t2);
        ms.addMappingRecursively(t1.getChild(0), t2.getChild(0));
        double expectedJaccard = (3.0) / (5.0);
        assertEquals(expectedJaccard, SimilarityMetrics.jaccardSimilarity(t1, t2, ms));
    }

    @Test
    public void testChawatheimilarity() {
        ITree t1 = TreeLoader.getDummySrc();
        ITree t2 = TreeLoader.getDummySrc();
        MappingStore ms = new MappingStore(t1, t2);
        ms.addMappingRecursively(t1.getChild(0), t2.getChild(0));
        double expectedChawathe = (3.0) / (4.0);
        assertEquals(expectedChawathe, SimilarityMetrics.chawatheSimilarity(t1, t2, ms));
    }
}
