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

package com.github.gumtreediff.utils;

import org.atteo.classindex.ClassIndex;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Registry of components that can be looked up and instantiated by a key.
 *
 * @param <K> the type of lookup key
 * @param <C> the type of component managed by this registry
 * @param <A> the type of annotation containing component registration metadata
 */
public abstract class Registry<K, C, A extends Annotation> {
    private final NavigableSet<Entry> entries = new TreeSet<>((o1, o2) -> {
        int cmp = Integer.compare(o1.priority, o2.priority);
        if (cmp == 0)
            cmp = o1.id.compareTo(o2.id);
        return cmp;
    });

    public static class Priority {
        public static final int MAXIMUM = 0;
        public static final int HIGH = 25;
        public static final int MEDIUM = 50;
        public static final int LOW = 75;
        public static final int MINIMUM = 100;
    }

    /**
     * Creates an instance registered under the given key.
     *
     * @param key the key used to find a registered component
     * @param args arguments passed to the component's factory
     * @return the created component, or {@code null} if no component is registered for the key
     *     or instantiation fails
     */
    public synchronized C get(K key, Object... args) {
        Factory<? extends C> factory = getFactory(key);
        if (factory != null)
            return factory.instantiate(args);
        return null;
    }

    /**
     * Finds the factory for the given key.
     *
     * @param key the key used to find a registered component
     * @return the component factory, or {@code null} if no component is registered for the key
     */
    public synchronized Factory<? extends C> getFactory(K key) {
        Entry entry = find(key);
        if (entry != null)
            return entry.factory;
        return null;
    }

    /**
     * Finds the entry whose key-matching logic accepts the given key.
     *
     * @param key the key to search for
     * @return the matching entry, or {@code null} if no entry matches
     */
    public synchronized Entry find(K key) {
        return findEntry(key);
    }

    /**
     * Finds an entry by its identifier.
     *
     * @param id the identifier to search for
     * @return the entry with that identifier, or {@code null} if none is found
     */
    public synchronized Entry findById(String id) {
        for (Entry e: entries)
            if (e.id.equals(id))
                return e;
        return null;
    }

    /**
     * Creates and adds an entry for a component class and its registration metadata.
     *
     * @param clazz the component class to register
     * @param annotation the metadata used to create the registry entry
     */
    public synchronized void install(Class<? extends C> clazz, A annotation) {
        Entry entry = newEntry(clazz, annotation);
        entries.add(entry);
    }

    /**
     * Removes all entries from this registry.
     */
    public synchronized void clear() {
        entries.clear();
    }

    protected abstract Entry newEntry(Class<? extends C> clazz, A annotation);

    /**
     * Discovers subclasses indexed for the given component type and installs those annotated
     * with the given registration annotation.
     *
     * @param componentType the base component type to discover
     * @param annotationType the annotation that supplies registration metadata
     */
    protected final synchronized void installAnnotatedSubclasses(
            Class<C> componentType, Class<A> annotationType) {
        ClassIndex.getSubclasses(componentType).forEach(clazz -> {
            A annotation = clazz.getAnnotation(annotationType);
            if (annotation != null)
                install(clazz, annotation);
        });
    }

    protected synchronized Entry findEntry(K key) {
        for (Entry e: entries)
            if (e.handle(key))
                return e;
        return null;
    }

    /**
     * Finds an entry by its component class.
     *
     * @param aClass the component class to search for
     * @return the entry for that class, or {@code null} if none is found
     */
    public synchronized Entry findByClass(Class<? extends C> aClass) {
        for (Entry e: entries)
            if (e.clazz.equals(aClass))
                return e;
        return null;
    }

    /**
     * Returns a snapshot of the entries currently in this registry in registry order.
     *
     * @return an unmodifiable snapshot of the registry entries
     */
    public synchronized Set<Entry> getEntries() {
        Set<Entry> snapshot = new TreeSet<>(entries.comparator());
        snapshot.addAll(entries);
        return Collections.unmodifiableSet(snapshot);
    }

    /**
     * An entry describing a registered component and how to instantiate it.
     */
    public abstract class Entry {
        /** The identifier associated with this entry. */
        public final String id;
        /** The priority used to order this entry in the registry. */
        public final int priority;
        final Class<? extends C> clazz;
        final Factory<? extends C> factory;

        protected Entry(String id, Class<? extends C> clazz, Factory<? extends C> factory, int priority) {
            this.id = id;
            this.clazz = clazz;
            this.factory = factory;
            this.priority = priority;
        }

        /**
         * Creates a component instance using this entry's factory.
         *
         * @param args arguments passed to the factory
         * @return the created component, or {@code null} if instantiation fails
         */
        public C instantiate(Object[] args) {
            try {
                return factory.newInstance(args);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                return null;
            }
        }

        protected abstract boolean handle(K key);

        /**
         * Returns this entry's identifier.
         *
         * @return this entry's identifier
         */
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

    /**
     * Creates instances of a registered component.
     *
     * @param <C> the type of component created by this factory
     */
    public interface Factory<C> {
        /**
         * Creates a component instance.
         *
         * @param args arguments used to create the component
         * @return the created component
         * @throws IllegalAccessException if the component constructor is inaccessible
         * @throws InvocationTargetException if the component constructor throws an exception
         * @throws InstantiationException if the component cannot be instantiated
         */
        C newInstance(Object[] args) throws IllegalAccessException, InvocationTargetException, InstantiationException;

        /**
         * Creates a component instance, returning {@code null} if instantiation fails.
         *
         * @param args arguments used to create the component
         * @return the created component, or {@code null} if instantiation fails
         */
        default C instantiate(Object[] args) {
            try {
                return newInstance(args);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                return null;
            }
        }
    }
}
