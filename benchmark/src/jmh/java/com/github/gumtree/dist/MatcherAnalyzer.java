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
 * Copyright 2016 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtree.dist;

import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;

import org.openjdk.jmh.annotations.*;

public class MatcherAnalyzer {
    @State(Scope.Benchmark)
    public static class TreeData {
        @Setup
        public void load() {
            try {
                String otherPath = refPath.replace("_v0_", "_v1_");
                src = TreeIoUtils.fromXml().generateFromFile(refPath).getRoot();
                dst = TreeIoUtils.fromXml().generateFromFile(otherPath).getRoot();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Param({})
        public String refPath;

        public ITree src;

        public ITree dst;
    }

    @Benchmark
    public void testClassicGumtree(TreeData d) {
        Matcher m = new CompositeMatchers.ClassicGumtree(d.src, d.dst, new MappingStore());
        m.match();
    }

}
