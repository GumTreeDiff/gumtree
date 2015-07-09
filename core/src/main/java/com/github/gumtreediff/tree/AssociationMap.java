package com.github.gumtreediff.tree;


import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

public class AssociationMap {
    ArrayList<String> keys = new ArrayList<>(); // FIXME or not, should we inline this class ? or use Entry to only have one list ? ... or both
    ArrayList<Object> values = new ArrayList<>();

    public Object get(String key) {
        int idx = keys.indexOf(key);
        if (idx == -1)
            return null;
        return values.get(idx);
    }

    /**
     This method won't remove if value == null
     */
    public Object set(String key, Object value) {
        int idx = keys.indexOf(key);
        if (idx == -1) {
            keys.add(key);
            values.add(value);
            return null;
        }
        return values.set(idx, value);
    }

    public Object remove(String key) {
        int idx = keys.indexOf(key);
        if (idx == -1)
            return null;
        if (idx == keys.size() - 1) {
            keys.remove(idx);
            return values.remove(idx);
        }
        keys.set(idx, keys.remove(keys.size() - 1));
        return values.set(idx, values.remove(values.size() - 1));
    }

    public Iterator<Entry<String, Object>> iterator() {
        return new Iterator<Entry<String, Object>>() {
            int currentPos = 0;
            @Override
            public boolean hasNext() {
                return currentPos < keys.size();
            }

            @Override
            public Entry<String, Object> next() {
                Entry<String, Object> e = new AbstractMap.SimpleEntry<>(keys.get(currentPos), values.get(currentPos));
                currentPos++;
                return e;
            }
        };
    }
}