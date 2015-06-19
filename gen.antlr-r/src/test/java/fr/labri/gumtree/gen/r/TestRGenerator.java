package fr.labri.gumtree.gen.r;

import java.io.IOException;

import org.junit.Test;

import fr.labri.gumtree.tree.ITree;
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
