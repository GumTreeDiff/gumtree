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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GumtreeProperties {
    Map<String, Object> properties = new HashMap<>();

    public void put(ConfigurationOptions option, Object value) {
        if (option != null)
            this.properties.put(option.name(), value);
    }

    public Object get(ConfigurationOptions option) {
        if (option != null)
            return this.properties.get(option.name());
        else
            return null;
    }

    /**
     * Adds a default value in the properties if there is not a property name passed
     * as argument
     *
     * @param propertyName name of the property to check
     * @param value        value to put in properties if the name does not exist.
     * @return
     */
    private Object setIfNotPresent(String propertyName, Object value) {
        if (!properties.containsKey(propertyName)) {
            properties.put(propertyName, value);
            return value;
        }
        return properties.get(propertyName);
    }

    public String tryConfigure(ConfigurationOptions propertyName, String value) {
        return tryConfigure(propertyName.name(), value);
    }

    private String tryConfigure(String propertyName, String value) {
        Object property = setIfNotPresent(propertyName, value);
        if (property != null)
            return property.toString();

        return value;
    }

    public int tryConfigure(ConfigurationOptions propertyName, int value) {
        return tryConfigure(propertyName.name(), value);
    }

    private int tryConfigure(String propertyName, int value) {
        Object property = setIfNotPresent(propertyName, value);
        if (property != null) {
            try {
                return Integer.parseInt(property.toString());
            }
            catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
        return value;
    }

    public double tryConfigure(ConfigurationOptions propertyName, double value) {
        return tryConfigure(propertyName.name(), value);
    }

    public double tryConfigure(String propertyName, double value) {
        Object property = setIfNotPresent(propertyName, value);
        if (property != null) {
            try {
                return Double.parseDouble(property.toString());
            }
            catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
        return value;
    }

    public String toString() {
        return properties.keySet().stream()
                .map(key -> key + "=" + properties.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
    }
}
