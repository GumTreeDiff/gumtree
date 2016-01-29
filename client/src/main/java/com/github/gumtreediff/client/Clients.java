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

public class Clients extends Registry.NamedRegistry<String, Client, Register> {
    private static Clients registry;

    public static Clients getInstance() {
        if (registry == null)
            registry = new Clients();
        return registry;
    }

    @Override
    protected String getName(Register annotation, Class<? extends Client> clazz) {
        String name = annotation.name();
        if (Register.no_value.equals(name))
            name = clazz.getSimpleName().toLowerCase();
        return name;
    }

    @Override
    protected ClientEntry newEntry(Class<? extends Client> clazz, Register annotation) {
        return new ClientEntry(clazz, annotation.name(), annotation.description(), annotation.experimental());
    }

    class ClientEntry extends NamedRegistry<String, Client, Register>.NamedEntry {
        final String description;

        public ClientEntry(Class<? extends Client> clazz, String id, String description, boolean experimental) {
            super(id, clazz, defaultFactory(clazz, String[].class), experimental);
            this.description = description;
        }
    }
}