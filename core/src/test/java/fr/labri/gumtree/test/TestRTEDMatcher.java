package fr.labri.gumtree.test;

import static fr.labri.gumtree.test.Constants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.optimal.rted.RtedMatcher;
import fr.labri.gumtree.tree.ITree;

public class TestRTEDMatcher {
	
	@Test
	public void testRtedMatcher() {
		ITree src = TreeIoUtils.fromXml(getClass().getResourceAsStream(ZS_SLIDE_SRC)).getRoot();
		ITree dst = TreeIoUtils.fromXml(getClass().getResourceAsStream(ZS_SLIDE_DST)).getRoot();
		Matcher matcher = new RtedMatcher(src, dst);
		matcher.match();
		assertEquals(5, matcher.getMappingSet().size());
		assertTrue(matcher.getMappings().has(src, dst));
		assertTrue(matcher.getMappings().has(src.getChild(0).getChild(0), dst.getChild(0)));
		assertTrue(matcher.getMappings().has(src.getChild(0).getChild(0).getChild(0), dst.getChild(0).getChild(0)));
		assertTrue(matcher.getMappings().has(src.getChild(0).getChild(1), dst.getChild(1).getChild(0)));
		assertTrue(matcher.getMappings().has(src.getChild(0).getChild(2), dst.getChild(2)));
	}

}
