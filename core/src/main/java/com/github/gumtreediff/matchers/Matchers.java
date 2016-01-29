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

public class Matchers extends Registry.NamedRegistry<String, Matcher, Register> {

    private static Matchers registry;
    private Factory<? extends Matcher> defaultMatcherFactory;

    public static Matchers getInstance() {
        if (registry == null)
            registry = new Matchers();
        return registry;
    }

    private Matchers() {
        install(CompositeMatchers.ClassicGumtree.class);
    }

    private void install(Class<? extends Matcher> clazz) {
        Register a = clazz.getAnnotation(Register.class);
        if (a == null)
            throw new RuntimeException("Expecting @Register annotation on " + clazz.getName());
        install(clazz, a);
    }

    public Matcher getMatcher(String id, ITree src, ITree dst) {
        return get(id, src, dst, new MappingStore());
    }

    public Matcher getMatcher(ITree src, ITree dst) {
        return defaultMatcherFactory.instantiate(new Object[]{src, dst, new MappingStore()});
    }

    @Override
    protected String getName(Register annotation, Class<? extends Matcher> clazz) {
        return annotation.id();
    }

    @Override
    protected Registry.NamedRegistry<String, Matcher, Register>.NamedEntry newEntry(Class<? extends Matcher> clazz, Register annotation) {
        Factory<? extends Matcher> factory = defaultFactory(clazz, ITree.class, ITree.class, MappingStore.class);
        if (annotation.defaultMatcher())
            defaultMatcherFactory = factory;
        return new NamedEntry(annotation.id(), clazz, factory, annotation.experimental());
    }
}
