package gen.sax;

import java.io.InputStreamReader;

import org.junit.Test;

import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.tree.TreeContext;

public class TestParsing {
	@Test
	public void testA1() throws Exception {
		TreeContext tc = new SAXTreeGenerator().generate(new InputStreamReader(getClass().getResourceAsStream("/action_v0.xml")));
		TreeIoUtils.toLISP(tc).writeTo(System.out);
//		TreeIoUtils.lispSerializer().toStream(tc, System.out);
//		TreeIoUtils.lispSerializer(tc).toString(tc);
	}
}
