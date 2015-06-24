package com.github.gumtreediff.gen.antlrjson;

import static org.junit.Assert.*;

import java.io.InputStreamReader;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.Test;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

public class TestJsonParsing {

    @Test
    public void testJsonParsing() throws Exception {
        TreeContext tc = new AntlrJsonTreeGenerator().generateFromReader(
                new InputStreamReader(getClass().getResourceAsStream("/sample.json")));
        ITree tree = tc.getRoot();
        assertEquals(4, tree.getType());
        assertEquals(37, tree.getSize());
    }

}
