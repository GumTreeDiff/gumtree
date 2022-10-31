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

import com.github.gumtreediff.utils.Registry;

/**
 * Registry of matchers, using a singleton pattern.
 */
public class Matchers extends Registry<String, Matcher, Register> {
    private static Matchers registry;
    private Factory<? extends Matcher> defaultMatcherFactory; //TODO: use the classical entries for priority handling.
    private int lowestPriority;

    /**
     * Return the matcher registry instance (singleton pattern).
     * @return
     */
    public static Matchers getInstance() {
        if (registry == null)
            registry = new Matchers();
        return registry;
    }

    /**
     * Return the matcher with the given id. If the id do not corresponding to an existing matcher,
     * null is returned.
     */
    public Matcher getMatcher(String id) {
        return get(id);
    }

    /**
     * Return the matcher with the given id. If the id do not corresponding to an existing matcher,
     * the matcher with highest priority is returned.
     *
     * @see #getMatcher()
     */
    public Matcher getMatcherWithFallback(String id) {
        if (id == null)
            return getMatcher();

        Matcher matcher = get(id);
        if (matcher != null)
            return matcher;
        else
            return getMatcher();
    }

    /**
     * Return matcher with the highest priority.
     *
     * @see Register#priority()
     */
    public Matcher getMatcher() {
        return defaultMatcherFactory.instantiate(new Object[]{});
    }

    private Matchers() {
    }

    @Override
    public void install(Class<? extends Matcher> clazz, Register a) {
        if (a == null)
            throw new IllegalArgumentException("Expecting @Register annotation on " + clazz.getName());
        if (defaultMatcherFactory == null) {
            defaultMatcherFactory = defaultFactory(clazz);
            lowestPriority = a.priority();
        }
        else if (a.priority() < lowestPriority) {
            defaultMatcherFactory = defaultFactory(clazz);
            lowestPriority = a.priority();
        }

        super.install(clazz, a);
    }

    @Override
    public void clear() {
        super.clear();
        defaultMatcherFactory = null;
    }

    protected String getName(Register annotation, Class<? extends Matcher> clazz) {
        return annotation.id();
    }

    @Override
    protected Entry newEntry(Class<? extends Matcher> clazz, Register annotation) {
        return new Entry(annotation.id(), clazz,
                defaultFactory(clazz), annotation.priority()) {

            @Override
            protected boolean handle(String key) {
                return annotation.id().equals(key); //FIXME: remove
            }
        };
    }
}
