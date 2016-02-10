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
            } catch(Exception e) {
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

    /* @Benchmark
    public void testCompleteGumtree(TreeData d) {
        Matcher m = new CompositeMatchers.CompleteGumtreeMatcher(d.src, d.dst, new MappingStore());
        m.match();
    } */
}
