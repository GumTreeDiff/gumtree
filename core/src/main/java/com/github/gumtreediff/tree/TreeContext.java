package com.github.gumtreediff.tree;

import com.github.gumtreediff.io.TreeIoUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TreeContext {

    Map<Integer, String> typeLabels = new HashMap<>();
    Map<String, Object> metadata = new HashMap<>();

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
}
