package com.github.gumtreediff.test;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.merge.Pcs;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.Set;

public class TestMerge {

    final String[] pcsesDummySrc = {
        "(0@@a,null,1@@b)", "(1@@b,null,3@@c)", "(0@@a,2@@e,null)", "(2@@e,null,null)",
        "(null,0@@a,null)", "(1@@b,3@@d,null)", "(3@@d,null,null)", "(0@@a,1@@b,2@@e)",
        "(1@@b,3@@c,3@@d)", "(null,null,0@@a)", "(3@@c,null,null)"
    };

    @Test
    public void testMerge() {
        ITree root = TreeLoader.getDummySrc();
        Set<Pcs> pcss = Pcs.fromTree(root);
        assertThat(11, is(equalTo(pcss.size())));
        for (int i = 0; i < pcsesDummySrc.length; i++)
            assertThat(pcss.toString(), containsString(pcsesDummySrc[i]));
    }
}
