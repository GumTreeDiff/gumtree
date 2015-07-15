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

import static org.junit.Assert.assertEquals;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.hash.RollingHashGenerator;
import org.junit.Before;
import org.junit.Test;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.hash.RollingHashGenerator;

public class TestHash {

    ITree root;

    @Before // FIXME Could it be before class ?
    public void init() {

    }

    @Test
    public void testRollingJavaHash() {
        ITree root = TreeLoader.getDummySrc();
        new RollingHashGenerator.JavaRollingHashGenerator().hash(root);
        assertEquals(-1381305887, root.getChild(0).getChild(0).getHash()); // for c
        assertEquals(-1380321823, root.getChild(0).getChild(1).getHash()); // for d
        assertEquals(-1762812253, root.getChild(0).getHash()); // for b
        assertEquals(-1407966943, root.getChild(1).getHash()); // for e
        assertEquals(-295599963, root.getHash()); // for a
    }

}
