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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;
import java.util.List;

import com.github.gumtreediff.tree.*;
import org.junit.jupiter.api.Test;

import static com.github.gumtreediff.tree.TreeMetricsProvider.Factory.BASE;

public class TestTreeUtils {

    @Test
    public void testPostOrderNumbering() {
        ITree root = TreeLoader.getDummySrc();
        TreeMetricsProvider m = MetricProviderFactory.computeTreeMetrics(root);
        assertEquals(4, m.get(root).position);
        assertEquals(2, m.get(root.getChild(0)).position);
        assertEquals(0, m.get(root.getChild(0).getChild(0)).position);
        assertEquals(1, m.get(root.getChild(0).getChild(1)).position);
        assertEquals(3, m.get(root.getChild(1)).position);
    }

    @Test
    public void testDepth2() {
        ITree root = TreeLoader.getDummySrc();
        System.out.println(root.toTreeString());
        TreeMetricsProvider m = MetricProviderFactory.computeTreeMetrics(root);
        assertEquals(0, m.get(root).depth);
        assertEquals(1, m.get(root.getChild(0)).depth);
        assertEquals(2, m.get(root.getChild(0).getChild(0)).depth);
        assertEquals(2, m.get(root.getChild(0).getChild(1)).depth);
        assertEquals(1, m.get(root.getChild(1)).depth);
    }

    @Test
    public void testSize2() {
        ITree root = TreeLoader.getDummySrc();
        System.out.println(root.toTreeString());
        TreeMetricsProvider m = MetricProviderFactory.computeTreeMetrics(root);
        assertEquals(5, m.get(root).size);
        assertEquals(3, m.get(root.getChild(0)).size);
        assertEquals(1, m.get(root.getChild(0).getChild(0)).size);
        assertEquals(1, m.get(root.getChild(0).getChild(1)).size);
        assertEquals(1, m.get(root.getChild(1)).size);
    }

    public static final int H_AO = 96746278;
    public static final int H_AL = 102925061;

    public static final int H_BO = 96747270;
    public static final int H_BL = 102926053;

    public static final int H_CO = 96749223;
    public static final int H_CL = 102928006;

    public static final int H_DO = 96749254;
    public static final int H_DL = 102928037;

    public static final int H_EO = 96748324;
    public static final int H_EL = 102927107;

    @Test
    public void testHash2() {
        ITree root = TreeLoader.getDummySrc();
        System.out.println(root.toTreeString());
        TreeMetricsProvider m = MetricProviderFactory.computeTreeMetrics(root);
        assertEquals(
                H_AO
                        + BASE * H_BO
                        + BASE * BASE * H_CO
                        + BASE * BASE * BASE * H_CL
                        + BASE * BASE * BASE * BASE * H_DO
                        + BASE * BASE * BASE * BASE * BASE * H_DL
                        + BASE * BASE * BASE * BASE * BASE * BASE * H_BL
                        + BASE * BASE * BASE * BASE * BASE * BASE * BASE * H_EO
                        + BASE * BASE * BASE * BASE * BASE * BASE * BASE * BASE * H_EL
                        + BASE * BASE * BASE * BASE * BASE * BASE * BASE * BASE * BASE * H_AL,
                m.get(root).hash);
        assertEquals(
                H_BO
                + BASE * H_CO
                + BASE * BASE * H_CL
                + BASE * BASE * BASE * H_DO
                + BASE * BASE * BASE * BASE * H_DL
                + BASE * BASE * BASE * BASE * BASE * H_BL,
                m.get(root.getChild(0)).hash);
        assertEquals(H_CO + BASE * H_CL, m.get(root.getChild(0).getChild(0)).hash);
        assertEquals(H_DO + BASE * H_DL, m.get(root.getChild(0).getChild(1)).hash);
        assertEquals(H_EO + BASE * H_EL, m.get(root.getChild(1)).hash);
    }

    @Test
    public void testHeight2() {
        ITree root = TreeLoader.getDummySrc();
        TreeMetricsProvider m = MetricProviderFactory.computeTreeMetrics(root);
        assertEquals(2, m.get(root).height); // depth of a
        assertEquals(1, m.get(root.getChild(0)).height); // depth of b
        assertEquals(0, m.get(root.getChild(0).getChild(0)).height); // depth of c
        assertEquals(0, m.get(root.getChild(0).getChild(1)).height); // depth of d
        assertEquals(0, m.get(root.getChild(1)).height); // depth of e
    }

    @Test
    public void testPostOrder() {
        ITree src = TreeLoader.getDummySrc();
        List<ITree> lst = TreeUtils.postOrder(src);
        Iterator<ITree> it = TreeUtils.postOrderIterator(src);
        compareListIterator(lst, it);
    }

    @Test
    public void testPostOrder2() {
        ITree dst = TreeLoader.getDummyDst();
        List<ITree> lst = TreeUtils.postOrder(dst);
        Iterator<ITree> it = TreeUtils.postOrderIterator(dst);
        compareListIterator(lst, it);
    }

    @Test
    public void testPostOrder3() {
        ITree big = TreeLoader.getDummyBig();
        List<ITree> lst = TreeUtils.postOrder(big);
        Iterator<ITree> it = TreeUtils.postOrderIterator(big);
        compareListIterator(lst, it);
    }

    @Test
    public void testBfs() {
        ITree src = TreeLoader.getDummySrc();
        List<ITree> lst = TreeUtils.breadthFirst(src);
        Iterator<ITree> it = TreeUtils.breadthFirstIterator(src);
        compareListIterator(lst, it);
    }

    @Test
    public void testBfsList() {
        ITree src = TreeLoader.getDummySrc();
        ITree dst = TreeLoader.getDummyDst();
        ITree big = TreeLoader.getDummyBig();
        compareListIterator(TreeUtils.breadthFirstIterator(src), "a", "b", "e", "c", "d");
        compareListIterator(TreeUtils.breadthFirstIterator(dst), "a", "f", "i", "b", "j", "c", "d", "h");
        compareListIterator(TreeUtils.breadthFirstIterator(big), "a", "b", "e", "f", "c",
                "d", "g", "l", "h", "m", "i", "j", "k");
    }

    @Test
    public void testPreOrderList() {
        ITree src = TreeLoader.getDummySrc();
        ITree dst = TreeLoader.getDummyDst();
        ITree big = TreeLoader.getDummyBig();
        compareListIterator(TreeUtils.preOrderIterator(src), "a", "b", "c", "d", "e");
        compareListIterator(TreeUtils.preOrderIterator(dst), "a", "f", "b", "c", "d", "h", "i", "j");
        compareListIterator(TreeUtils.preOrderIterator(big), "a", "b", "c", "d", "e",
                "f", "g", "h", "i", "j", "k", "l", "m");
    }

    void compareListIterator(List<ITree> lst, Iterator<ITree> it) {
        for (ITree i: lst) {
            assertEquals(i, it.next());
        }
        assertFalse(it.hasNext());
    }

    void compareListIterator(Iterator<ITree> it, String... expected) {
        for (String e: expected) {
            ITree n = it.next();
            assertEquals(e, n.getLabel());
        }
        assertFalse(it.hasNext(),"Iterator has next");
    }

    @Test
    public void testBfs2() {
        ITree dst = TreeLoader.getDummyDst();
        List<ITree> lst = TreeUtils.breadthFirst(dst);
        Iterator<ITree> it = TreeUtils.breadthFirstIterator(dst);
        compareListIterator(lst, it);
    }

    @Test
    public void testBfs3() {
        ITree big = TreeLoader.getDummySrc();
        List<ITree> lst = TreeUtils.breadthFirst(big);
        Iterator<ITree> it = TreeUtils.breadthFirstIterator(big);
        compareListIterator(lst, it);
    }
    
    @Test
    public void testLeafIterator() {
        ITree src = TreeLoader.getDummySrc();
        Iterator<ITree> srcLeaves = TreeUtils.leafIterator(TreeUtils.postOrderIterator(src));
        ITree leaf = null;
        leaf = srcLeaves.next();
        leaf = srcLeaves.next();
        leaf = srcLeaves.next();
        leaf = srcLeaves.next();
        leaf = srcLeaves.next();
        assertNull(leaf);
    }

}
