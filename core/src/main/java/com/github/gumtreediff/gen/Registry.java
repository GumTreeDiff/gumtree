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


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class Registry<K, C, A> {

    Set<Entry> entries = new TreeSet<>((o1, o2) -> {
        int cmp = o1.priority - o2.priority;
        if (cmp == 0)
            cmp = o1.id.compareToIgnoreCase(o2.id); // FIXME or not ... is id a good unique stuff
        return cmp;
    });

    public static class Priority {
        public static final int MAXIMUM = 0;
        public static final int HIGH = 25;
        public static final int MEDIUM = 50;
        public static final int LOW = 75;
        public static final int MINIMUM = 100;
    }

    public C get(K key, Object... args) {
        Factory<? extends C> factory = getFactory(key);
        if (factory != null)
            return factory.instantiate(args);
        return null;
    }

    public Factory<? extends C> getFactory(K key) {
        Entry entry = find(key);
        if (entry != null)
            return entry.factory;
        return null;
    }

    public Entry find(K key) {
        return findEntry(key);
    }

    public Entry findById(String id) {
        for (Entry e: entries)
            if (e.id.equals(id))
                return e;
        return null;
    }

    public void install(Class<? extends C> clazz, A annotation) {
        Entry entry = newEntry(clazz, annotation);
        entries.add(entry);
    }

    protected abstract Entry newEntry(Class<? extends C> clazz, A annotation);

    protected Entry findEntry(K key) {
        for (Entry e: entries)
            if (e.handle(key))
                return e;
        return null;
    }

    public Entry findByClass(Class<? extends C> aClass) {
        for (Entry e: entries)
            if (e.clazz.equals(aClass))
                return e;
        return null;
    }

    public Set<Entry> getEntries() {
        return Collections.unmodifiableSet(entries);
    }

    public abstract class Entry {
        public final String id;
        public final int priority;
        final Class<? extends C> clazz;
        final Factory<? extends C> factory;

        protected Entry(String id, Class<? extends C> clazz, Factory<? extends C> factory, int priority) {
            this.id = id;
            this.clazz = clazz;
            this.factory = factory;
            this.priority = priority;
        }

        public C instantiate(Object[] args) {
            try {
                return factory.newInstance(args);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                return null;
            }
        }

        protected abstract boolean handle(K key);

        @Override
        public String toString() {
            return id;
        }
    }

    protected Factory<? extends C> defaultFactory(Class<? extends C> clazz, Class... signature) {
        try {
            Constructor<? extends C> ctor = clazz.getConstructor(signature);
            return (args) -> ctor.newInstance(args);
        } catch (NoSuchMethodException e) {
            System.out.println(Arrays.toString(clazz.getConstructors()));
            throw new RuntimeException(String.format("This is a static bug. Constructor %s(%s) not found",
                    clazz.getName(), Arrays.toString(signature)), e);
        }
    }

    public interface Factory<C> {
        C newInstance(Object[] args) throws IllegalAccessException, InvocationTargetException, InstantiationException;

        default C instantiate(Object[] args) {
            try {
                return newInstance(args);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                return null;
            }
        }
    }
}
