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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

public class AssociationMap {
    // FIXME or not, should we inline this class ? or use Entry to only have one list ? ... or both
    ArrayList<Object> values = new ArrayList<>();
    ArrayList<String> keys = new ArrayList<>();

    public Object get(String key) {
        int idx = keys.indexOf(key);
        if (idx == -1)
            return null;
        return values.get(idx);
    }

    /**
     * set metadata `key` with `value` and returns the previous value
     * This method won't remove if value == null
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