package com.github.gumtreediff.gen.c;

import java.io.IOException;

import org.junit.Test;
import static org.junit.Assert.*;

import com.github.gumtreediff.tree.ITree;

public class TestCGenerator {

    @Test
    public void testSimpleSyntax() throws IOException {
        String input = "int main() { printf(\"Hello world!\"); return 0; }";
        // @TODO find a way to not depend on cgum binary.
        // ITree t = new CTreeGenerator().generateFromString(input).getRoot();
        // assertEquals(450000, t.getType());
    }

}
