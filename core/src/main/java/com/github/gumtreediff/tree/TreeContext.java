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

package com.github.gumtreediff.tree;

import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.io.TreeIoUtils.MetadataSerializer;
import com.github.gumtreediff.io.TreeIoUtils.MetadataUnserializer;
import com.github.gumtreediff.io.TreeIoUtils.TreeFormatter;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * The tree context class contains an AST together with its context (key - value metadata).
 *
 * @see Tree
 */
public class TreeContext {
    private final Map<String, Object> metadata = new HashMap<>();

    private final MetadataSerializers serializers = new MetadataSerializers();

    private Tree root;

    @Override
    public String toString() {
        return TreeIoUtils.toText(this).toString();
    }

    /**
     * Set the AST of the TreeContext.
     */
    public void setRoot(Tree root) {
        this.root = root;
    }

    /**
     * Return the AST of the TreeContext.
     */
    public Tree getRoot() {
        return root;
    }

    /**
     * Utility method to create a default tree with a given type and label.
     * @see DefaultTree#DefaultTree(Type, String)
     */
    public Tree createTree(Type type, String label) {
        return new DefaultTree(type, label);
    }

    /**
     * Utility method to create a default tree with a given type.
     * @see DefaultTree#DefaultTree(Type)
     */
    public Tree createTree(Type type) {
        return new DefaultTree(type);
    }

    /**
     * Utility method to create a fake tree with the given children.
     * @see FakeTree#FakeTree(Tree...)
     */
    public Tree createFakeTree(Tree... trees) {
        return new FakeTree(trees);
    }

    /**
     * Get the AST metadata with the given key.
     * There is no way to know if the metadata is really null or does not exists.
     *
     * @return the metadata or null if not found
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Store an AST metadata with the given key and value.
     *
     * @return the previous value of metadata if existing or null
     */
    public Object setMetadata(String key, Object value) {
        return metadata.put(key, value);
    }

    /**
     * Get an iterator on the AST metadata.
     */
    public Iterator<Entry<String, Object>> getMetadata() {
        return metadata.entrySet().iterator();
    }

    /**
     * Get the metadata serializers for this tree context.
     */
    public MetadataSerializers getSerializers() {
        return serializers;
    }

    public TreeContext export(MetadataSerializers s) {
        serializers.addAll(s);
        return this;
    }

    public TreeContext export(String key, MetadataSerializer s) {
        serializers.add(key, s);
        return this;
    }

    public TreeContext export(String... name) {
        for (String n : name)
            serializers.add(n, x -> x.toString());
        return this;
    }

    public static class Marshallers<E> {
        Map<String, E> serializers = new HashMap<>();

        public static final Pattern valid_id = Pattern.compile("[a-zA-Z0-9_]*");

        public void addAll(Marshallers<E> other) {
            addAll(other.serializers);
        }

        public void addAll(Map<String, E> serializers) {
            serializers.forEach((k, s) -> add(k, s));
        }

        public void add(String name, E serializer) {
            if (!valid_id.matcher(name).matches()) // TODO I definitely don't like this rule, we should think twice
                throw new RuntimeException("Invalid key for serialization");
            serializers.put(name, serializer);
        }

        public void remove(String key) {
            serializers.remove(key);
        }

        public Set<String> exports() {
            return serializers.keySet();
        }
    }

    public static class MetadataSerializers extends Marshallers<MetadataSerializer> {
        public void serialize(TreeFormatter formatter, String key, Object value) throws Exception {
            MetadataSerializer s = serializers.get(key);
            if (s != null)
                formatter.serializeAttribute(key, s.toString(value));
        }
    }

    public static class MetadataUnserializers extends Marshallers<MetadataUnserializer> {
        public void load(Tree tree, String key, String value) throws Exception {
            MetadataUnserializer s = serializers.get(key);
            if (s != null) {
                if (key.equals("pos"))
                    tree.setPos(Integer.parseInt(value));
                else if (key.equals("length"))
                    tree.setLength(Integer.parseInt(value));
                else
                    tree.setMetadata(key, s.fromString(value));
            }
        }
    }
}
