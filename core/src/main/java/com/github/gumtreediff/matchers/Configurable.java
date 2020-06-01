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

public interface Configurable {

    public default void configure(GumTreeProperties properties) {
    }

    public default Set<ConfigurationOptions> getApplicableOptions() {
        return Sets.newHashSet();
    }

    public default void setOption(ConfigurationOptions option, Object value) {
        if (!getApplicableOptions().contains(option))
            throw new RuntimeException(
                    "Option " + option.name() + " is not allowed. Applicable options are: " + getApplicableOptions());

        GumTreeProperties properties = new GumTreeProperties();
        properties.put(option, value);
        configure(properties);

    }
}
