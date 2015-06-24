package com.github.gumtreediff.test;

import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Pair;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Pair;

public class TreeLoader {

    private TreeLoader() {}

    public static Pair<ITree, ITree> getActionPair() {
        return new Pair<ITree, ITree>(load("/action_v0.xml"), load("/action_v1.xml"));
    }

    public static Pair<ITree, ITree> getZsCustomPair() {
        return new Pair<ITree, ITree>(load("/zs_v0.xml"), load("/zs_v1.xml"));
    }

    public static Pair<ITree, ITree> getZsSlidePair() {
        return new Pair<ITree, ITree>(load("/zs_slide_v0.xml"), load("/zs_slide_v1.xml"));
    }

    public static Pair<ITree, ITree> getDummyPair() {
        return new Pair<ITree, ITree>(load("/Dummy_v0.xml"), load("/Dummy_v1.xml"));
    }

    public static ITree getDummySrc() {
        return load("/Dummy_v0.xml");
    }

    public static ITree getDummyDst() {
        return load("/Dummy_v1.xml");
    }

    public static ITree getDummyBig() {
        return load("/Dummy_big.xml");
    }

    public static ITree load(String name) {
        return TreeIoUtils.fromXml(System.class.getResourceAsStream(name)).getRoot();
    }
}
