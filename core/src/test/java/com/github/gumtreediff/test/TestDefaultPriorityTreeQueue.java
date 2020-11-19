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

import com.github.gumtreediff.matchers.heuristic.gt.PriorityTreeQueue;
import com.github.gumtreediff.matchers.heuristic.gt.DefaultPriorityTreeQueue;
import com.github.gumtreediff.tree.Tree;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestDefaultPriorityTreeQueue {
    @Test
    public void testPopOpenWithHeight() {
        var tree = TreeLoader.getDummySrc();
        var queue = new DefaultPriorityTreeQueue(tree, 0,
                PriorityTreeQueue.HEIGHT_PRIORITY_CALCULATOR);
        assertEquals(2, queue.currentPriority());
        List<Tree> p = queue.popOpen();
        assertEquals(1, p.size());
        assertEquals(1, queue.currentPriority());
        p = queue.popOpen();
        assertEquals(0, queue.currentPriority());
        assertEquals(1, p.size());
        p = queue.popOpen();
        assertTrue(queue.isEmpty());
        assertEquals(3, p.size());
    }

    @Test
    public void testPopOpenWithSize() {
        var tree = TreeLoader.getDummySrc();
        var queue = new DefaultPriorityTreeQueue(tree, 0,
                PriorityTreeQueue.SIZE_PRIORITY_CALCULATOR);
        assertEquals(5, queue.currentPriority());
        List<Tree> p = queue.popOpen();
        assertEquals(1, p.size());
        assertEquals(3, queue.currentPriority());
        p = queue.popOpen();
        assertEquals(1, p.size());
        assertEquals(1, queue.currentPriority());
        p = queue.popOpen();
        assertEquals(3, p.size());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testPopOpenWithSizeAndMinPriority() {
        var tree = TreeLoader.getDummySrc();
        var queue = new DefaultPriorityTreeQueue(tree, 2,
                PriorityTreeQueue.SIZE_PRIORITY_CALCULATOR);
        assertEquals(5, queue.currentPriority());
        List<Tree> p = queue.popOpen();
        assertEquals(1, p.size());
        assertEquals(3, queue.currentPriority());
        p = queue.popOpen();
        assertEquals(1, p.size());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testSynchronize() {
        var tree = TreeLoader.getDummySrc();
        var queue1 = new DefaultPriorityTreeQueue(tree, 0, PriorityTreeQueue.HEIGHT_PRIORITY_CALCULATOR);
        var queue2 = new DefaultPriorityTreeQueue(tree, 0, PriorityTreeQueue.HEIGHT_PRIORITY_CALCULATOR);
        queue2.popOpen();
        assertEquals(2, queue1.currentPriority());
        assertEquals(1, queue2.currentPriority());
        PriorityTreeQueue.synchronize(queue1, queue2);
        assertEquals(1, queue1.currentPriority());
        assertEquals(1, queue2.currentPriority());

        var queue3 = new DefaultPriorityTreeQueue(tree, 0, PriorityTreeQueue.HEIGHT_PRIORITY_CALCULATOR);
        queue3.clear();
        PriorityTreeQueue.synchronize(queue2, queue3);
        assertTrue(queue2.isEmpty());
        assertTrue(queue3.isEmpty());
    }

}
