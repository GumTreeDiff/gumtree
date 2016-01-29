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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class Generators extends Registry<String, TreeGenerator, Register> {

    private static Generators registry;

    public static final Generators getInstance() {
        if (registry == null)
            registry = new Generators();
        return registry;
    }

    private final List<TreeGeneratorEntry> entries = new ArrayList<>();

    public TreeContext getTree(String file) throws UnsupportedOperationException, IOException {
        TreeGenerator p = get(file);
        if (p == null)
            throw new UnsupportedOperationException("No generator found for file: " + file);
        return p.generateFromFile(file);
    }

    @Override
    public void install(Class<? extends TreeGenerator> clazz, Register annotation) {
        entries.add(newEntry(clazz, annotation));
    }

    @Override
    protected TreeGeneratorEntry newEntry(Class<? extends TreeGenerator> clazz, Register annotation) {
        return new TreeGeneratorEntry(annotation.id(), annotation.accept(), clazz, annotation.experimental());
    }

    @Override
    protected Entry findEntry(String key) {
        for (Entry e : entries)
            if (e.handle(key))
                return e;
        return null;
    }

    public Collection<? extends Entry> getEntries() { // FIXME should copy or transform the list
        return entries;
    }

    class TreeGeneratorEntry extends Entry {
        final Pattern[] accept;

        public TreeGeneratorEntry(String id, String[] accept,
                                  Class<? extends TreeGenerator> clazz, boolean experimental) {
            super(id, clazz, defaultFactory(clazz), experimental);

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
            return String.format("%s: %s", Arrays.toString(accept), clazz);
        }
    }
}
