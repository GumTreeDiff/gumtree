package fr.labri.gumtree.gen.sax;

import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.tree.TreeContext;
import org.junit.Test;

import java.io.InputStreamReader;

public class TestParsing {
	@Test
	public void testA1() throws Exception {
		TreeContext tc = new SAXTreeGenerator().generateFromReader(new InputStreamReader(getClass().getResourceAsStream("/Dummy_v0.xml")));
		TreeIoUtils.toXml(tc).writeTo(System.out);
//		TreeIoUtils.lispSerializer().toStream(tc, System.out);
//		TreeIoUtils.lispSerializer(tc).toString(tc);
	}
}
