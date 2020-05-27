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

import java.io.InputStream;
import java.util.Properties;

public class GumTreeProperties {

    public static Properties properties;

    static {
        reset();
    }

    public static void reset() {
        InputStream propFile;
        try {
            properties = new Properties();
            propFile = GumTreeProperties.class.getClassLoader().getResourceAsStream("config-gumtree.properties");

            properties.load(propFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static Integer getPropertyInteger(String key) {
        return Integer.valueOf(properties.getProperty(key));
    }

    public static Boolean getPropertyBoolean(String key) {
        return Boolean.valueOf(properties.getProperty(key));
    }

    public static Double getPropertyDouble(String key) {
        return Double.valueOf(properties.getProperty(key));
    }

}
