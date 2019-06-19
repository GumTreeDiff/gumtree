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

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.MultiMappingStore;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TypeSet;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class TestMappingStore {
    @Test
    public void testMappingStore() {
        ITree t1 = new Tree(TypeSet.type("foo"));
        ITree t2 = new Tree(TypeSet.type("foo"));
        MappingStore ms = new MappingStore(t1, t2);
        assertEquals(0, ms.size());
        assertFalse(ms.isSrcMapped(t1));
        assertFalse(ms.isDstMapped(t2));
        ms.addMapping(t1, t2);
        assertEquals(1, ms.size());
        assertTrue(ms.isSrcMapped(t1));
        assertTrue(ms.isDstMapped(t2));
        assertFalse(ms.areBothUnmapped(t1, t2));
        ITree t3 = new Tree(TypeSet.type("foo"));
        ITree t4 = new Tree(TypeSet.type("foo"));
        assertFalse(ms.areSrcsUnmapped(Arrays.asList(new ITree[] {t1, t3})));
        assertFalse(ms.areDstsUnmapped(Arrays.asList(new ITree[] {t2, t4})));
        assertFalse(ms.areBothUnmapped(t1, t3));
        assertFalse(ms.areBothUnmapped(t3, t2));
        assertTrue(ms.areBothUnmapped(t3, t4));
        Mapping m = ms.asSet().iterator().next();
        assertEquals(t1, m.first);
        assertEquals(t2, m.second);
        ms.removeMapping(t1, t2);
        assertEquals(0, ms.size());
        assertTrue(ms.areSrcsUnmapped(Arrays.asList(new ITree[] {t1, t3})));
        assertTrue(ms.areDstsUnmapped(Arrays.asList(new ITree[] {t2, t4})));
        t3.setParentAndUpdateChildren(t1);
        t4.setParentAndUpdateChildren(t2);
        ms.addMappingRecursively(t1, t2);
        assertEquals(2, ms.size());
        assertTrue(ms.has(t1, t2));
        assertTrue(ms.has(t3, t4));
    }

    @Test
    public void testMultiMappingStore() {
        MultiMappingStore ms = new MultiMappingStore();
        ITree t1 = new Tree(TypeSet.type("foo"));
        ITree t2 = new Tree(TypeSet.type("foo"));
        ms.addMapping(t1, t2);
        assertEquals(1, ms.size());
        assertTrue(ms.has(t1, t2));
        assertTrue(ms.isSrcUnique(t1));
        assertTrue(ms.isDstUnique(t2));
        ITree t3 = new Tree(TypeSet.type("foo"));
        ITree t4 = new Tree(TypeSet.type("foo"));
        ms.addMapping(t3, t4);
        assertEquals(2, ms.size());
        assertTrue(ms.has(t3, t4));
        assertTrue(ms.isSrcUnique(t3));
        assertTrue(ms.isDstUnique(t4));
        ms.addMapping(t1, t4);
        System.out.println(ms);
        assertEquals(3, ms.size());
        assertTrue(ms.has(t1, t4));
        assertFalse(ms.isSrcUnique(t1));
        assertFalse(ms.isDstUnique(t4));
        assertTrue(ms.isSrcUnique(t3));
        assertTrue(ms.isDstUnique(t2));
        ms.removeMapping(t1, t4);
        assertEquals(2, ms.size());
        assertTrue(ms.isSrcUnique(t1));
        assertTrue(ms.isDstUnique(t2));
        assertTrue(ms.isSrcUnique(t3));
        assertTrue(ms.isDstUnique(t4));
    }
}
