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

import com.github.gumtreediff.gen.Registry;
import com.github.gumtreediff.tree.ITree;

public class Matchers extends Registry<String, Matcher, Register> {

    private static Matchers registry;
    private Factory<? extends Matcher> defaultMatcherFactory; // FIXME shouln't be removed and use priority instead ?

    public static Matchers getInstance() {
        if (registry == null)
            registry = new Matchers();
        return registry;
    }

    private Matchers() {
        install(CompositeMatchers.ClassicGumtree.class);
        install(CompositeMatchers.ChangeDistiller.class);
        install(CompositeMatchers.XyMatcher.class);
    }

    private void install(Class<? extends Matcher> clazz) {
        Register a = clazz.getAnnotation(Register.class);
        if (a == null)
            throw new RuntimeException("Expecting @Register annotation on " + clazz.getName());
        if (defaultMatcherFactory == null && a.defaultMatcher())
            defaultMatcherFactory = defaultFactory(clazz, ITree.class, ITree.class, MappingStore.class);
        install(clazz, a);
    }

    public Matcher getMatcher(String id, ITree src, ITree dst) {
        return get(id, src, dst, new MappingStore());
    }

    public Matcher getMatcher(ITree src, ITree dst) {
        return defaultMatcherFactory.instantiate(new Object[]{src, dst, new MappingStore()});
    }

    protected String getName(Register annotation, Class<? extends Matcher> clazz) {
        return annotation.id();
    }

    @Override
    protected Entry newEntry(Class<? extends Matcher> clazz, Register annotation) {
        return new Entry(annotation.id(), clazz,
                defaultFactory(clazz, ITree.class, ITree.class, MappingStore.class), annotation.priority()) {

            @Override
            protected boolean handle(String key) {
                return annotation.id().equals(key); // Fixme remove
            }
        };
    }
}
