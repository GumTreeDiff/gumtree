package com.github.gumtreediff.gen.sax;

import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.Test;

import java.io.InputStreamReader;

public class TestParsing {
    @Test
    public void testA1() throws Exception {
        TreeContext tc = new SaxTreeGenerator().generateFromReader(
                new InputStreamReader(getClass().getResourceAsStream("/action_v0.xml")));
        TreeIoUtils.toXml(tc).writeTo(System.out);
    }
}
