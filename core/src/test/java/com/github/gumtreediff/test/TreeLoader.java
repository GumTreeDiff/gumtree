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

package com.github.gumtreediff.test;

import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.tree.TreeContext;

import java.io.IOException;

public class TreeLoader {

    private TreeLoader() {}

    public static Pair<TreeContext, TreeContext> getActionPair() {
        return new Pair<>(load("/action_v0.xml"), load("/action_v1.xml"));
    }

    public static Pair<TreeContext, TreeContext> getGumtreePair() {
        return new Pair<>(load("/gumtree_v0.xml"), load("/gumtree_v1.xml"));
    }

    public static Pair<TreeContext, TreeContext> getZsCustomPair() {
        return new Pair<>(load("/zs_v0.xml"), load("/zs_v1.xml"));
    }

    public static Pair<TreeContext, TreeContext> getZsSlidePair() {
        return new Pair<>(load("/zs_slide_v0.xml"), load("/zs_slide_v1.xml"));
    }

    public static Pair<TreeContext, TreeContext> getDummyPair() {
        return new Pair<>(load("/Dummy_v0.xml"), load("/Dummy_v1.xml"));
    }

    public static ITree getDummySrc() {
        return load("/Dummy_v0.xml").getRoot();
    }

    public static ITree getDummyDst() {
        return load("/Dummy_v1.xml").getRoot();
    }

    public static ITree getDummyBig() {
        return load("/Dummy_big.xml").getRoot();
    }

    public static TreeContext load(String name) {
        try {
            return TreeIoUtils.fromXml().generateFromStream(System.class.getResourceAsStream(name));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to load test ressorce: %s", name), e);
        }
    }
}
