package fr.labri.gumtree.tree;


import java.util.ArrayList;

public class AssociationMap {
    ArrayList<String> keys = new ArrayList<>(); // FIXME or not, should we inline this class ?
    ArrayList<Object> values = new ArrayList<>();

    // TODO Maybe let's add some bloom filter for compact hashing

    <M> M fetch(String key, M defaultValue) {
        int i = keys.lastIndexOf(key);
        if (i == -1)
            return defaultValue;
        return (M) values.get(i);
    }

    void put(String key, Object value) {
        keys.add(keys.size(), key);
        values.add(values.size(), value);
    }

    boolean replace(String key, Object value) {
        int i = keys.lastIndexOf(key);
        if (i == -1) {
            put(key, value);
            return true;
        }
        keys.set(i, key);
        values.set(i, value);
        return false;
    }

    boolean remove(String key) {
        int i = keys.lastIndexOf(key);
        if (i == -1)
            return false;
        keys.remove(i); // this is slow ! but we can switch the last,
                        // since it may change the order of some other keys.
        values.remove(i);
        return true;
    }
    
    void compact() {
        keys.trimToSize();
        values.trimToSize();
    }
}