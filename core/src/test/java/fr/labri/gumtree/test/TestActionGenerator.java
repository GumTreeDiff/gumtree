package fr.labri.gumtree.test;

import static fr.labri.gumtree.test.Constants.*;

import org.junit.Before;
import org.junit.Test;

import fr.labri.gumtree.actions.ActionGenerator;
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
		
		System.out.println(ag.getActions());
	}
}
