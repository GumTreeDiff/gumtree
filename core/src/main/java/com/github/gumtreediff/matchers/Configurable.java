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
package com.github.gumtreediff.matchers;

import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Interface for configurable gumtree objects, mainly matchers.
 * Options are furnished via a dedicated GumtreeProperties object as
 * key - values.
 * The list of existing keys is in a dedicated enum.
 *
 * @see ConfigurationOptions
 * @see GumtreeProperties
 */
public interface Configurable {
    /**
     * Default configure method that does nothing.
     * Has to be overriden by subclasses to make use of the
     * data inside of the provided GumTreeProperties object.
     */
    default void configure(GumtreeProperties properties) {
    }

    /**
     * Return the list of options applicable to the objects.
     */
    default Set<ConfigurationOptions> getApplicableOptions() {
        return Sets.newHashSet();
    }

    /**
     * Modify the provided option to the provided value. Raise an exception
     * if the provided option is not in the set of applicable options.
     *
     * @see #getApplicableOptions()
     */
    default void setOption(ConfigurationOptions option, Object value) {
        if (!getApplicableOptions().contains(option))
            throw new IllegalArgumentException(
                    "Option " + option.name() + " is not allowed. Applicable options are: " + getApplicableOptions());

        GumtreeProperties properties = new GumtreeProperties();
        properties.put(option, value);
        configure(properties);
    }
}
