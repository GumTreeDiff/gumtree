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

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TypeSet;
import com.github.gumtreediff.utils.StringAlgorithms;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

public class TestStringAlgorithms {
    @Test
    public void testStringLcss() {
        String s1 = "abcdefg";
        String s2 = "agcefd";
        List<int[]> idx = StringAlgorithms.lcss(s1, s2);
        assertEquals(4, idx.size());
        assertArrayEquals(new int[]{0, 0}, idx.get(0));
        assertArrayEquals(new int[]{2, 2}, idx.get(1));
        assertArrayEquals(new int[]{4, 3}, idx.get(2));
        assertArrayEquals(new int[]{5, 4}, idx.get(3));

        List<int[]> hunks = StringAlgorithms.hunks(s1, s2);
        assertEquals(3, hunks.size());
        assertArrayEquals(new int[]{0, 1, 0, 1}, hunks.get(0));
        assertArrayEquals(new int[]{2, 3, 2, 3}, hunks.get(1));
        assertArrayEquals(new int[]{4, 6, 3, 5}, hunks.get(2));
    }

    @Test
    public void testITreeLcss() {
        List<ITree> l1 = Arrays.asList(new Tree[] {
                new Tree(TypeSet.type("a")),
                new Tree(TypeSet.type("b")),
                new Tree(TypeSet.type("c")),
                new Tree(TypeSet.type("d")),
                new Tree(TypeSet.type("e")),
                new Tree(TypeSet.type("f")),
                new Tree(TypeSet.type("g")),
        });
        List<ITree> l2 = Arrays.asList(new Tree[] {
                new Tree(TypeSet.type("a")),
                new Tree(TypeSet.type("g")),
                new Tree(TypeSet.type("c")),
                new Tree(TypeSet.type("e")),
                new Tree(TypeSet.type("f")),
                new Tree(TypeSet.type("d"))
        });
        List<int[]> idx = StringAlgorithms.lcss(l1, l2);
        assertEquals(4, idx.size());
        assertArrayEquals(new int[]{0, 0}, idx.get(0));
        assertArrayEquals(new int[]{2, 2}, idx.get(1));
        assertArrayEquals(new int[]{4, 3}, idx.get(2));
        assertArrayEquals(new int[]{5, 4}, idx.get(3));
    }

    @Test
    public void testLcs() {
        String s1 = "abcdefg";
        String s2 = "agcefd";
        String lcs = StringAlgorithms.lcs(s1, s2);
        assertEquals("ef", lcs);
    }
}
