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
 * Copyright 2016 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.test;


import com.github.gumtreediff.utils.Pair;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class TestPair {

    @Test
    public void testEquals() {
        Pair<String, String> p1 = new Pair<>(new String("a"), new String("b"));
        Pair<String, String> p2 = new Pair<>(new String("a"), new String("b"));
        Pair<String, String> p3 = new Pair<>(new String("b"), new String("a"));
        assertTrue(p1.equals(p2));
        assertTrue(!p1.equals(p3));
    }

}
