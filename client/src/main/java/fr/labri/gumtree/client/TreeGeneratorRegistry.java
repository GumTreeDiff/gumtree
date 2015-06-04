package fr.labri.gumtree.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.labri.gumtree.io.TreeGenerator;
import fr.labri.gumtree.tree.TreeContext;

public class TreeGeneratorRegistry {
	
	private final List<TreeGenerator> producers;
	
	private static TreeGeneratorRegistry registry;
	
	public final static TreeGeneratorRegistry getInstance() {
		if (registry == null) registry = new TreeGeneratorRegistry();
		return registry;
	}
	
	private TreeGeneratorRegistry() {
		producers = new ArrayList<>();
		
		installGenerator("fr.labri.gumtree.gen.jdt.JdtTreeGenerator");
		installGenerator("fr.labri.gumtree.gen.jdt.cd.CdJdtTreeGenerator");
		installGenerator("fr.labri.gumtree.gen.js.RhinoTreeGenerator");
		installGenerator("gen.sax.SAXTreeGenerator");
		installGenerator("fr.labri.gumtree.gen.antlrjson.AntlrJsonTreeGenerator");
		installGenerator("fr.labri.gumtree.gen.xml.XMLTreeGenerator");
		installGenerator("fr.labri.gumtree.gen.c.CTreeGenerator");
		installGenerator("fr.labri.gumtree.gen.ruby.RubyTreeGenerator");
	}
	
	private void installGenerator(String name) {
		TreeGenerator g = loadGenerator(name);
		if (g != null) {
			// TODO info message ??? 
			producers.add(g);
		}
	}
	
	private TreeGenerator loadGenerator(String name) {
		try {
			Class<?> c = Class.forName(name);
			return c.asSubclass(TreeGenerator.class).newInstance();
		} catch (ClassCastException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			// TODO else warning ? ... or maybe it's the converse
		}
		return null;
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
		return p.generateFromFile(file);
	}
	
	public TreeContext getTree(String file, String[] generators) throws IOException {
		TreeGenerator p = getGenerator(file, generators);
		return p.generateFromFile(file);
	}

}
