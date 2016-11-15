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

        ActionGenerator ag = new ActionGenerator(src, dst, ms);
        ag.generate();
        List<Action> actions = ag.getActions();

        assertEquals(4,  actions.size());
        Action a1 = actions.get(0);
        assertTrue(a1 instanceof Insert);
        Insert i = (Insert) a1;
        assertEquals("1@@h", i.getNode().toShortString());
        assertEquals("0@@a", i.getParent().toShortString());
        assertEquals(2, i.getPosition());
        Action a2 = actions.get(1);
        assertTrue(a2 instanceof Move);
        Move m = (Move) a2;
        assertEquals("0@@e", m.getNode().toShortString());
        assertEquals("1@@h", m.getParent().toShortString());
        assertEquals(0, m.getPosition());
        Action a3 = actions.get(2);
        assertTrue(a3 instanceof Update);
        Update u = (Update) a3;
        assertEquals("0@@f", u.getNode().toShortString());
        assertEquals("y", u.getValue());
        Action a4 = actions.get(3);
        assertTrue(a4 instanceof Delete);
        assertEquals("0@@g", a4.getNode().toShortString());
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
        System.out.println(actions);
    }

}
