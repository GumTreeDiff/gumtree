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

import java.util.function.Function;

public class Couple<T1, T2> {

    public final T1 one;

    public final T2 other;

    public Couple(T1 one, T2 other) {
        this.one = one;
        this.other = other;
    }

    public T1 getOne() {
        return one;
    }

    public T2 getOther() {
        return other;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Couple<?, ?> couple = (Couple<?, ?>) o;
        if (one.equals(couple.one) && other.equals(couple.other))
            return true;
        if (one.equals(couple.other) && other.equals(couple.one))
            return true;
        return false;
    }

    @Override
    public int hashCode() {
        return one.hashCode() + other.hashCode();
    }

    @Override
    public String toString() {
        return "{" + getOne().toString() + ", " + getOther().toString() + "}";
    }

    public final String inspect(Function<T1, String> f1, Function<T2, String> f2) {
        return "(" + f1.apply(one) + ", " + f2.apply(other) + ")";
    }

}
