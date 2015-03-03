package fr.labri.gumtree.test;

import static fr.labri.gumtree.test.Constants.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.optimal.zs.*;
import fr.labri.gumtree.tree.ITree;

public class TestZsNewMatcher {
	
	ITree src, dst;

	@Before // FIXME Could it be before class ?
	public void init() {
		src = TreeIoUtils.fromXml(getClass().getResourceAsStream(ZS_SRC)).getRoot();
		dst = TreeIoUtils.fromXml(getClass().getResourceAsStream(ZS_DST)).getRoot();
	}
	
	@Test
	public void testActions() {
		Matcher matcher = new ZsNewMatcher(src, dst);
		matcher.match();
		Assert.assertEquals(5, matcher.getMappingSet().size());
	}

}
