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

package com.github.gumtreediff.gen;

import com.github.gumtreediff.tree.TreeContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

public class Generators extends Registry<String, TreeGenerator, Register> {

    private static Generators registry;

    public static final Generators getInstance() {
        if (registry == null)
            registry = new Generators();
        return registry;
    }

    public TreeContext getTree(String file) throws UnsupportedOperationException, IOException {
        TreeGenerator p = get(file);
        if (p == null)
            throw new UnsupportedOperationException("No generator found for file: " + file);
        return p.generateFromFile(file);
    }

    public TreeContext getTree(String generator, String file) throws UnsupportedOperationException, IOException {
        for (Entry e : entries)
            if (e.id.equals(generator))
                return e.instantiate(null).generateFromFile(file);
        throw new UnsupportedOperationException("No generator \"" + generator + "\" found.");
    }

    @Override
    protected Entry newEntry(Class<? extends TreeGenerator> clazz, Register annotation) {
        return new Entry(annotation.id(), clazz, defaultFactory(clazz), annotation.priority()) {
            final Pattern[] accept;

            {
                String[] accept = annotation.accept();
                this.accept = new Pattern[accept.length];
                for (int i = 0; i < accept.length; i++)
                    this.accept[i] = Pattern.compile(accept[i]);
            }

            @Override
            protected boolean handle(String key) {
                for (Pattern pattern : accept)
                    if (pattern.matcher(key).find())
                        return true;
                return false;
            }

            @Override
            public String toString() {
                return String.format("%s: %s", Arrays.toString(accept), clazz.getCanonicalName());
            }
        };
    }
}
