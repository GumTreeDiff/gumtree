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

package com.github.gumtreediff.client;

import com.github.gumtreediff.gen.Registry;

public class Clients extends Registry<String, Client, Register> {
    private static Clients registry;

    public static Clients getInstance() {
        if (registry == null)
            registry = new Clients();
        return registry;
    }

    protected String getName(Register annotation, Class<? extends Client> clazz) {
        String name = annotation.name();
        if (Register.no_value.equals(name))
            name = clazz.getSimpleName().toLowerCase();
        return name;
    }

    @Override
    protected Entry newEntry(Class<? extends Client> clazz, Register annotation) {
        String name = annotation.name().equals(Register.no_value)
                ? clazz.getSimpleName() : annotation.name();
        return new Entry(name.toLowerCase(), clazz, defaultFactory(clazz, String[].class),  annotation.priority()) {
            @Override
            protected boolean handle(String key) {
                return id.equalsIgnoreCase(key);
            }

            final String description;
            {
                description = annotation.description();
            }

            @Override
            public String toString() {
                return String.format("%s: %s", id, description);
            }
        };
    }
}