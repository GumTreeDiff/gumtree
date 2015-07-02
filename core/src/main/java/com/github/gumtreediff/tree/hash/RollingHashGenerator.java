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

package com.github.gumtreediff.tree.hash;

import java.util.HashMap;
import java.util.Map;

import com.github.gumtreediff.tree.ITree;
import static com.github.gumtreediff.tree.hash.HashUtils.*;

public abstract class RollingHashGenerator implements HashGenerator {

    public void hash(ITree t) {
        for (ITree n: t.postOrder())
            if (n.isLeaf())
                n.setHash(leafHash(n));
            else
                n.setHash(innerNodeHash(n));
    }

    public abstract int hashFunction(String s);

    public int leafHash(ITree t) {
        return BASE * hashFunction(HashUtils.inSeed(t)) + hashFunction(HashUtils.outSeed(t));
    }

    public int innerNodeHash(ITree t) {
        int size = t.getSize() * 2 - 1;
        int hash = hashFunction(HashUtils.inSeed(t)) * fpow(BASE, size);

        for (ITree c: t.getChildren()) {
            size = size - c.getSize() * 2;
            hash += c.getHash() * fpow(BASE, size);
        }

        hash += hashFunction(HashUtils.outSeed(t));
        return hash;
    }

    public static class JavaRollingHashGenerator extends RollingHashGenerator {

        @Override
        public int hashFunction(String s) {
            return s.hashCode();
        }

    }

    public static class Md5RollingHashGenerator extends RollingHashGenerator {

        @Override
        public int hashFunction(String s) {
            return md5(s);
        }

    }

    public static class RandomRollingHashGenerator extends RollingHashGenerator {

        private static final Map<String, Integer> digests = new HashMap<>();

        @Override
        public int hashFunction(String s) {
            return rdmHash(s);
        }

        public static int rdmHash(String s) {
            if (!digests.containsKey(s)) {
                int digest = (int) (Math.random() * (Integer.MAX_VALUE - 1));
                digests.put(s, digest);
                return digest;
            } else return digests.get(s);
        }

    }

}
