package fr.labri.gumtree.gen.antlrjson;

import static org.junit.Assert.*;

import java.io.InputStreamReader;

import org.junit.Test;

import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.TreeContext;

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
