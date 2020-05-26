package com.github.gumtreediff.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.gumtreediff.matchers.GumTreeProperties;
import com.github.gumtreediff.matchers.heuristic.XyBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.cd.ChangeDistillerBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.cd.ChangeDistillerLeavesMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.AbstractBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.AbstractSubtreeMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.CompleteBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.GreedySubtreeMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.SimpleBottomUpMatcher;

class TestGumTreeProperties {

	@BeforeEach
	public void setup() {
		GumTreeProperties.reset();
	}

	@Test
	void testDefaultValue() {
		assertNotNull(GumTreeProperties.getProperty("gt.xym.sim"));
		assertEquals("0.5", GumTreeProperties.getProperty("gt.xym.sim"));
		assertEquals(0.5, GumTreeProperties.getPropertyDouble("gt.xym.sim"), 0);
	}

	@Test
	void testBottomUpMatcher() {

		assertNotNull(GumTreeProperties.getProperty("gt.xym.sim"));
		assertEquals("0.5", GumTreeProperties.getProperty("gt.xym.sim"));

		XyBottomUpMatcher xm = new XyBottomUpMatcher();
		assertEquals(GumTreeProperties.getPropertyDouble("gt.xym.sim"), xm.SIM_THRESHOLD, 0);

		Double newth = 0.99;
		GumTreeProperties.setProperty("gt.xym.sim", newth.toString());
		xm.configure();
		assertEquals(newth, xm.SIM_THRESHOLD, 0);
	}

	@Test
	void ChangeDistillerBottomUpMatcher() {

		assertNotNull(GumTreeProperties.getProperty("gt.cd.ssim1"));
		assertEquals("0.6", GumTreeProperties.getProperty("gt.cd.ssim1"));

		assertNotNull(GumTreeProperties.getProperty("gt.cd.ssim1"));
		assertEquals("0.4", GumTreeProperties.getProperty("gt.cd.ssim2"));

		ChangeDistillerBottomUpMatcher xm = new ChangeDistillerBottomUpMatcher();
		assertEquals(GumTreeProperties.getPropertyDouble("gt.cd.ssim1"), xm.STRUCT_SIM_THRESHOLD_1, 0);
		assertEquals(GumTreeProperties.getPropertyDouble("gt.cd.ssim2"), xm.STRUCT_SIM_THRESHOLD_2, 0);

		Double newth = 0.99;
		GumTreeProperties.setProperty("gt.cd.ssim1", newth.toString());
		GumTreeProperties.setProperty("gt.cd.ssim2", newth.toString());
		xm.configure();
		assertEquals(newth, xm.STRUCT_SIM_THRESHOLD_1, 0);
		assertEquals(newth, xm.STRUCT_SIM_THRESHOLD_2, 0);

		assertNotNull(GumTreeProperties.getProperty("gt.cd.ml"));
		assertEquals("4", GumTreeProperties.getProperty("gt.cd.ml"));

		assertEquals(GumTreeProperties.getPropertyInteger("gt.cd.ml"), xm.MAX_NUMBER_OF_LEAVES);

		final Integer nl = 10;
		GumTreeProperties.setProperty("gt.cd.ml", nl.toString());
		xm.configure();
		assertEquals(nl, xm.MAX_NUMBER_OF_LEAVES);

	}

	@Test
	void ChangeDistillerLeavesMatcher() {

		assertNotNull(GumTreeProperties.getProperty("gt.cd.lsim"));
		assertEquals("0.5", GumTreeProperties.getProperty("gt.cd.lsim"));

		ChangeDistillerLeavesMatcher xm = new ChangeDistillerLeavesMatcher();
		assertEquals(GumTreeProperties.getPropertyDouble("gt.cd.lsim"), xm.LABEL_SIM_THRESHOLD, 0);

		final Double newth = 0.99;
		GumTreeProperties.setProperty("gt.cd.lsim", newth.toString());
		xm.configure();
		assertEquals(newth, xm.LABEL_SIM_THRESHOLD, 0);
	}

	@Test
	void AbstractBottomUpMatcher() {

		assertNotNull(GumTreeProperties.getProperty("gt.bum.smt"));
		assertEquals("0.5", GumTreeProperties.getProperty("gt.bum.smt"));

		AbstractBottomUpMatcher xm = new CompleteBottomUpMatcher();
		assertEquals(GumTreeProperties.getPropertyDouble("gt.bum.smt"), xm.SIM_THRESHOLD, 0);

		final Double newth = 0.99;
		GumTreeProperties.setProperty("gt.bum.smt", newth.toString());
		xm.configure();
		assertEquals(newth, xm.SIM_THRESHOLD, 0);

		assertNotNull(GumTreeProperties.getProperty("gt.bum.szt"));
		assertEquals("1000", GumTreeProperties.getProperty("gt.bum.szt"));

		assertEquals(GumTreeProperties.getPropertyInteger("gt.bum.szt"), xm.SIZE_THRESHOLD);

		final Integer nl = 10;
		GumTreeProperties.setProperty("gt.bum.szt", nl.toString());
		xm.configure();
		assertEquals(nl, xm.SIZE_THRESHOLD);

	}

	@Test
	void AbstractSubtreeMatcher() {

		AbstractSubtreeMatcher xm = new GreedySubtreeMatcher();

		assertNotNull(GumTreeProperties.getProperty("gt.stm.mh"));
		assertEquals("2", GumTreeProperties.getProperty("gt.stm.mh"));

		assertEquals(GumTreeProperties.getPropertyInteger("gt.stm.mh"), xm.MIN_HEIGHT);

		final Integer nl = 10;
		GumTreeProperties.setProperty("gt.stm.mh", nl.toString());
		xm.configure();
		assertEquals(nl, xm.MIN_HEIGHT);

	}

	@Test
	void SimpleBottomUpMatcher() {

		SimpleBottomUpMatcher xm = new SimpleBottomUpMatcher();

		assertNotNull(GumTreeProperties.getProperty("gt.bum.smt.sbup"));
		assertEquals("0.4", GumTreeProperties.getProperty("gt.bum.smt.sbup"));

		assertEquals(GumTreeProperties.getPropertyDouble("gt.bum.smt.sbup"), xm.SIM_THRESHOLD, 0);

		final Double newth = 0.99;
		GumTreeProperties.setProperty("gt.bum.smt.sbup", newth.toString());
		xm.configure();
		assertEquals(newth, xm.SIM_THRESHOLD, 0);

	}

}
