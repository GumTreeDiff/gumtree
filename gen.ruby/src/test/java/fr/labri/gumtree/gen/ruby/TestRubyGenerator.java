package fr.labri.gumtree.gen.ruby;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.junit.Test;
import static org.junit.Assert.*;

import fr.labri.gumtree.tree.ITree;

public class TestRubyGenerator {
	
	@Test
	public void testFileParsing() throws IOException {
		Reader r = new InputStreamReader(getClass().getResourceAsStream("/sample.rb"));
		ITree tree = new RubyTreeGenerator().generateFromReader(r).getRoot();
		assertEquals(102, tree.getType());
		assertEquals(1726, tree.getSize());
	}
	
	@Test
	public void testSimpleSyntax() throws IOException {
		String input = "module Foo; puts \"Hello world!\"; end;";
		ITree t = new RubyTreeGenerator().generateFromString(input).getRoot();
		assertEquals(102, t.getType());
	}
	
	@Test
	public void testRuby2Syntax() throws IOException {
		String input = "{ foo: true }";
		ITree t = new RubyTreeGenerator().generateFromString(input).getRoot();
		assertEquals(102, t.getType());
	}

}
