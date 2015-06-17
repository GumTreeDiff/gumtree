package fr.labri.gumtree.gen;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.TreeContext;
import fr.labri.gumtree.tree.TreeUtils;

public abstract class TreeGenerator {
	
	protected abstract TreeContext generate(Reader r) throws IOException;
	
	public TreeContext generateFromReader(Reader r) throws IOException {
		TreeContext ctx = generate(r);
		ctx.validate();
		return ctx;
	}
	
	public TreeContext generateFromFile(String path) throws IOException {
		return generateFromReader(new FileReader(path));
	}
	
	public TreeContext generateFromFile(File file) throws IOException {
		return generateFromReader(new FileReader(file));
	}
	
	public TreeContext generateFromString(String content) throws IOException {
		return generateFromReader(new StringReader(content));
	}
	
	public abstract boolean handleFile(String file);
	
	public abstract String getName();

}
