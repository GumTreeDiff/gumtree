package com.github.gumtreediff.test;

import com.github.gumtreediff.matchers.CompositeMatcher;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.heuristic.gt.GreedyBottomUpMatcher;
import com.github.gumtreediff.matchers.heuristic.gt.GreedySubtreeMatcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Pair;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestGumtreeMatcher {

    @Test
    public void testMinHeightThreshold() {
        Pair<TreeContext, TreeContext> trees = TreeLoader.getGumtreePair();
        GreedySubtreeMatcher.MIN_HEIGHT = 0;
        GreedyBottomUpMatcher.SIZE_THRESHOLD = 0;
        Matcher m = new CompositeMatchers.ClassicGumtree(trees.getFirst().getRoot(), trees.getSecond().getRoot(), new MappingStore());
        m.match();
        assertEquals(5, m.getMappingSet().size());
        GreedySubtreeMatcher.MIN_HEIGHT = 1;
        GreedyBottomUpMatcher.SIZE_THRESHOLD = 0;
        m = new CompositeMatchers.ClassicGumtree(trees.getFirst().getRoot(), trees.getSecond().getRoot(), new MappingStore());
        m.match();
        assertEquals(4, m.getMappingSet().size());
    }

    @Test
    public void testSizeThreshold() {
        Pair<TreeContext, TreeContext> trees = TreeLoader.getGumtreePair();
        GreedySubtreeMatcher.MIN_HEIGHT = 0;
        GreedyBottomUpMatcher.SIZE_THRESHOLD = 0;
        Matcher m = new CompositeMatchers.ClassicGumtree(trees.getFirst().getRoot(), trees.getSecond().getRoot(), new MappingStore());
        m.match();
        assertEquals(5, m.getMappingSet().size());
        GreedySubtreeMatcher.MIN_HEIGHT = 0;
        GreedyBottomUpMatcher.SIZE_THRESHOLD = 5;
        m = new CompositeMatchers.ClassicGumtree(trees.getFirst().getRoot(), trees.getSecond().getRoot(), new MappingStore());
        m.match();
        assertEquals(6, m.getMappingSet().size());
    }

}
