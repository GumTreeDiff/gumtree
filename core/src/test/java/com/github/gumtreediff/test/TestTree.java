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

import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;

public class TestTree {

    @Test
    public void testIdComparator() {
        ITree root = TreeLoader.getDummySrc();
        List<ITree> nodes = root.getTrees();
        assertTrue(nodes.get(0).getLabel().equals("a"));
        assertTrue(nodes.get(1).getLabel().equals("b"));
        assertTrue(nodes.get(2).getLabel().equals("c"));
        assertTrue(nodes.get(3).getLabel().equals("d"));
        assertTrue(nodes.get(4).getLabel().equals("e"));
    }

    @Test
    public void testGetParents() {
        ITree tree = TreeLoader.getDummySrc();
        List<ITree> trees = new LinkedList<>(tree.getTrees());
        ITree n = trees.get(2);
        assertTrue(n.getLabel().equals("c"));
        List<ITree> parents = n.getParents();
        assertTrue(parents.size() == 2);
        assertTrue(parents.get(0).getLabel().equals("b"));
        assertTrue(parents.get(1).getLabel().equals("a"));
    }

    @Test
    public void testDeepCopy() {
        ITree root = TreeLoader.getDummySrc();
        TreeUtils.postOrderNumbering(root);
        ITree croot = root.deepCopy();
        assertTrue(croot.getSize() == root.getSize());
        root.setLabel("new");
        root.getChildren().get(0).setLabel("new");
        root.getChildren().get(0).getChildren().get(0).setLabel("new");
        assertTrue(croot.getLabel().equals("a"));
        assertTrue(croot.getChildren().get(0).getLabel().equals("b"));
        assertTrue(croot.getChildren().get(0).getChildren().get(0).getLabel().equals("c"));
        assertTrue(root.getLabel().equals("new"));
        assertTrue(root.getChildren().get(0).getLabel().equals("new"));
        assertTrue(root.getChildren().get(0).getChildren().get(0).getLabel().equals("new"));
    }

    @Test
    public void testIsClone() {
        ITree tree = TreeLoader.getDummySrc();
        ITree copy = tree.deepCopy();
        assertTrue(tree.isIsomorphicTo(copy));
    }

}
