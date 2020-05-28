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


    /**
     * Stores the properties
     */
    private Properties properties;

    /**
     * Store the default properties.
     */
    protected static GumTreeProperties globalProperties = null;

    static {

        globalProperties = new GumTreeProperties();
        globalProperties.loadDefaultValues();

    }

    public GumTreeProperties() {
        properties = new Properties();
    }

    public GumTreeProperties(Properties properties) {
        this.properties = properties;
    }

    public void loadDefaultValues() {
        InputStream propFile;
        try {
            properties.clear();

            propFile = GumTreeProperties.class.getClassLoader().getResourceAsStream("config-gumtree.properties");

            properties.load(propFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public Integer getPropertyInteger(String key) {
        return Integer.valueOf(properties.getProperty(key));
    }

    public Boolean getPropertyBoolean(String key) {
        return Boolean.valueOf(properties.getProperty(key));
    }

    public Double getPropertyDouble(String key) {
        return Double.valueOf(properties.getProperty(key));
    }

    public Properties getProperties() {
        return properties;
    }

    public static GumTreeProperties getGlobalProperties() {
        return globalProperties;
    }

}
