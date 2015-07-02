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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.github.gumtreediff.tree.ITree;

public class HashUtils {

    private HashUtils() {}

    public static final int BASE = 33;

    public static final HashGenerator DEFAULT_HASH_GENERATOR = new RollingHashGenerator.Md5RollingHashGenerator();

    public static int byteArrayToInt(byte[] b) {
        return   b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24;
    }

    public static int standardHash(ITree t) {
        return Integer.hashCode(t.getType()) + HashUtils.BASE * t.getLabel().hashCode();
    }

    public static String inSeed(ITree t) {
        return ITree.OPEN_SYMBOL + t.getLabel() + ITree.SEPARATE_SYMBOL + t.getType();
    }

    public static String outSeed(ITree t) {
        return  t.getType() + ITree.SEPARATE_SYMBOL + t.getLabel() + ITree.CLOSE_SYMBOL;
    }

    public static int md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(s.getBytes());
            return byteArrayToInt(digest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return ITree.NO_VALUE;
    }

    public static int fpow(int a, int b) {
        if (b == 1)
            return a;
        int result = 1;
        while (b > 0) {
            if ((b & 1) != 0)
                result *= a;
            b >>= 1;
            a *= a;
        }
        return result;
    }

}
