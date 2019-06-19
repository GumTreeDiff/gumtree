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

import java.util.Iterator;
import java.util.List;

import com.github.gumtreediff.tree.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestTree {
    @Test
    public void testChildUrl() {
        ITree root = TreeLoader.getDummySrc();
        assertEquals("b", root.getChild("0").getLabel());
        assertEquals("c", root.getChild("0.0").getLabel());
        assertEquals("d", root.getChild("0.1").getLabel());
        assertEquals("e", root.getChild("1").getLabel());
        assertThrows(IllegalArgumentException.class, () -> {
            root.getChild("toto");
        });
    }

    @Test
    public void testGetParents() {
        ITree tree = TreeLoader.getDummySrc();
        ITree c = tree.getChild("0.0");
        assertEquals("c", c.getLabel());
        List<ITree> parents = c.getParents();
        assertEquals(2, parents.size());
        assertEquals("b", parents.get(0).getLabel());
        assertEquals("a", parents.get(1).getLabel());
        assertEquals(tree, parents.get(parents.size() - 1));
    }

    @Test
    public void testGetDescendants() {
        ITree tree = TreeLoader.getDummySrc();
        ITree b = tree.getChild(0);
        assertEquals("b", b.getLabel());
        List<ITree> descendants = b.getDescendants();
        assertEquals(2, descendants.size());
        assertEquals("c", descendants.get(0).getLabel());
        assertEquals("d", descendants.get(1).getLabel());
    }

    @Test
    public void testChildManipulation() {
        ITree t1 = new Tree(TypeSet.type("foo"));
        assertTrue(t1.isLeaf());
        assertTrue(t1.isRoot());
        assertEquals(-1, t1.positionInParent());
        assertEquals(0, t1.getChildren().size());
        ITree t2 = new Tree(TypeSet.type("foo"));
        t1.addChild(t2);
        assertFalse(t1.isLeaf());
        assertTrue(t1.isRoot());
        assertTrue(t2.isLeaf());
        assertEquals(t1, t2.getParent());
        ITree t3 = new Tree(TypeSet.type("foo"));
        t3.setParentAndUpdateChildren(t1);
        assertTrue(t3.isLeaf());
        assertEquals(t1, t3.getParent());
        assertEquals(2, t1.getChildren().size());
        assertEquals(t3, t1.getChild(1));
        assertEquals(1, t3.positionInParent());
        assertEquals(1, t1.getChildPosition(t3));
        ITree t4 = new Tree(TypeSet.type("foo"));
        assertEquals(-1, t1.getChildPosition(t4));
        t4.setParentAndUpdateChildren(t2);
        assertNotEquals(t1, t4.getParent());
        assertEquals(t2, t4.getParent());
        assertEquals(-1, t1.getChildPosition(t4));
        assertEquals(0, t2.getChildPosition(t4));
    }

    @Test
    public void testDeepCopy() {
        ITree root = TreeLoader.getDummySrc();
        ITree rootCpy = root.deepCopy();
        assertTrue(root.isIsomorphicTo(rootCpy));
        Iterator<ITree> rootIt = TreeUtils.preOrderIterator(root);
        for (ITree cpy : rootCpy.preOrder()) {
            ITree t = rootIt.next();
            assertNotEquals(t, cpy);
        }
        
        ITree rootWithFake = new Tree(TypeSet.type("foo"));
        ITree fakeChild = new FakeTree();
        rootWithFake.addChild(fakeChild);
        ITree rootWithFakeCpy = rootWithFake.deepCopy();
        assertTrue(rootWithFakeCpy.isIsomorphicTo(rootWithFake));
        assertNotEquals(rootWithFake, rootWithFakeCpy);
        assertNotEquals(rootWithFake.getChild(0), rootWithFakeCpy.getChild(0));
    }

    @Test
    public void testIsomophism() {
        ITree root = TreeLoader.getDummySrc();
        ITree rootCpy = TreeLoader.getDummySrc();
        assertTrue(root.isIsomorphicTo(rootCpy));
        rootCpy.getChild("0.0").setLabel("foo");
        assertFalse(root.isIsomorphicTo(rootCpy));
        root.getChild("0.0").setLabel("foo");
        assertTrue(root.isIsomorphicTo(rootCpy));
        root.addChild(new FakeTree());
        assertFalse(root.isIsomorphicTo(rootCpy));
        rootCpy.addChild(new FakeTree());
        assertTrue(root.isIsomorphicTo(rootCpy));
    }

    @Test
    public void testIsostructure() {
        ITree root = TreeLoader.getDummySrc();
        ITree rootCpy = TreeLoader.getDummySrc();
        assertTrue(root.isIsoStructuralTo(rootCpy));
        rootCpy.getChild("0.0").setLabel("foo");
        assertTrue(root.isIsoStructuralTo(rootCpy));
        root.getChild("0.0").setLabel("foo");
        assertTrue(root.isIsoStructuralTo(rootCpy));
        root.addChild(new FakeTree());
        assertFalse(root.isIsoStructuralTo(rootCpy));
        rootCpy.addChild(new FakeTree());
        assertTrue(root.isIsoStructuralTo(rootCpy));
    }

    @Test
    public void testIsClone() {
        ITree tree = TreeLoader.getDummySrc();
        ITree copy = tree.deepCopy();
        assertTrue(tree.isIsomorphicTo(copy));
    }

    @Test
    public void testImmutable() {
        ITree tree = TreeLoader.getDummySrc();
        ITree immutable = new ImmutableTree(tree);
        assertTrue(tree.isIsomorphicTo(immutable));
        assertEquals(immutable, immutable.getChild(0).getParent());
        assertThrows(UnsupportedOperationException.class, () -> immutable.setLabel("foo"));
        assertThrows(UnsupportedOperationException.class, () -> immutable.setLength(12));
        assertThrows(UnsupportedOperationException.class, () -> immutable.setPos(12));
        assertThrows(UnsupportedOperationException.class,
                () -> immutable.setType(TypeSet.type("foo")));
        assertThrows(UnsupportedOperationException.class, () -> immutable.setMetadata("foo", null));
        assertThrows(UnsupportedOperationException.class, () -> immutable.getChildren().remove(0));
        assertThrows(UnsupportedOperationException.class, () -> immutable.getChild(0).setLabel("foo"));
        ITree immutableCpy = immutable.deepCopy();
        assertTrue(immutableCpy.isIsomorphicTo(immutable));
        assertDoesNotThrow(() -> immutableCpy.setLabel("foo"));
        assertDoesNotThrow(() -> immutableCpy.getChildren().remove(0));
        assertFalse(immutableCpy.isIsomorphicTo(immutable));
    }

    @Test
    public void testTypesAndLabels() {
        ITree t1 = new Tree(TypeSet.type("foo"));
        ITree t2 = new Tree(TypeSet.type("foo"));
        assertTrue(t1.hasSameType(t2));
        assertTrue(t1.hasSameTypeAndLabel(t2));
        ITree t3 = new Tree(TypeSet.type("bar"));
        assertFalse(t1.hasSameType(t3));
        ITree t4 = new Tree(TypeSet.type("foo"), "hello");
        assertTrue(t1.hasSameType(t4));
        assertFalse(t1.hasSameTypeAndLabel(t4));
    }

    @Test
    public void testToString() {
        ITree t1 = new Tree(TypeSet.type("foo"));
        assertEquals("foo [0,0]", t1.toString());
        ITree t2 = new Tree(TypeSet.type("foo"), "hello");
        assertEquals("foo: hello [0,0]", t2.toString());
        ITree t3 = new Tree(TypeSet.type("foo"), "hello");
        t3.setPos(1);
        t3.setLength(2);
        assertEquals("foo: hello [1,3]", t3.toString());
    }
}
