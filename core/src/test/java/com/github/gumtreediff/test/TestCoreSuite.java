package com.github.gumtreediff.test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestTree.class,
        TestTreeUtils.class,
        TestTreeIoUtils.class,
        TestHash.class,
        TestZsMatcher.class,
        TestRtedMatcher.class })
public class TestCoreSuite {}
