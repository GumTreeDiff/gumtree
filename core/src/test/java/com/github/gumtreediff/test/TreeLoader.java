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
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.tree.TreeContext;

import java.io.IOException;

public class TreeLoader {

    private TreeLoader() {}

    /*
     v0
     --
     a
       e
         f
       b
         c
         d
       g
         h
       i
       j
         k

     v1
     --
     z
       b
         c
         d
       h
         e
           y
       x
         w
       j
         u
           v
             k
     */
    public static Pair<TreeContext, TreeContext> getActionPair() {
        return new Pair<>(load("/action_v0.xml"), load("/action_v1.xml"));
    }

    /*
       v0
       -
       a
         e
           f
         b
           c
           d
         g

       v1
       -
       z
         b
           c
           d
         h
           e
             y
         g
     */
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

    public static Pair<TreeContext, TreeContext> getCdCustomPair() {
        return new Pair<>(load("/cd_v0.xml"), load("/cd_v1.xml"));
    }

    public static Pair<Tree, Tree> getBottomUpPair() {
        return new Pair<>(load("/bottom_up_v0.xml").getRoot(),
                load("/bottom_up_v1.xml").getRoot());
    }

    /*
     *  a
     *    b
     *      c
     *      d
     *    e
     */
    public static Tree getDummySrc() {
        return load("/Dummy_v0.xml").getRoot();
    }

    public static Tree getDummyDst() {
        return load("/Dummy_v1.xml").getRoot();
    }

    public static Tree getSubtreeSrc() {
        return load("/subtree.xml").getRoot();
    }

    public static Tree getDummyBig() {
        return load("/Dummy_big.xml").getRoot();
    }

    public static TreeContext load(String name) {
        try {
            return TreeIoUtils.fromXml().generateFrom().stream(TreeLoader.class.getResourceAsStream(name));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to load test ressorce: %s", name), e);
        }
    }
}
