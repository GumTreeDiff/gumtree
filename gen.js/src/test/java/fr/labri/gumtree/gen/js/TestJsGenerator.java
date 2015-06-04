package fr.labri.gumtree.gen.js;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.junit.Test;

import fr.labri.gumtree.tree.ITree;

public class TestJsGenerator {
	
	@Test
	public void testFileParsing() throws IOException {
		Reader r = new InputStreamReader(getClass().getResourceAsStream("/sample.js"));
		ITree tree = new RhinoTreeGenerator().generateFromReader(r).getRoot();
		assertEquals(136, tree.getType());
		assertEquals(401, tree.getSize());
	}
	
	@Test
	public void testSimpleSyntax() throws IOException {
		String input = "console.log(\"Hello world!\");";
		ITree tree = new RhinoTreeGenerator().generateFromString(input).getRoot();
		assertEquals(136, tree.getType());
		assertEquals(7, tree.getSize());
	}

}
