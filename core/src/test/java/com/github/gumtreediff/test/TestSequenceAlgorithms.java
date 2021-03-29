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

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TypeSet;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.utils.SequenceAlgorithms;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

public class TestSequenceAlgorithms {
    @Test
    public void testStringLcss() {
        String s1 = "abcdefg";
        String s2 = "agcefd";
        List<int[]> idx = SequenceAlgorithms.longestCommonSubsequence(s1, s2);
        assertArrayEquals(new int[][] { {0, 0}, {2, 2}, {4, 3}, {5, 4} }, idx.toArray());

        List<int[]> hunks = SequenceAlgorithms.hunks(s1, s2);
        assertArrayEquals(new int[][] { {0, 1, 0, 1}, {2, 3, 2, 3}, {4, 6, 3, 5} }, hunks.toArray());
    }

    @Test
    public void testITreeLcss() {
        List<Tree> l1 = Arrays.asList(new DefaultTree[] {
                new DefaultTree(TypeSet.type("a")),
                new DefaultTree(TypeSet.type("b")),
                new DefaultTree(TypeSet.type("c")),
                new DefaultTree(TypeSet.type("d")),
                new DefaultTree(TypeSet.type("e")),
                new DefaultTree(TypeSet.type("f")),
                new DefaultTree(TypeSet.type("g")),
        });
        List<Tree> l2 = Arrays.asList(new DefaultTree[] {
                new DefaultTree(TypeSet.type("a")),
                new DefaultTree(TypeSet.type("g")),
                new DefaultTree(TypeSet.type("c")),
                new DefaultTree(TypeSet.type("e")),
                new DefaultTree(TypeSet.type("f")),
                new DefaultTree(TypeSet.type("d"))
        });
        List<int[]> idx = SequenceAlgorithms.longestCommonSubsequenceWithTypeAndLabel(l1, l2);
        assertArrayEquals(new int[][] { {0, 0}, {2, 2}, {4, 3}, {5, 4} }, idx.toArray());
    }

    @Test
    public void testITreeLcssIsomorphism() {
        Pair<TreeContext, TreeContext> trees = TreeLoader.getActionPair();
        Tree t1 = trees.first.getRoot();
        Tree t2 = trees.second.getRoot();
        List<int[]> idx =
                SequenceAlgorithms.longestCommonSubsequenceWithIsomorphism(t1.getChildren(), t2.getChildren());

        assertArrayEquals(new int[][] { {1, 0} }, idx.toArray());
    }

    @Test
    public void testLcs() {
        String s1 = "abcdefg";
        String s2 = "agcefd";
        String lcs = SequenceAlgorithms.longestCommonSequence(s1, s2);
        assertEquals("ef", lcs);
    }
}
