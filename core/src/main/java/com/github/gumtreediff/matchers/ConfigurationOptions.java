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
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */
package com.github.gumtreediff.matchers;

public enum ConfigurationOptions {
    /**
     * Property defining the minimum similarity threshold in bottom-up
     * matchers to match two inner nodes. It has a double value.
     * @see com.github.gumtreediff.matchers.heuristic.gt.AbstractBottomUpMatcher
     * @see com.github.gumtreediff.matchers.heuristic.gt.SimpleBottomUpMatcher
     */
    bu_minsim,

    /**
     * Property defining the minimum size threshold in bottom-up
     * in order to have the last chance match applied. It has an integer value.
     * @see com.github.gumtreediff.matchers.heuristic.gt.AbstractBottomUpMatcher
     */
    bu_minsize,

    /**
     * Property defining the minimum priority threshold in subtree matchers
     * in order to be considered for matching. Priority relates to the
     * priority calculator (such as size or height). It has an integer value.
     * @see com.github.gumtreediff.matchers.heuristic.gt.AbstractSubtreeMatcher
     */
    st_minprio,

    /**
     * Property defining the priority calculator in subtree matchers.
     * It has an string value that can be either size or height.
     * @see com.github.gumtreediff.matchers.heuristic.gt.AbstractSubtreeMatcher
     */
    st_priocalc,

    /**
     * Property defining the minimum label similarity threshold in change distiller
     * matcher to have a match between two nodes. It has a double value.
     * @see com.github.gumtreediff.matchers.heuristic.cd.ChangeDistillerLeavesMatcher
     */
    cd_labsim,

    /**
     * Property defining the maximum number of leaves threshold in change distiller
     * bottom-up matcher to change from the structsim2 threshold to the strucsim1 threshold
     * in order to match two nodes. It has an integer value.
     * @see com.github.gumtreediff.matchers.heuristic.cd.ChangeDistillerBottomUpMatcher
     */
    cd_maxleaves,

    /**
     * Property defining the minimum similarity threshold in change distiller
     * for nodes having more than cd_maxleaves leaves
     * in order to match two nodes. It has a double value.
     * @see com.github.gumtreediff.matchers.heuristic.cd.ChangeDistillerBottomUpMatcher
     */
    cd_structsim1,

    /**
     * Property defining the minimum similarity threshold in change distiller
     * for nodes having less than (or equals to) cd_maxleaves leaves
     * in order to match two nodes. It has a double value.
     * @see com.github.gumtreediff.matchers.heuristic.cd.ChangeDistillerBottomUpMatcher
     */
    cd_structsim2,

    /**
     * Property defining the minimum similarity threshold in XYDiff bottom-up matcher
     * to match two nodes.
     * @see com.github.gumtreediff.matchers.heuristic.XyBottomUpMatcher
     */
    xy_minsim,
}
