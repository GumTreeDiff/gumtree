package fr.labri.gumtree.test;

import org.junit.Test;
import static org.junit.Assert.*;

import static fr.labri.gumtree.test.Constants.*;

import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.optimal.zs.*;
import fr.labri.gumtree.tree.ITree;

public class TestZsMatcher {
	
	@Test
	public void testZsMatcherWithDistinctTypes() {
		ITree src = TreeIoUtils.fromXml(getClass().getResourceAsStream(ZS_SRC)).getRoot();
		ITree dst = TreeIoUtils.fromXml(getClass().getResourceAsStream(ZS_DST)).getRoot();
		Matcher matcher = new ZsMatcher(src, dst);
		matcher.match();
		assertEquals(5, matcher.getMappingSet().size());
		assertTrue(matcher.getMappings().has(src, dst.getChild(0)));
		assertTrue(matcher.getMappings().has(src.getChild(0), dst.getChild(0).getChild(0)));
		assertTrue(matcher.getMappings().has(src.getChild(1), dst.getChild(0).getChild(1)));
		assertTrue(matcher.getMappings().has(src.getChild(1).getChild(0), dst.getChild(0).getChild(1).getChild(0)));
		assertTrue(matcher.getMappings().has(src.getChild(1).getChild(2), dst.getChild(0).getChild(1).getChild(2)));
	}

	@Test
	public void testZsMatcher() {
		ITree src = TreeIoUtils.fromXml(getClass().getResourceAsStream(ZS_SLIDE_SRC)).getRoot();
		ITree dst = TreeIoUtils.fromXml(getClass().getResourceAsStream(ZS_SLIDE_DST)).getRoot();
		Matcher matcher = new ZsMatcher(src, dst);
		matcher.match();
		assertEquals(5, matcher.getMappingSet().size());
		assertTrue(matcher.getMappings().has(src, dst));
		assertTrue(matcher.getMappings().has(src.getChild(0).getChild(0), dst.getChild(0)));
		assertTrue(matcher.getMappings().has(src.getChild(0).getChild(0).getChild(0), dst.getChild(0).getChild(0)));
		assertTrue(matcher.getMappings().has(src.getChild(0).getChild(1), dst.getChild(1).getChild(0)));
		assertTrue(matcher.getMappings().has(src.getChild(0).getChild(2), dst.getChild(2)));
	}
	
}
