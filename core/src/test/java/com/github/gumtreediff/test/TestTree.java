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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.gumtreediff.tree.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestTree {
    @Test
    public void testSearchSubtree() {
        Tree root = TreeLoader.getSubtreeSrc();
        Tree subtree = new DefaultTree(TypeSet.type("a"));
        subtree.addChild(new DefaultTree(TypeSet.type("b")));
        subtree.addChild(new DefaultTree(TypeSet.type("c"), "foo"));
        List<Tree> results = root.searchSubtree(subtree);
        assertEquals(2, results.size());
        assertTrue(results.get(0).isIsomorphicTo(subtree));

        results = root.searchSubtree(root);
        assertEquals(1, results.size());
        assertTrue(results.get(0).isIsomorphicTo(root));

        Tree otherSubtree = new DefaultTree(TypeSet.type("a"));
        otherSubtree.addChild(new DefaultTree(TypeSet.type("b")));
        results = root.searchSubtree(otherSubtree);
        assertEquals(0, results.size());
    }

    @Test
    public void testIsRoot() {
        Tree tree = new DefaultTree(TypeSet.type("a"));
        tree.addChild(new DefaultTree(TypeSet.type("b")));
        tree.addChild(new DefaultTree(TypeSet.type("c"), "foo"));
        assertTrue(tree.isRoot());
        assertFalse(tree.getChild(0).isRoot());
        assertFalse(tree.getChild(1).isRoot());
    }

    @Test
    public void testInsertChild() {
        Tree tree = new DefaultTree(TypeSet.type("a"));
        tree.addChild(new DefaultTree(TypeSet.type("b")));
        tree.addChild(new DefaultTree(TypeSet.type("c"), "foo"));
        System.out.println(tree.getChildren().size());
        tree.insertChild(new DefaultTree(TypeSet.type("m")), 1);
        assertEquals(TypeSet.type("m"), tree.getChild(1).getType());
        assertEquals(tree, tree.getChild(1).getParent());
    }

    @Test
    public void testTreesBetweenPositions() {
        Tree root = TreeLoader.getDummySrc();
        List<Tree> treesOutside = root.getTreesBetweenPositions(100, 200);
        assertEquals(0, treesOutside.size());
        List<Tree> allTrees = root.getTreesBetweenPositions(0, 100);
        assertEquals(5, allTrees.size());
        List<Tree> firstLeafTrees = root.getTreesBetweenPositions(0, 10);
        assertEquals(2, firstLeafTrees.size());
    }

    @Test
    public void testChildUrl() {
        Tree root = TreeLoader.getDummySrc();
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
        Tree tree = TreeLoader.getDummySrc();
        Tree c = tree.getChild("0.0");
        assertEquals("c", c.getLabel());
        List<Tree> parents = c.getParents();
        assertEquals(2, parents.size());
        assertEquals("b", parents.get(0).getLabel());
        assertEquals("a", parents.get(1).getLabel());
        assertEquals(tree, parents.get(parents.size() - 1));
    }

    @Test
    public void testGetDescendants() {
        Tree tree = TreeLoader.getDummySrc();
        Tree b = tree.getChild(0);
        assertEquals("b", b.getLabel());
        List<Tree> descendants = b.getDescendants();
        assertEquals(2, descendants.size());
        assertEquals("c", descendants.get(0).getLabel());
        assertEquals("d", descendants.get(1).getLabel());
    }

    @Test
    public void testChildManipulation() {
        Tree t1 = new DefaultTree(TypeSet.type("foo"));
        assertTrue(t1.isLeaf());
        assertTrue(t1.isRoot());
        assertEquals(-1, t1.positionInParent());
        assertEquals(0, t1.getChildren().size());
        Tree t2 = new DefaultTree(TypeSet.type("foo"));
        t1.addChild(t2);
        assertFalse(t1.isLeaf());
        assertTrue(t1.isRoot());
        assertTrue(t2.isLeaf());
        assertEquals(t1, t2.getParent());
        Tree t3 = new DefaultTree(TypeSet.type("foo"));
        t3.setParentAndUpdateChildren(t1);
        assertTrue(t3.isLeaf());
        assertEquals(t1, t3.getParent());
        assertEquals(2, t1.getChildren().size());
        assertEquals(t3, t1.getChild(1));
        assertEquals(1, t3.positionInParent());
        assertEquals(1, t1.getChildPosition(t3));
        Tree t4 = new DefaultTree(TypeSet.type("foo"));
        t4.setParent(t1);
        t4.setParentAndUpdateChildren(t2);
        assertNotEquals(t1, t4.getParent());
        assertEquals(t2, t4.getParent());
        assertEquals(-1, t1.getChildPosition(t4));
        assertEquals(0, t2.getChildPosition(t4));
        List<Tree> children = new ArrayList<>();
        children.add(t3);
        children.add(t4);
        t2.setChildren(children);
        assertEquals(2, t2.getChildren().size());
        assertTrue(t2.getChildren().contains(t3));
        assertTrue(t2.getChildren().contains(t4));
        assertEquals(t2, t3.getParent());
        assertEquals(t2, t4.getParent());
    }

    @Test
    public void testDeepCopy() {
        Tree root = TreeLoader.getDummySrc();
        Tree rootCpy = root.deepCopy();
        assertTrue(root.isIsomorphicTo(rootCpy));
        Iterator<Tree> rootIt = TreeUtils.preOrderIterator(root);
        for (Tree cpy : rootCpy.preOrder()) {
            Tree t = rootIt.next();
            assertNotEquals(t, cpy);
        }
        
        Tree rootWithFake = new DefaultTree(TypeSet.type("foo"));
        Tree fakeChild = new FakeTree();
        rootWithFake.addChild(fakeChild);
        Tree rootWithFakeCpy = rootWithFake.deepCopy();
        assertTrue(rootWithFakeCpy.isIsomorphicTo(rootWithFake));
        assertNotEquals(rootWithFake, rootWithFakeCpy);
        assertNotEquals(rootWithFake.getChild(0), rootWithFakeCpy.getChild(0));
    }

    @Test
    public void testIsomophism() {
        Tree root = TreeLoader.getDummySrc();
        Tree rootCpy = TreeLoader.getDummySrc();
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
        Tree root = TreeLoader.getDummySrc();
        Tree rootCpy = TreeLoader.getDummySrc();
        assertTrue(root.isIsoStructuralTo(rootCpy));
        rootCpy.getChild("0.0").setLabel("foo");
        assertTrue(root.isIsoStructuralTo(rootCpy));
        rootCpy.getChild("0.1").setType(TypeSet.type("foo"));
        assertFalse(root.isIsoStructuralTo(rootCpy));
        root.getChild("0.1").setType(TypeSet.type("foo"));
        assertTrue(root.isIsoStructuralTo(rootCpy));
        root.addChild(new FakeTree());
        assertFalse(root.isIsoStructuralTo(rootCpy));
        rootCpy.addChild(new FakeTree());
        assertTrue(root.isIsoStructuralTo(rootCpy));
    }

    @Test
    public void testIsClone() {
        Tree tree = TreeLoader.getDummySrc();
        Tree copy = tree.deepCopy();
        assertTrue(tree.isIsomorphicTo(copy));
    }

    @Test
    public void testImmutable() {
        Tree tree = TreeLoader.getDummySrc();
        tree.setMetadata("foo", "bar");
        Tree immutable = new ImmutableTree(tree);
        assertTrue(tree.isIsomorphicTo(immutable));
        assertEquals("bar", immutable.getMetadata("foo"));
        assertEquals(immutable, immutable.getChild(0).getParent());
        assertThrows(UnsupportedOperationException.class, () -> immutable.setLabel("foo"));
        assertThrows(UnsupportedOperationException.class, () -> immutable.setLength(12));
        assertThrows(UnsupportedOperationException.class, () -> immutable.setPos(12));
        assertThrows(UnsupportedOperationException.class,
                () -> immutable.setType(TypeSet.type("foo")));
        assertThrows(UnsupportedOperationException.class, () -> immutable.setMetadata("foo", null));
        assertThrows(UnsupportedOperationException.class, () -> immutable.getChildren().remove(0));
        assertThrows(UnsupportedOperationException.class, () -> immutable.getChild(0).setLabel("foo"));
        assertThrows(UnsupportedOperationException.class, () -> immutable.getChild(0).setParent(null));
        Tree immutableCpy = immutable.deepCopy();
        assertTrue(immutableCpy.isIsomorphicTo(immutable));
        assertDoesNotThrow(() -> immutableCpy.setLabel("foo"));
        assertDoesNotThrow(() -> immutableCpy.getChildren().remove(0));
        assertFalse(immutableCpy.isIsomorphicTo(immutable));
    }

    @Test
    public void testFakeTree() {
        Tree fake = new FakeTree();
        assertEquals(Tree.NO_LABEL, fake.getLabel());
        assertEquals(Type.NO_TYPE, fake.getType());
        assertThrows(UnsupportedOperationException.class, () -> fake.setLabel("foo"));
        assertThrows(UnsupportedOperationException.class, () -> fake.setPos(0));
        assertThrows(UnsupportedOperationException.class, () -> fake.setLength(0));
        assertThrows(UnsupportedOperationException.class, () -> fake.setMetadata("foo", "bar"));
        assertThrows(UnsupportedOperationException.class, () -> fake.setType(Type.NO_TYPE));
    }

    @Test
    public void testTypesAndLabels() {
        Type origType = TypeSet.type("anewtype");
        Type otherType = TypeSet.type("othernewtype");
        Type origCopyType = TypeSet.type("anewtype");
        assertNotNull(origType);
        assertSame(origType, origCopyType);
        assertNotEquals(origType, otherType);
        assertEquals("anewtype", origType.name);
        Tree t1 = new DefaultTree(TypeSet.type("foo"));
        Tree t2 = new DefaultTree(TypeSet.type("foo"));
        assertTrue(t1.hasSameType(t2));
        assertFalse(t1.getType().isEmpty());
        assertTrue(t1.hasSameTypeAndLabel(t2));
        Tree t3 = new DefaultTree(TypeSet.type("bar"));
        assertFalse(t1.hasSameType(t3));
        Tree t4 = new DefaultTree(TypeSet.type("foo"), "hello");
        assertTrue(t1.hasSameType(t4));
        assertFalse(t1.hasSameTypeAndLabel(t4));
        assertTrue(Type.NO_TYPE.isEmpty());
        assertTrue(TypeSet.type(null).isEmpty());
    }

    @Test
    public void testToString() {
        Tree t1 = new DefaultTree(TypeSet.type("foo"));
        assertEquals("foo [0,0]", t1.toString());
        Tree t2 = new DefaultTree(TypeSet.type("foo"), "hello");
        assertEquals("foo: hello [0,0]", t2.toString());
        Tree t3 = new DefaultTree(TypeSet.type("foo"), "hello");
        t3.setPos(1);
        t3.setLength(2);
        assertEquals("foo: hello [1,3]", t3.toString());
    }
}
