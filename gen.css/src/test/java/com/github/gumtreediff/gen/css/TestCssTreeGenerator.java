package com.github.gumtreediff.gen.css;

import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertEquals;

public class TestCssTreeGenerator {

    @Test
    public void testSimple() throws Exception {
        Reader r = new StringReader("@import url(\"bluish.css\") projection, tv;\n" +
                "body {\n" +
                "\tfont-size: 11pt;\n" +
                "}\n" +
                "ul li {\n" +
                "\tbackground-color: black;\n" +
                "}");
        TreeContext ctx = new CssTreeGenerator().generateFromReader(r);
        ITree tree = ctx.getRoot();
        assertEquals(10, tree.getSize());
    }
}
