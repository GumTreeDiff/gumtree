package com.github.gumtreediff.tree;

import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.io.TreeIoUtils.MetadataSerializer;
import com.github.gumtreediff.io.TreeIoUtils.TreeFormatter;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class TreeContext {

    Map<Integer, String> typeLabels = new HashMap<>();
    final Map<String, Object> metadata = new HashMap<>();
    final MetadataSerializers serializers = new MetadataSerializers();

    ITree root;

    @Override
    public String toString() {
        return TreeIoUtils.toLisp(this).toString();
    }

    public void setRoot(ITree root) {
        this.root = root;
    }

    public ITree getRoot() {
        return root;
    }

    public String getTypeLabel(ITree tree) {
        return getTypeLabel(tree.getType());
    }

    public String getTypeLabel(int type) {
        String tl = typeLabels.get(type);
        if (tl == null)
            tl = Integer.toString(type);
        return tl;
    }

    protected void registerTypeLabel(int type, String name) {
        if (name == null || name.equals(ITree.NO_LABEL))
            return;
        String typeLabel = typeLabels.get(type);
        if (typeLabel == null) {
            typeLabels.put(type, name);
        } else if (!typeLabel.equals(name))
            throw new RuntimeException(String.format("Redefining type %d: '%s' with '%s'", type, typeLabel, name));
    }

    public ITree createTree(int type, String label, String typeLabel) {
        registerTypeLabel(type, typeLabel);

        return new Tree(type, label);
    }

    public ITree createTree(ITree... trees) {
        return new AbstractTree.FakeTree(trees);
    }

    public void validate() {
        root.refresh();
        TreeUtils.postOrderNumbering(root);
    }

    public boolean hasLabelFor(int type) {
        return typeLabels.containsKey(type);
    }

    /**
     * Get a global metadata.
     * There is no way to know if the metadata is really null or does not exists.
     * @param key of metadata
     * @return the metadata or null if not found
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Get a local metadata, if available. Otherwise get a global metadata.
     * There is no way to know if the metadata is really null or does not exists.
     * @param key of metadata
     * @return the metadata or null if not found
     */
    public Object getMetadata(ITree node, String key) {
        Object metadata = node.getMetadata(key);
        if (metadata == null)
            return getMetadata(key);
        return metadata;
    }

    /**
     * Store a global metadata.
     * @param key of the metadata
     * @param value of the metadata
     * @return the previous value of metadata if existed or null
     */
    public Object setMetadata(String key, Object value) {
        return metadata.put(key, value);
    }

    /**
     * Store a local metadata
     * @param key of the metadata
     * @param value of the metadata
     * @return the previous value of metadata if existed or null
     */
    public Object setMetadata(ITree node, String key, Object value) {
        if (node == null)
            return setMetadata(key, value);
        else {
            Object res = node.setMetadata(key, value);
            if (res == null)
                return getMetadata(key);
            return res;
        }
    }

    /**
     * Get an iterator on global metadata only
     * @return
     */
    public Iterator<Entry<String, Object>> getMetadata() {
        return metadata.entrySet().iterator();
    }

    /**
     * Get serializers for this tree context
     * @return
     */
    public MetadataSerializers getSerializers() {
        return serializers;
    }

    public TreeContext export(String name, MetadataSerializer serializer) {
        serializers.add(name, serializer);
        return this;
    }

    public TreeContext export(String... name) {
        for (String n: name)
            serializers.add(n, x -> x.toString());
        return this;
    }

    /**
     * Get an iterator on local and global metadata.
     * To only get local metadata, simply use : `node.getMetadata()`
     * @return
     */
    public Iterator<Entry<String, Object>> getMetadata(ITree node) {
        if (node == null)
            return getMetadata();
        return new Iterator<Entry<String, Object>>() {
            final Iterator<Entry<String, Object>> localIterator = node.getMetadata();
            final Iterator<Entry<String, Object>> globalIterator = getMetadata();
            final Set<String> seenKeys = new HashSet<>();

            Iterator<Entry<String, Object>> currentIterator = localIterator;
            Entry<String, Object> nextEntry;

            {
                next();
            }

            @Override
            public boolean hasNext() {
                return nextEntry != null;
            }

            @Override
            public Entry<String, Object> next() {
                Entry<String, Object> n = nextEntry;
                if (currentIterator == localIterator) {
                    if (localIterator.hasNext()) {
                        nextEntry = localIterator.next();
                        seenKeys.add(nextEntry.getKey());
                        return n;
                    } else {
                        currentIterator = globalIterator;
                    }
                }
                nextEntry = null;
                while (globalIterator.hasNext()) {
                    Entry<String, Object> e = globalIterator.next();
                    if (!(seenKeys.contains(e.getKey()) || (e.getValue() == null))) {
                        nextEntry = e;
                        seenKeys.add(nextEntry.getKey());
                        break;
                    }
                }
                return n;
            }
        };
    }

    public static class MetadataSerializers {
        Map<String, MetadataSerializer> serializers = new HashMap<>();

        public static final Pattern valid_id = Pattern.compile("[a-zA-Z0-9_]*");

        public void addAll(MetadataSerializers other) {
            addAll(other.serializers);
        }

        public void addAll(Map<String, MetadataSerializer> serializers) {
            serializers.forEach((k, s)-> add(k, s));
        }

        public void add(String name, MetadataSerializer serializer) {
            if (!valid_id.matcher(name).matches())
                throw new RuntimeException("Invalid key for serialization");
            serializers.put(name, serializer);
        }

        public void remove(String key) {
            serializers.remove(key);
        }

        public Set<String> exports() {
            return serializers.keySet();
        }

        public void serialize(TreeFormatter formatter, String key, Object value) throws Exception {
            MetadataSerializer s = serializers.get(key);
            if (s != null)
                formatter.serializeAttribute(key, s.toString(value));
        }
    }
}
