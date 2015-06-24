package com.github.gumtreediff.gen.r;

import java.io.IOException;

import com.github.gumtreediff.tree.ITree;
import org.junit.Test;

import com.github.gumtreediff.tree.ITree;
import static org.junit.Assert.*;

public class TestRGenerator {

    @Test
    public void testSimpleSyntax() throws IOException {
        String input = "v <- c(1,2,3);";
        ITree t = new RTreeGenerator().generateFromString(input).getRoot();
        assertEquals(67, t.getType());
        assertEquals(8, t.getSize());
    }

}
