package fr.labri.gumtree.test;

import static fr.labri.gumtree.test.Constants.ACTION_DST;
import static fr.labri.gumtree.test.Constants.ACTION_SRC;

import org.junit.Before;
import org.junit.Test;

import fr.labri.gumtree.actions.ActionGenerator;
import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.matchers.Mapping;
import fr.labri.gumtree.matchers.MappingStore;
import fr.labri.gumtree.matchers.optimal.zs.ZsMatcher;
import fr.labri.gumtree.tree.ITree;

public class TestZsMatcher {
	
	ITree src, dst;

	@Before // FIXME Could it be before class ?
	public void init() {
		src = TreeIoUtils.fromXml(getClass().getResourceAsStream(ACTION_SRC)).getRoot();
		dst = TreeIoUtils.fromXml(getClass().getResourceAsStream(ACTION_DST)).getRoot();
	}
	
	@Test
	public void testActions() {
		ZsMatcher matcher = new ZsMatcher(src, dst);
		matcher.match();
		for (Mapping m: matcher.getMappingSet()) 
			System.out.println(m);
	}

}
