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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.CompositeMatchers.CompositeMatcher;
import com.github.gumtreediff.matchers.ConfigurationOptions;
import com.github.gumtreediff.matchers.GumtreeProperties;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.heuristic.XyBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.cd.ChangeDistillerBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.cd.ChangeDistillerLeavesMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.AbstractBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.AbstractSubtreeMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.CompleteBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.GreedyBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.GreedySubtreeMatcher;

class TestGumtreeProperties {
    @Test
    void testBottomUpMatcher() {
        XyBottomUpMatcher matcher = new XyBottomUpMatcher();
        double originalValue = 0.5;
        assertEquals(originalValue, matcher.getSimThreshold());

        final double localth = 0.888888;
        GumtreeProperties customProperties = new GumtreeProperties();
        customProperties.put(ConfigurationOptions.xy_minsim, localth);
        matcher.configure(customProperties);
        assertEquals(localth, matcher.getSimThreshold(), 0);

        GumtreeProperties noPropertyProperties = new GumtreeProperties();

        matcher.configure(noPropertyProperties);
        assertEquals(originalValue, matcher.getSimThreshold(), 0);

        Set<ConfigurationOptions> options = matcher.getApplicableOptions();
        assertEquals(1, options.size());
        assertTrue(options.contains(ConfigurationOptions.xy_minsim));
    }

    @Test
    void testChangeDistillerBottomUpMatcher() {

        //
        ChangeDistillerBottomUpMatcher matcher = new ChangeDistillerBottomUpMatcher();

        Double anotherValue = 0.9999;
        GumtreeProperties properties = new GumtreeProperties();

        properties.put(ConfigurationOptions.cd_structsim1, anotherValue);
        properties.put(ConfigurationOptions.cd_structsim2, anotherValue);

        assertNotEquals(anotherValue, matcher.getStructSimThreshold1());
        assertNotEquals(anotherValue, matcher.getStructSimThreshold2());

        int defaultNL = matcher.getMaxNumberOfLeaves();
        matcher.configure(properties);
        assertEquals(defaultNL, matcher.getMaxNumberOfLeaves(), 0);
        assertEquals(anotherValue, matcher.getStructSimThreshold1(), 0);
        assertEquals(anotherValue, matcher.getStructSimThreshold2(), 0);

        int newNl = 1111;
        GumtreeProperties properties2 = new GumtreeProperties();
        properties2.put(ConfigurationOptions.cd_maxleaves, newNl);
        matcher.configure(properties2);
        assertEquals(newNl, matcher.getMaxNumberOfLeaves());

        Set<ConfigurationOptions> options = matcher.getApplicableOptions();
        assertEquals(3, options.size());
        assertTrue(options.contains(ConfigurationOptions.cd_structsim1));
        assertTrue(options.contains(ConfigurationOptions.cd_structsim2));
        assertTrue(options.contains(ConfigurationOptions.cd_maxleaves));
    }

    @Test
    void testChangeDistillerLeavesMatcher() {
        ChangeDistillerLeavesMatcher matcher = new ChangeDistillerLeavesMatcher();
        GumtreeProperties properties = new GumtreeProperties();
        final Double anotherValue = 0.99999;

        properties.put(ConfigurationOptions.cd_labsim, anotherValue);
        matcher.configure(properties);
        assertEquals(anotherValue, matcher.getLabelSimThreshold(), 0);

        Set<ConfigurationOptions> options = matcher.getApplicableOptions();
        assertEquals(1, options.size());
        assertTrue(options.contains(ConfigurationOptions.cd_labsim));
    }

    @Test
    void testAbstractBottomUpMatcher() {
        AbstractBottomUpMatcher matcher = new CompleteBottomUpMatcher();
        GumtreeProperties properties = new GumtreeProperties();
        final Double anotherValue = 0.99;

        properties.put(ConfigurationOptions.bu_minsim, anotherValue);
        matcher.configure(properties);
        assertEquals(anotherValue, matcher.getSimThreshold(), 0);

        final Integer nl = 1000;
        properties.put(ConfigurationOptions.bu_minsize, nl);
        matcher.configure(properties);
        assertEquals(nl, matcher.getSizeThreshold());

        Set<ConfigurationOptions> options = matcher.getApplicableOptions();
        assertEquals(2, options.size());
        assertTrue(options.contains(ConfigurationOptions.bu_minsim));
        assertTrue(options.contains(ConfigurationOptions.bu_minsize));
    }

    @Test
    void testAbstractSubtreeMatcher() {
        AbstractSubtreeMatcher matcher = new GreedySubtreeMatcher();
        GumtreeProperties properties = new GumtreeProperties();
        final Integer nl = 10;

        properties.put(ConfigurationOptions.st_minprio, nl);
        matcher.configure(properties);
        assertEquals(nl, matcher.getMinPriority());

        Set<ConfigurationOptions> options = matcher.getApplicableOptions();
        assertEquals(2, options.size());
        assertTrue(options.contains(ConfigurationOptions.st_minprio));
        assertTrue(options.contains(ConfigurationOptions.st_priocalc));
    }

    @Test
    public void testCompositeMatcher() {
        CompositeMatcher composite = new CompositeMatchers.ClassicGumtree();
        List<Matcher> matchers = composite.matchers();

        Stream<Matcher> greedyMatchers = matchers.stream().filter(e -> e instanceof GreedySubtreeMatcher);
        Optional<GreedySubtreeMatcher> opGreedySubTree = greedyMatchers.map(obj -> (GreedySubtreeMatcher) obj)
                .findAny();

        assertTrue(opGreedySubTree.isPresent());

        int newMHvalue = 99999;
        assertNotEquals(newMHvalue, opGreedySubTree.get().getMinPriority());

        GumtreeProperties properties = new GumtreeProperties();
        properties.put(ConfigurationOptions.st_minprio, newMHvalue);

        composite.configure(properties);

        assertEquals(newMHvalue, opGreedySubTree.get().getMinPriority());

        Stream<Matcher> greedyBottomMatchers = matchers.stream().filter(e -> e instanceof GreedyBottomUpMatcher);

        Optional<GreedyBottomUpMatcher> opGreedyBottomUp = greedyBottomMatchers.map(obj -> (GreedyBottomUpMatcher) obj)
                .findAny();

        assertTrue(opGreedyBottomUp.isPresent());

        final int newSizeThrvalue = 989898;
        assertNotEquals(newSizeThrvalue, opGreedyBottomUp.get().getSizeThreshold());

        properties.put(ConfigurationOptions.bu_minsize, newSizeThrvalue);

        double originalSimThr = opGreedyBottomUp.get().getSimThreshold();

        composite.configure(properties);

        assertEquals(newSizeThrvalue, opGreedyBottomUp.get().getSizeThreshold());
        assertEquals(originalSimThr, opGreedyBottomUp.get().getSimThreshold());

        assertNotNull(composite.getApplicableOptions());
        assertFalse(composite.getApplicableOptions().isEmpty());

        int optionsFromGreedySubMatcher = opGreedySubTree.get().getApplicableOptions().size();
        assertEquals(2, optionsFromGreedySubMatcher);

        int optionsFromGreedyBottomUpMatcher = opGreedyBottomUp.get().getApplicableOptions().size();
        assertEquals(2, optionsFromGreedyBottomUpMatcher);

        assertEquals((optionsFromGreedySubMatcher + optionsFromGreedyBottomUpMatcher),
                composite.getApplicableOptions().size());

        assertTrue(composite.getApplicableOptions().containsAll(opGreedySubTree.get().getApplicableOptions()));
        assertTrue(composite.getApplicableOptions().containsAll(opGreedyBottomUp.get().getApplicableOptions()));
    }
}
