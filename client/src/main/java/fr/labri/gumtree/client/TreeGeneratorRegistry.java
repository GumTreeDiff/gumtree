package fr.labri.gumtree.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.labri.gumtree.gen.c.CTreeGenerator;
import fr.labri.gumtree.gen.jdt.JdtTreeGenerator;
import fr.labri.gumtree.gen.jdt.cd.CdJdtTreeGenerator;
import fr.labri.gumtree.gen.js.RhinoTreeGenerator;
import fr.labri.gumtree.gen.xml.XMLTreeGenerator;
import fr.labri.gumtree.io.TreeGenerator;
import fr.labri.gumtree.tree.TreeContext;
import gen.sax.SAXTreeGenerator;

public class TreeGeneratorRegistry {
	
	private final List<TreeGenerator> producers;
	
	private static TreeGeneratorRegistry registry;
	
	public final static TreeGeneratorRegistry getInstance() {
		if (registry == null) registry = new TreeGeneratorRegistry();
		return registry;
	}
	
	private TreeGeneratorRegistry() {
		producers = new ArrayList<>();
		producers.add(new JdtTreeGenerator());
		producers.add(new CdJdtTreeGenerator());
		producers.add(new RhinoTreeGenerator());
		producers.add(new SAXTreeGenerator());
		producers.add(new XMLTreeGenerator());
		producers.add(new CTreeGenerator());
	}
	
	private TreeGenerator getGenerator(String file, String[] generators) {
		TreeGenerator fallback = null;
		for (TreeGenerator p: producers) {			
			if (p.handleFile(file)) {
				if (generators == null) return p;
				else {
					if (fallback == null) fallback = p;
					if (Arrays.binarySearch(generators, p.getName()) != -1) return p;
				}
			}
		}
		
		if (fallback != null) return fallback;
		throw new RuntimeException(String.format("No generator found for: '%s'", file));
	}
	
	public TreeContext getTree(String file) throws IOException {
		TreeGenerator p = getGenerator(file, null);
		return p.fromFile(file);
	}
	
	public TreeContext getTree(String file, String[] generators) throws IOException {
		TreeGenerator p = getGenerator(file, generators);
		return p.fromFile(file);
	}

}
