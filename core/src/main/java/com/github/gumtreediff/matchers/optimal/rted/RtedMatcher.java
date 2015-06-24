package com.github.gumtreediff.matchers.optimal.rted;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;

import java.util.List;

public class RtedMatcher extends Matcher {

    public RtedMatcher(ITree src, ITree dst, MappingStore store) {
        super(src, dst, store);
    }

    @Override
    public void match() {
        RtedAlgorithm a = new RtedAlgorithm(1D, 1D, 1D);
        a.init(src, dst);
        a.computeOptimalStrategy();
        a.nonNormalizedTreeDist();
        List<int[]> arrayMappings = a.computeEditMapping();
        List<ITree> srcs = TreeUtils.postOrder(src);
        List<ITree> dsts = TreeUtils.postOrder(dst);
        for (int[] m: arrayMappings) {
            if (m[0] != 0 && m[1] != 0) {
                ITree src = srcs.get(m[0] - 1);
                ITree dst = dsts.get(m[1] - 1);
                if (src.isMatchable(dst))
                    addMapping(src, dst);
            }
        }
    }

}
