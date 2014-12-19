package gen.sax;

import org.junit.Test;

import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.tree.TreeContext;

public class TestParsing {
	@Test
	public void testA1() throws Exception {
		TreeContext tc = new SAXTreeGenerator().generate(getClass().getResourceAsStream("/action_v0.xml"));
		TreeIoUtils.toLISP(tc).writeTo(System.out);
//		System.out.println(TreeIoUtils.toLISP(tc).toString());
	}
}
