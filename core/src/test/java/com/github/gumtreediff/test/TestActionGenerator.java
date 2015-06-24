package com.github.gumtreediff.test;

import java.util.List;

import static org.junit.Assert.*;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Pair;
import org.junit.Test;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Pair;

public class TestActionGenerator {

    @Test
    public void testWithActionExample() {
        Pair<ITree, ITree> trees = TreeLoader.getActionPair();
        ITree src = trees.getFirst();
        ITree dst = trees.getSecond();
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
        Pair<ITree, ITree> trees = TreeLoader.getZsCustomPair();
        ITree src = trees.getFirst();
        ITree dst = trees.getSecond();
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
