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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

        XyBottomUpMatcher matcher = new XyBottomUpMatcher();
        double originalValue = 0.5;
        assertEquals(originalValue, matcher.getSim_threshold());

        final double localth = 0.888888;
        GumTreeProperties customProperties = new GumTreeProperties();
        customProperties.put(ConfigurationOptions.GT_XYM_SIM, localth);
        matcher.configure(customProperties);
        assertEquals(localth, matcher.getSim_threshold(), 0);

        GumTreeProperties noPropertyProperties = new GumTreeProperties();
        // No value inside

        matcher.configure(noPropertyProperties);
        assertEquals(originalValue, matcher.getSim_threshold(), 0);

        Set<ConfigurationOptions> options = matcher.getApplicableOptions();
        assertEquals(1, options.size());
        assertTrue(options.contains(ConfigurationOptions.GT_XYM_SIM));
    }

    @Test
    void testChangeDistillerBottomUpMatcher() {

        //
        ChangeDistillerBottomUpMatcher matcher = new ChangeDistillerBottomUpMatcher();

        Double anotherValue = 0.9999;
        GumTreeProperties properties = new GumTreeProperties();

        properties.put(ConfigurationOptions.GT_CD_SSIM1, anotherValue);
        properties.put(ConfigurationOptions.GT_CD_SSIM2, anotherValue);

        assertNotEquals(anotherValue, matcher.getStruct_sim_threshold_1());
        assertNotEquals(anotherValue, matcher.getStruct_sim_threshold_2());

        int defaultNL = matcher.getMax_number_of_leaves();
        matcher.configure(properties);
        assertEquals(defaultNL, matcher.getMax_number_of_leaves(), 0);
        assertEquals(anotherValue, matcher.getStruct_sim_threshold_1(), 0);
        assertEquals(anotherValue, matcher.getStruct_sim_threshold_2(), 0);

        int newNl = 1111;
        GumTreeProperties properties2 = new GumTreeProperties();
        properties2.put(ConfigurationOptions.GT_CD_ML, newNl);
        matcher.configure(properties2);
        assertEquals(newNl, matcher.getMax_number_of_leaves());

        Set<ConfigurationOptions> options = matcher.getApplicableOptions();
        assertEquals(3, options.size());
        assertTrue(options.contains(ConfigurationOptions.GT_CD_SSIM1));
        assertTrue(options.contains(ConfigurationOptions.GT_CD_SSIM2));
        assertTrue(options.contains(ConfigurationOptions.GT_CD_ML));
    }

    @Test
    void testChangeDistillerLeavesMatcher() {

        ChangeDistillerLeavesMatcher matcher = new ChangeDistillerLeavesMatcher();

        GumTreeProperties properties = new GumTreeProperties();

        final Double anotherValue = 0.99999;

        properties.put(ConfigurationOptions.GT_CD_LSIM, anotherValue);
        matcher.configure(properties);
        assertEquals(anotherValue, matcher.getLabel_sim_threshold(), 0);

        Set<ConfigurationOptions> options = matcher.getApplicableOptions();
        assertEquals(1, options.size());
        assertTrue(options.contains(ConfigurationOptions.GT_CD_LSIM));
    }

    @Test
    void testAbstractBottomUpMatcher() {

        AbstractBottomUpMatcher matcher = new CompleteBottomUpMatcher();

        GumTreeProperties properties = new GumTreeProperties();
        final Double anotherValue = 0.99;
        properties.put(ConfigurationOptions.GT_BUM_SMT, anotherValue);
        matcher.configure(properties);
        assertEquals(anotherValue, matcher.getSim_threshold(), 0);

        final Integer nl = 1000;
        properties.put(ConfigurationOptions.GT_BUM_SZT, nl);
        matcher.configure(properties);
        assertEquals(nl, matcher.getSize_threshold());

        Set<ConfigurationOptions> options = matcher.getApplicableOptions();
        assertEquals(2, options.size());
        assertTrue(options.contains(ConfigurationOptions.GT_BUM_SMT));
        assertTrue(options.contains(ConfigurationOptions.GT_BUM_SZT));
    }

    @Test
    void testAbstractSubtreeMatcher() {

        AbstractSubtreeMatcher matcher = new GreedySubtreeMatcher();

        GumTreeProperties properties = new GumTreeProperties();

        final Integer nl = 10;
        properties.put(ConfigurationOptions.GT_STM_MH, nl);
        matcher.configure(properties);
        assertEquals(nl, matcher.getMin_height());

        Set<ConfigurationOptions> options = matcher.getApplicableOptions();
        assertEquals(1, options.size());
        assertTrue(options.contains(ConfigurationOptions.GT_STM_MH));

    }

    @Test
    void testSimpleBottomUpMatcher() {

        SimpleBottomUpMatcher matcher = new SimpleBottomUpMatcher();

        GumTreeProperties properties = new GumTreeProperties();

        final Double anotherValue = 0.99;
        properties.put(ConfigurationOptions.GT_BUM_SMT_SBUP, anotherValue.toString());
        matcher.configure(properties);
        assertEquals(anotherValue, matcher.getSim_threshold(), 0);

        Set<ConfigurationOptions> options = matcher.getApplicableOptions();
        assertEquals(1, options.size());
        assertTrue(options.contains(ConfigurationOptions.GT_BUM_SMT_SBUP));

    }

    @Test
    void testValidOptionMatcher() {

        SimpleBottomUpMatcher matcher = new SimpleBottomUpMatcher();

        final Double anotherValue = 0.999;
        matcher.setOption(ConfigurationOptions.GT_BUM_SMT_SBUP, anotherValue);

        assertEquals(anotherValue, matcher.getSim_threshold(), 0);

        Set<ConfigurationOptions> options = matcher.getApplicableOptions();
        assertEquals(1, options.size());
        assertTrue(options.contains(ConfigurationOptions.GT_BUM_SMT_SBUP));

        ConfigurationOptions anotherOption = ConfigurationOptions.GT_STM_MH;
        assertFalse(options.contains(anotherOption));

        matcher = new SimpleBottomUpMatcher();
        final Double originalValue = matcher.getSim_threshold();
        try {
            matcher.setOption(anotherOption, anotherValue);
            fail("Expected one exception: Option not allowed");
        } catch (Exception e) {
            assertEquals(originalValue, matcher.getSim_threshold());
        }

    }

}
