package fr.labri.gumtree.gen.sax;

import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.tree.TreeContext;
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
