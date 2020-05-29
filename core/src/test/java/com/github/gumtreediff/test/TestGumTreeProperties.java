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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.github.gumtreediff.matchers.ConfigurationOptions;
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

    @Test
    void testBottomUpMatcher() {

        XyBottomUpMatcher xm = new XyBottomUpMatcher();
        double originalValue = 0.5;
        assertEquals(originalValue, xm.getSim_threshold());

        final double localth = 0.888888;
        GumTreeProperties customProperties = new GumTreeProperties();
        customProperties.put(ConfigurationOptions.GT_XYM_SIM, localth);
        xm.configure(customProperties);
        assertEquals(localth, xm.getSim_threshold(), 0);

        GumTreeProperties noPropertyProperties = new GumTreeProperties();
        // No value inside

        xm.configure(noPropertyProperties);
        assertEquals(originalValue, xm.getSim_threshold(), 0);

        Set<ConfigurationOptions> options = xm.getApplicableOptions();
        assertEquals(1, options.size());
        assertTrue(options.contains(ConfigurationOptions.GT_XYM_SIM));
    }

    @Test
    void testChangeDistillerBottomUpMatcher() {

        //
        ChangeDistillerBottomUpMatcher xm = new ChangeDistillerBottomUpMatcher();

        Double anotherValue = 0.9999;
        GumTreeProperties properties = new GumTreeProperties();

        properties.put(ConfigurationOptions.GT_CD_SSIM1, anotherValue);
        properties.put(ConfigurationOptions.GT_CD_SSIM2, anotherValue);

        assertNotEquals(anotherValue, xm.getStruct_sim_threshold_1());
        assertNotEquals(anotherValue, xm.getStruct_sim_threshold_2());

        int defaultNL = xm.getMax_number_of_leaves();
        xm.configure(properties);
        assertEquals(defaultNL, xm.getMax_number_of_leaves(), 0);
        assertEquals(anotherValue, xm.getStruct_sim_threshold_1(), 0);
        assertEquals(anotherValue, xm.getStruct_sim_threshold_2(), 0);


        int newNl = 1111;
        GumTreeProperties properties2 = new GumTreeProperties();
        properties2.put(ConfigurationOptions.GT_CD_ML, newNl);
        xm.configure(properties2);
        assertEquals(newNl, xm.getMax_number_of_leaves());

        Set<ConfigurationOptions> options = xm.getApplicableOptions();
        assertEquals(3, options.size());
        assertTrue(options.contains(ConfigurationOptions.GT_CD_SSIM1));
        assertTrue(options.contains(ConfigurationOptions.GT_CD_SSIM2));
        assertTrue(options.contains(ConfigurationOptions.GT_CD_ML));
    }

    @Test
    void testChangeDistillerLeavesMatcher() {

        ChangeDistillerLeavesMatcher xm = new ChangeDistillerLeavesMatcher();

        GumTreeProperties properties = new GumTreeProperties();

        final Double anotherValue = 0.99999;

        properties.put(ConfigurationOptions.GT_CD_LSIM, anotherValue);
        xm.configure(properties);
        assertEquals(anotherValue, xm.getLabel_sim_threshold(), 0);

        Set<ConfigurationOptions> options = xm.getApplicableOptions();
        assertEquals(1, options.size());
        assertTrue(options.contains(ConfigurationOptions.GT_CD_LSIM));
    }

    @Test
    void testAbstractBottomUpMatcher() {

        AbstractBottomUpMatcher xm = new CompleteBottomUpMatcher();

        GumTreeProperties properties = new GumTreeProperties();
        final Double anotherValue = 0.99;
        properties.put(ConfigurationOptions.GT_BUM_SMT, anotherValue);
        xm.configure(properties);
        assertEquals(anotherValue, xm.getSim_threshold(), 0);

        final Integer nl = 1000;
        properties.put(ConfigurationOptions.GT_BUM_SZT, nl);
        xm.configure(properties);
        assertEquals(nl, xm.getSize_threshold());

        Set<ConfigurationOptions> options = xm.getApplicableOptions();
        assertEquals(2, options.size());
        assertTrue(options.contains(ConfigurationOptions.GT_BUM_SMT));
        assertTrue(options.contains(ConfigurationOptions.GT_BUM_SZT));
    }

    @Test
    void testAbstractSubtreeMatcher() {

        AbstractSubtreeMatcher xm = new GreedySubtreeMatcher();

        GumTreeProperties properties = new GumTreeProperties();

        final Integer nl = 10;
        properties.put(ConfigurationOptions.GT_STM_MH, nl);
        xm.configure(properties);
        assertEquals(nl, xm.getMin_height());

        Set<ConfigurationOptions> options = xm.getApplicableOptions();
        assertEquals(1, options.size());
        assertTrue(options.contains(ConfigurationOptions.GT_STM_MH));

    }

    @Test
    void testSimpleBottomUpMatcher() {

        SimpleBottomUpMatcher xm = new SimpleBottomUpMatcher();

        GumTreeProperties properties = new GumTreeProperties();

        final Double anotherValue = 0.99;
        properties.put(ConfigurationOptions.GT_BUM_SMT_SBUP, anotherValue.toString());
        xm.configure(properties);
        assertEquals(anotherValue, xm.getSim_threshold(), 0);

        Set<ConfigurationOptions> options = xm.getApplicableOptions();
        assertEquals(1, options.size());
        assertTrue(options.contains(ConfigurationOptions.GT_BUM_SMT_SBUP));

    }

}
