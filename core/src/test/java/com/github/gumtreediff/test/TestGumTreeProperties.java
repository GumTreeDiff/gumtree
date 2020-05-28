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
 * Copyright 2011-2016 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2016 Floréal Morandat <florealm@gmail.com>
 */
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
        GumTreeProperties.getGlobalProperties().loadDefaultValues();
    }

    @Test
    void testDefaultValue() {
        assertNotNull(GumTreeProperties.getGlobalProperties().getProperty("gt.xym.sim"));
        assertEquals("0.5", GumTreeProperties.getGlobalProperties().getProperty("gt.xym.sim"));
        assertEquals(0.5, GumTreeProperties.getGlobalProperties().getPropertyDouble("gt.xym.sim"), 0);
    }

    @Test
    void testBottomUpMatcher() {

        assertNotNull(GumTreeProperties.getGlobalProperties().getProperty("gt.xym.sim"));
        assertEquals("0.5", GumTreeProperties.getGlobalProperties().getProperty("gt.xym.sim"));

        XyBottomUpMatcher xm = new XyBottomUpMatcher();
        assertEquals(GumTreeProperties.getGlobalProperties().getPropertyDouble("gt.xym.sim"), xm.getSim_threshold(), 0);

        Double newth = 0.99;
        GumTreeProperties.getGlobalProperties().setProperty("gt.xym.sim", newth.toString());
        xm.configure(GumTreeProperties.getGlobalProperties());
        assertEquals(newth, xm.getSim_threshold(), 0);

        Double localth = 0.88;
        GumTreeProperties customProperties = new GumTreeProperties();
        customProperties.setProperty("gt.xym.sim", localth.toString());
        xm.configure(customProperties);
        assertEquals(localth, xm.getSim_threshold(), 0);

        GumTreeProperties inheritProperties = new GumTreeProperties(
                GumTreeProperties.getGlobalProperties().getProperties());
        xm.configure(inheritProperties);
        assertEquals(newth, xm.getSim_threshold(), 0);
    }

    @Test
    void testChangeDistillerBottomUpMatcher() {

        assertNotNull(GumTreeProperties.getGlobalProperties().getProperty("gt.cd.ssim1"));
        assertEquals("0.6", GumTreeProperties.getGlobalProperties().getProperty("gt.cd.ssim1"));

        assertNotNull(GumTreeProperties.getGlobalProperties().getProperty("gt.cd.ssim1"));
        assertEquals("0.4", GumTreeProperties.getGlobalProperties().getProperty("gt.cd.ssim2"));

        ChangeDistillerBottomUpMatcher xm = new ChangeDistillerBottomUpMatcher();
        assertEquals(GumTreeProperties.getGlobalProperties().getPropertyDouble("gt.cd.ssim1"),
                xm.getStruct_sim_threshold_1(), 0);
        assertEquals(GumTreeProperties.getGlobalProperties().getPropertyDouble("gt.cd.ssim2"),
                xm.getStruct_sim_threshold_2(), 0);

        Double newth = 0.99;
        GumTreeProperties.getGlobalProperties().setProperty("gt.cd.ssim1", newth.toString());
        GumTreeProperties.getGlobalProperties().setProperty("gt.cd.ssim2", newth.toString());
        xm.configure(GumTreeProperties.getGlobalProperties());
        assertEquals(newth, xm.getStruct_sim_threshold_1(), 0);
        assertEquals(newth, xm.getStruct_sim_threshold_2(), 0);

        assertNotNull(GumTreeProperties.getGlobalProperties().getProperty("gt.cd.ml"));
        assertEquals("4", GumTreeProperties.getGlobalProperties().getProperty("gt.cd.ml"));

        assertEquals(GumTreeProperties.getGlobalProperties().getPropertyInteger("gt.cd.ml"),
                xm.getMax_number_of_leaves());

        final Integer nl = 10;
        GumTreeProperties.getGlobalProperties().setProperty("gt.cd.ml", nl.toString());
        xm.configure(GumTreeProperties.getGlobalProperties());
        assertEquals(nl, xm.getMax_number_of_leaves());

    }

    @Test
    void testChangeDistillerLeavesMatcher() {

        assertNotNull(GumTreeProperties.getGlobalProperties().getProperty("gt.cd.lsim"));
        assertEquals("0.5", GumTreeProperties.getGlobalProperties().getProperty("gt.cd.lsim"));

        ChangeDistillerLeavesMatcher xm = new ChangeDistillerLeavesMatcher();
        assertEquals(GumTreeProperties.getGlobalProperties().getPropertyDouble("gt.cd.lsim"),
                xm.getLabel_sim_threshold(), 0);

        final Double newth = 0.99;
        GumTreeProperties.getGlobalProperties().setProperty("gt.cd.lsim", newth.toString());
        xm.configure(GumTreeProperties.getGlobalProperties());
        assertEquals(newth, xm.getLabel_sim_threshold(), 0);
    }

    @Test
    void testAbstractBottomUpMatcher() {

        assertNotNull(GumTreeProperties.getGlobalProperties().getProperty("gt.bum.smt"));
        assertEquals("0.5", GumTreeProperties.getGlobalProperties().getProperty("gt.bum.smt"));

        AbstractBottomUpMatcher xm = new CompleteBottomUpMatcher();
        assertEquals(GumTreeProperties.getGlobalProperties().getPropertyDouble("gt.bum.smt"), xm.getSim_threshold(), 0);

        final Double newth = 0.99;
        GumTreeProperties.getGlobalProperties().setProperty("gt.bum.smt", newth.toString());
        xm.configure(GumTreeProperties.getGlobalProperties());
        assertEquals(newth, xm.getSim_threshold(), 0);

        assertNotNull(GumTreeProperties.getGlobalProperties().getProperty("gt.bum.szt"));
        assertEquals("1000", GumTreeProperties.getGlobalProperties().getProperty("gt.bum.szt"));

        assertEquals(GumTreeProperties.getGlobalProperties().getPropertyInteger("gt.bum.szt"), xm.getSize_threshold());

        final Integer nl = 10;
        GumTreeProperties.getGlobalProperties().setProperty("gt.bum.szt", nl.toString());
        xm.configure(GumTreeProperties.getGlobalProperties());
        assertEquals(nl, xm.getSize_threshold());

    }

    @Test
    void testAbstractSubtreeMatcher() {

        AbstractSubtreeMatcher xm = new GreedySubtreeMatcher();

        assertNotNull(GumTreeProperties.getGlobalProperties().getProperty("gt.stm.mh"));
        assertEquals("2", GumTreeProperties.getGlobalProperties().getProperty("gt.stm.mh"));

        assertEquals(GumTreeProperties.getGlobalProperties().getPropertyInteger("gt.stm.mh"), xm.getMin_height());

        final Integer nl = 10;
        GumTreeProperties.getGlobalProperties().setProperty("gt.stm.mh", nl.toString());
        xm.configure(GumTreeProperties.getGlobalProperties());
        assertEquals(nl, xm.getMin_height());

    }

    @Test
    void testSimpleBottomUpMatcher() {

        SimpleBottomUpMatcher xm = new SimpleBottomUpMatcher();

        assertNotNull(GumTreeProperties.getGlobalProperties().getProperty("gt.bum.smt.sbup"));
        assertEquals("0.4", GumTreeProperties.getGlobalProperties().getProperty("gt.bum.smt.sbup"));

        assertEquals(GumTreeProperties.getGlobalProperties().getPropertyDouble("gt.bum.smt.sbup"),
                xm.getSim_threshold(), 0);

        final Double newth = 0.99;
        GumTreeProperties.getGlobalProperties().setProperty("gt.bum.smt.sbup", newth.toString());
        xm.configure(GumTreeProperties.getGlobalProperties());
        assertEquals(newth, xm.getSim_threshold(), 0);

    }

}
