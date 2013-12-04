package fr.labri.gumtree.io;

import java.io.IOException;

import fr.labri.gumtree.tree.Tree;

public abstract class TreeGenerator {
	
	public Tree generate(String file) throws IOException {
		Tree tree = doGenerate(file);
		tree.refreshMetrics();
		return tree;
	}
	
	public abstract Tree doGenerate(String file) throws IOException;
	
	public abstract boolean handleFile(String file);
	
	public abstract String getName();

}
