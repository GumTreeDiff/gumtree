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

import com.github.gumtreediff.actions.*;
import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TypeSet;
import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestActionGenerator {
    @Test
    public void testWithActionExample() {
        Pair<TreeContext, TreeContext> trees = TreeLoader.getActionPair();
        Tree src = trees.first.getRoot();
        Tree dst = trees.second.getRoot();
        MappingStore ms = new MappingStore(src, dst);
        ms.addMapping(src, dst);
        ms.addMapping(src.getChild(1), dst.getChild(0));
        ms.addMapping(src.getChild("1.0"), dst.getChild("0.0"));
        ms.addMapping(src.getChild("1.1"), dst.getChild("0.1"));
        ms.addMapping(src.getChild(0), dst.getChild(1).getChild(0));
        ms.addMapping(src.getChild("0.0"), dst.getChild("1.0.0"));
        ms.addMapping(src.getChild(4), dst.getChild(3));
        ms.addMapping(src.getChild("4.0"), dst.getChild("3.0.0.0"));
        EditScript actions = new SimplifiedChawatheScriptGenerator().computeActions(ms);
        assertEquals(9, actions.size());

        Action a = actions.get(0);
        assertTrue(a instanceof Insert);
        Insert i = (Insert) a;
        assertEquals("h", i.getNode().getLabel());
        assertEquals("a", i.getParent().getLabel());
        assertEquals(2, i.getPosition());

        a = actions.get(1);
        assertTrue(a instanceof TreeInsert);
        TreeInsert ti = (TreeInsert) a;
        assertEquals("x", ti.getNode().getLabel());
        assertEquals("a", ti.getParent().getLabel());
        assertEquals(3, ti.getPosition());

        a = actions.get(2);
        assertTrue(a instanceof Move);
        Move m = (Move) a;
        assertEquals("e", m.getNode().getLabel());
        assertEquals("h", m.getParent().getLabel());
        assertEquals(0, m.getPosition());

        a = actions.get(3);
        assertTrue(a instanceof Insert);
        Insert i2 = (Insert) a;
        assertEquals("u", i2.getNode().getLabel());
        assertEquals("j", i2.getParent().getLabel());
        assertEquals(0, i2.getPosition());

        a = actions.get(4);
        assertTrue(a instanceof Update);
        Update u = (Update) a;
        assertEquals("f", u.getNode().getLabel());
        assertEquals("y", u.getValue());

        a = actions.get(5);
        assertTrue(a instanceof Insert);
        Insert i3 = (Insert) a;
        assertEquals("v", i3.getNode().getLabel());
        assertEquals("u", i3.getParent().getLabel());
        assertEquals(0, i3.getPosition());

        a = actions.get(6);
        assertTrue(a instanceof Move);
        Move m2 = (Move) a;
        assertEquals("k", m2.getNode().getLabel());
        assertEquals("v", m2.getParent().getLabel());
        assertEquals(0, m.getPosition());

        a = actions.get(7);
        assertTrue(a instanceof TreeDelete);
        TreeDelete td = (TreeDelete) a;
        assertEquals("g", td.getNode().getLabel());

        a = actions.get(8);
        assertTrue(a instanceof Delete);
        Delete d = (Delete) a;
        assertEquals("i", d.getNode().getLabel());
    }

    @Test
    public void testWithUnmappedRoot() {
        Tree src = new DefaultTree(TypeSet.type("foo"), "");
        Tree dst = new DefaultTree(TypeSet.type("bar"), "");
        MappingStore ms = new MappingStore(src, dst);
        EditScript actions = new SimplifiedChawatheScriptGenerator().computeActions(ms);
        assertEquals(2, actions.size());
    }

    @Test
    public void testWithActionExampleNoMove() {
        Pair<TreeContext, TreeContext> trees = TreeLoader.getActionPair();
        Tree src = trees.first.getRoot();
        Tree dst = trees.second.getRoot();
        MappingStore ms = new MappingStore(src, dst);
        ms.addMapping(src, dst);
        ms.addMapping(src.getChild(1), dst.getChild(0));
        ms.addMapping(src.getChild(1).getChild(0), dst.getChild(0).getChild(0));
        ms.addMapping(src.getChild(1).getChild(1), dst.getChild(0).getChild(1));
        ms.addMapping(src.getChild(0), dst.getChild(1).getChild(0));
        ms.addMapping(src.getChild(0).getChild(0), dst.getChild(1).getChild(0).getChild(0));
        ms.addMapping(src.getChild(4), dst.getChild(3));
        ms.addMapping(src.getChild(4).getChild(0), dst.getChild(3).getChild(0).getChild(0).getChild(0));

        EditScript actions = new InsertDeleteChawatheScriptGenerator().computeActions(ms);

        assertEquals(12, actions.size());
    }

    @Test
    public void testWithZsCustomExample() {
        Pair<TreeContext, TreeContext> trees = TreeLoader.getZsCustomPair();
        Tree src = trees.first.getRoot();
        Tree dst = trees.second.getRoot();
        MappingStore ms = new MappingStore(src, dst);
        ms.addMapping(src, dst.getChild(0));
        ms.addMapping(src.getChild(0), dst.getChild("0.0"));
        ms.addMapping(src.getChild(1), dst.getChild("0.1"));
        ms.addMapping(src.getChild("1.0"), dst.getChild("0.1.0"));
        ms.addMapping(src.getChild("1.2"), dst.getChild("0.1.2"));
        ms.addMapping(src.getChild("1.3"), dst.getChild("0.1.3"));

        EditScript actions = new ChawatheScriptGenerator().computeActions(ms);
        assertEquals(5, actions.size());
        assertThat(actions, hasItems(
                new Insert(dst, null, 0),
                new Move(src, dst, 0),
                new Insert(dst.getChild("0.1.1"), src.getChild("1"), 1),
                new Update(src.getChild("1.3"), "r2"),
                new Delete(src.getChild("1.1"))
        ));

        actions = new SimplifiedChawatheScriptGenerator().computeActions(ms);
        assertEquals(5, actions.size());
        assertThat(actions, hasItems(
                new Insert(dst, null, 0),
                new Move(src, dst, 0),
                new Insert(dst.getChild("0.1.1"), src.getChild("1"), 1),
                new Update(src.getChild("1.3"), "r2"),
                new Delete(src.getChild("1.1"))
        ));



        actions = new InsertDeleteChawatheScriptGenerator().computeActions(ms);
        assertEquals(7, actions.size());
        assertThat(actions, hasItems(
                new Insert(dst, null, 0),
                new TreeDelete(src),
                new TreeInsert(dst.getChild(0), dst, 0),
                new Insert(dst.getChild("0.1.1"), src.getChild("1"), 1),
                new Delete(src.getChild("1.1")),
                new Delete(src.getChild("1.3")),
                new Insert(dst.getChild("0.1.1"), src.getChild(1), 1)
        ));
    }

    @Test
    void testAlignChildren() {
        Tree t1 = new DefaultTree(TypeSet.type("root"));
        Tree a1 = new DefaultTree(TypeSet.type("a"));
        t1.addChild(a1);
        Tree b1 = new DefaultTree(TypeSet.type("b"));
        t1.addChild(b1);
        // root [0,0]
        //     a [0,0]
        //     b [0,0]

        Tree t2 = new DefaultTree(TypeSet.type("root"));
        Tree b2 = new DefaultTree(TypeSet.type("b"));
        t2.addChild(b2);
        Tree a2 = new DefaultTree(TypeSet.type("a"));
        t2.addChild(a2);
        // root [0,0]
        //     b [0,0]
        //     a [0,0]

        MappingStore mp = new MappingStore(t1, t2);
        mp.addMapping(t1, t2);
        mp.addMapping(a1, a2);
        mp.addMapping(b1, b2);

        EditScript actions =  new ChawatheScriptGenerator().computeActions(mp);

        assertEquals(1, actions.size());
    }
}
