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

/**
 * A class to store immutable pairs of objects.
 * @param <T1> the type of the first object.
 * @param <T2> the type of the second object.
 */
public class Pair<T1, T2> {
    /**
     * The first object.
     */
    public final T1 first;

    /**
     * The second object.
     */
    public final T2 second;

    /**
     * Instantiate a pair between the given left and right objects.
     */
    public Pair(T1 a, T2 b) {
        this.first = a;
        this.second = b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Pair<?, ?> pair = (Pair<?, ?>) o;
        return first.equals(pair.first) && second.equals(pair.second);
    }

    @Override
    public int hashCode() {
        int result = first.hashCode();
        result = 33 * result + second.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s -> %s", first.toString(), second.toString());
    }
}
