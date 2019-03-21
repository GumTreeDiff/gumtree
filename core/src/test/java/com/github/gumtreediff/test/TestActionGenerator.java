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

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.io.ActionsIoUtils;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestActionGenerator {

    @Test
    public void testWithActionExample() {
        Pair<TreeContext, TreeContext> trees = TreeLoader.getActionPair();
        ITree src = trees.getFirst().getRoot();
        ITree dst = trees.getSecond().getRoot();
        MappingStore ms = new MappingStore();
        ms.link(src, dst);
        ms.link(src.getChild(1), dst.getChild(0));
        ms.link(src.getChild(1).getChild(0), dst.getChild(0).getChild(0));
        ms.link(src.getChild(1).getChild(1), dst.getChild(0).getChild(1));
        ms.link(src.getChild(0), dst.getChild(1).getChild(0));
        ms.link(src.getChild(0).getChild(0), dst.getChild(1).getChild(0).getChild(0));
        ms.link(src.getChild(4), dst.getChild(3));
        ms.link(src.getChild(4).getChild(0), dst.getChild(3).getChild(0).getChild(0).getChild(0));

        ActionGenerator ag = new ActionGenerator(src, dst, ms);
        ag.generate();
        List<Action> actions = ag.getActions();
        assertEquals(9,  actions.size());

        Action a = actions.get(0);
        assertTrue(a instanceof Insert);
        Insert i = (Insert) a;
        assertEquals("0@@h", i.getNode().toShortString());
        assertEquals("0@@a", i.getParent().toShortString());
        assertEquals(2, i.getPosition());

        a = actions.get(1);
        assertTrue(a instanceof TreeInsert);
        TreeInsert ti = (TreeInsert) a;
        assertEquals("0@@x", ti.getNode().toShortString());
        assertEquals("0@@a", ti.getParent().toShortString());
        assertEquals(3, ti.getPosition());

        a = actions.get(2);
        assertTrue(a instanceof Move);
        Move m = (Move) a;
        assertEquals("0@@e", m.getNode().toShortString());
        assertEquals("0@@h", m.getParent().toShortString());
        assertEquals(0, m.getPosition());

        a = actions.get(3);
        assertTrue(a instanceof Insert);
        Insert i2 = (Insert) a;
        assertEquals("0@@u", i2.getNode().toShortString());
        assertEquals("0@@j", i2.getParent().toShortString());
        assertEquals(0, i2.getPosition());

        a = actions.get(4);
        assertTrue(a instanceof Update);
        Update u = (Update) a;
        assertEquals("0@@f", u.getNode().toShortString());
        assertEquals("y", u.getValue());

        a = actions.get(5);
        assertTrue(a instanceof Insert);
        Insert i3 = (Insert) a;
        assertEquals("0@@v", i3.getNode().toShortString());
        assertEquals("0@@u", i3.getParent().toShortString());
        assertEquals(0, i3.getPosition());

        a = actions.get(6);
        assertTrue(a instanceof Move);
        Move m2 = (Move) a;
        assertEquals("0@@k", m2.getNode().toShortString());
        assertEquals("0@@v", m2.getParent().toShortString());
        assertEquals(0, m.getPosition());

        a = actions.get(7);
        assertTrue(a instanceof TreeDelete);
        TreeDelete td = (TreeDelete) a;
        assertEquals("0@@g", td.getNode().toShortString());

        a = actions.get(8);
        assertTrue(a instanceof Delete);
        Delete d = (Delete) a;
        assertEquals("0@@i", d.getNode().toShortString());
    }

    @Test
    public void testWithActionExampleNoMove() {
        ActionGenerator.REMOVE_MOVES_AND_UPDATES = true;
        Pair<TreeContext, TreeContext> trees = TreeLoader.getActionPair();
        ITree src = trees.getFirst().getRoot();
        ITree dst = trees.getSecond().getRoot();
        MappingStore ms = new MappingStore();
        ms.link(src, dst);
        ms.link(src.getChild(1), dst.getChild(0));
        ms.link(src.getChild(1).getChild(0), dst.getChild(0).getChild(0));
        ms.link(src.getChild(1).getChild(1), dst.getChild(0).getChild(1));
        ms.link(src.getChild(0), dst.getChild(1).getChild(0));
        ms.link(src.getChild(0).getChild(0), dst.getChild(1).getChild(0).getChild(0));
        ms.link(src.getChild(4), dst.getChild(3));
        ms.link(src.getChild(4).getChild(0), dst.getChild(3).getChild(0).getChild(0).getChild(0));

        ActionGenerator ag = new ActionGenerator(src, dst, ms);
        ag.generate();

        for (Action a: ag.getActions())
            System.out.println(a.format(trees.getFirst()));

        List<Action> actions = ag.getActions();
    }

    @Test
    public void testWithZsCustomExample() {
        Pair<TreeContext, TreeContext> trees = TreeLoader.getZsCustomPair();
        ITree src = trees.getFirst().getRoot();
        ITree dst = trees.getSecond().getRoot();
        MappingStore ms = new MappingStore();
        ms.link(src, dst.getChild(0));
        ms.link(src.getChild(0), dst.getChild(0).getChild(0));
        ms.link(src.getChild(1), dst.getChild(0).getChild(1));
        ms.link(src.getChild(1).getChild(0), dst.getChild(0).getChild(1).getChild(0));
        ms.link(src.getChild(1).getChild(2), dst.getChild(0).getChild(1).getChild(2));

        ActionGenerator ag = new ActionGenerator(src, dst, ms);
        ag.generate();
        List<Action> actions = ag.getActions();
    }

}
