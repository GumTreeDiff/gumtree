package fr.labri.gumtree.gen.antlrjson;

import java.io.InputStreamReader;
import java.io.Reader;

import org.junit.Test;

import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.tree.TreeContext;

public class TestParsing {
	@Test
	public void testA1() throws Exception {
		TreeContext tc = new AntlrJsonTreeGenerator().generate("src/test/resources/test_parsing.json");
		TreeIoUtils.toCompactXML(tc).writeTo(System.out);
//		TreeIoUtils.lispSerializer().toStream(tc, System.out);
//		TreeIoUtils.lispSerializer(tc).toString(tc);
	}
}
