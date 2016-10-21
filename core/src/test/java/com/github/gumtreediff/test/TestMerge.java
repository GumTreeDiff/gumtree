package com.github.gumtreediff.test;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Pair;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.Triple;
import com.github.gumtreediff.tree.merge.Pcs;
import com.github.gumtreediff.tree.merge.PcsMerge;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Set;

public class TestMerge {

    final String[] pcsesDummySrc = {
        "(0@@a,null,1@@b)", "(1@@b,null,3@@c)", "(0@@a,2@@e,null)", "(2@@e,null,null)",
        "(null,0@@a,null)", "(1@@b,3@@d,null)", "(3@@d,null,null)", "(0@@a,1@@b,2@@e)",
        "(1@@b,3@@c,3@@d)", "(null,null,0@@a)", "(3@@c,null,null)"
    };

    @Test
    public void testPcses() {
        ITree root = TreeLoader.getDummySrc();
        Set<Pcs> pcss = Pcs.fromTree(root);
        assertThat(11, is(equalTo(pcss.size())));
        for (int i = 0; i < pcsesDummySrc.length; i++)
            assertThat(pcss.toString(), containsString(pcsesDummySrc[i]));
    }

    @Test
    public void testMerge() {
        Triple<TreeContext, TreeContext, TreeContext> trees = TreeLoader.getMergeTriple();
        ITree base = trees.getFirst().getRoot();
        ITree left = trees.getSecond().getRoot();
        ITree right = trees.getThird().getRoot();
        MappingStore ml = new MappingStore();
        ml.link(base, left);
        ml.link(base.treeAt(new int[] {0}), left.treeAt(new int[] {1}));
        ml.link(base.treeAt(new int[] {0, 0}), left.treeAt(new int[] {1, 0}));
        ml.link(base.treeAt(new int[] {0, 1}), left.treeAt(new int[] {1, 1}));
        ml.link(base.treeAt(new int[] {0, 2}), left.treeAt(new int[] {1, 2}));
        ml.link(base.treeAt(new int[] {1}), left.treeAt(new int[] {0}));
        ml.link(base.treeAt(new int[] {1, 0}), left.treeAt(new int[] {0, 0}));
        Matcher mtl = new FixedMatcher(ml);
        MappingStore mr = new MappingStore();
        mr.link(base, right);
        mr.link(base.treeAt(new int[] {0}), right.treeAt(new int[] {0}));
        mr.link(base.treeAt(new int[] {0, 0}), right.treeAt(new int[] {0, 1}));
        mr.link(base.treeAt(new int[] {0, 1}), right.treeAt(new int[] {0, 0}));
        mr.link(base.treeAt(new int[] {0, 2}), right.treeAt(new int[] {0, 2}));
        mr.link(base.treeAt(new int[] {1}), right.treeAt(new int[] {1}));
        Matcher mtr = new FixedMatcher(mr);
        PcsMerge m = new PcsMerge(trees.getFirst(), trees.getSecond(), trees.getThird(), mtl, mtr);
        Set<Pair<Pcs, Pcs>> inconsistencies = m.computeMerge();
        assertEquals(0, inconsistencies.size());
    }

    @Test
    public void testMergeWithConflict() {
        Triple<TreeContext, TreeContext, TreeContext> trees = TreeLoader.getConflictTriple();
        ITree base = trees.getFirst().getRoot();
        ITree left = trees.getSecond().getRoot();
        ITree right = trees.getThird().getRoot();
        MappingStore ml = new MappingStore();
        ml.link(base, left);
        Matcher mtl = new FixedMatcher(ml);
        MappingStore mr = new MappingStore();
        mr.link(base, right);
        Matcher mtr = new FixedMatcher(mr);
        PcsMerge m = new PcsMerge(trees.getFirst(), trees.getSecond(), trees.getThird(), mtl, mtr);
        Set<Pair<Pcs, Pcs>> inconsistencies = m.computeMerge();
        assertEquals(3, inconsistencies.size());
    }

    static class FixedMatcher extends Matcher {

        public FixedMatcher(MappingStore m) {
            super(null, null, m);
        }

        @Override
        public void match() {}
    }

}
