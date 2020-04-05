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

/**
 * Registry of tree generators, using a singleton pattern.
 */
public class TreeGenerators extends Registry<String, TreeGenerator, Register> {

    private static TreeGenerators registry;

    /**
     * Return the tree generators registry instance (singleton pattern)
     */
    public static TreeGenerators getInstance() {
        if (registry == null)
            registry = new TreeGenerators();
        return registry;
    }

    /**
     * Automatically search a tree generator for the given file path, and use it
     * to parse it
     * @param file the file path
     * @return the TreeContext of the file
     * @throws UnsupportedOperationException if no suitable generator is found
     */
    public TreeContext getTree(String file) throws UnsupportedOperationException, IOException {
        TreeGenerator p = get(file);
        if (p == null)
            throw new UnsupportedOperationException("No generator found for file: " + file);
        return p.generateFrom().file(file);
    }

    /**
     * Use the tree generator with the supplied name to parse the file at the given path
     * to parse it
     * @param generator the tree generator's name. if null, fallbacks to @see getTree(String)
     * @throws UnsupportedOperationException if no suitable generator is found
     */
    public TreeContext getTree(String file, String generator) throws UnsupportedOperationException, IOException {
        if (generator == null)
            return getTree(file);

        for (Entry e : entries)
            if (e.id.equals(generator))
                return e.instantiate(null).generateFrom().file(file);

        throw new UnsupportedOperationException("No generator \"" + generator + "\" found.");
    }

    /**
     * Indicate whether or not the given file path has a related tree generator
     */
    public boolean hasGeneratorForFile(String file) {
        return get(file) != null;
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
                return String.format("%d\t%s\t%s: %s", priority, id, Arrays.toString(accept), clazz.getCanonicalName());
            }
        };
    }
}
