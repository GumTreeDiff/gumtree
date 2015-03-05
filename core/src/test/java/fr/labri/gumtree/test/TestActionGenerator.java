package fr.labri.gumtree.test;

import java.util.List;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import static fr.labri.gumtree.test.Constants.*;
import fr.labri.gumtree.actions.ActionGenerator;
import fr.labri.gumtree.actions.model.Action;
import fr.labri.gumtree.actions.model.Delete;
import fr.labri.gumtree.actions.model.Insert;
import fr.labri.gumtree.actions.model.Move;
import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.matchers.MappingStore;
import fr.labri.gumtree.tree.ITree;

public class TestActionGenerator {
	ITree src, dst;

	@Before // FIXME Could it be before class ?
	public void init() {
		src = TreeIoUtils.fromXml(getClass().getResourceAsStream(ACTION_SRC)).getRoot();
		dst = TreeIoUtils.fromXml(getClass().getResourceAsStream(ACTION_DST)).getRoot();
	}
	
	@Test
	public void testActions() {
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
		assertEquals(3,  actions.size());
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
		assertTrue(a3 instanceof Delete);
		assertEquals("0@@g", a3.getNode().toShortString());
	}
}
