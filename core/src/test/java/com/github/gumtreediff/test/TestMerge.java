package com.github.gumtreediff.test;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.merge.Pcs;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class TestMerge {

    @Test
    public void testMerge() {
        ITree root = TreeLoader.getDummySrc();
        Set<Pcs> pcss = Pcs.fromTree(root);
        Assert.assertEquals(11, pcss.size());
        Assert.assertEquals("[(0@@a,null,1@@b), (1@@b,null,3@@c), (0@@a,2@@e,null), (2@@e,null,null), (null,0@@a,null), (1@@b,3@@d,null), (3@@d,null,null), (0@@a,1@@b,2@@e), (1@@b,3@@c,3@@d), (null,null,0@@a), (3@@c,null,null)]", pcss.toString());
    }

}
