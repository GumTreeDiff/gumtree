package fr.labri.gumtree.gen;

import fr.labri.gumtree.tree.TreeContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TreeGeneratorRegistry {

    static class Entry {
        final String id;
        final Pattern[] accept;
        final Class<? extends TreeGenerator> generator;

        public Entry(String id, String[] accept, Class<? extends TreeGenerator> generator) {
            this.id = id;
            this.generator = generator;
            this.accept = new Pattern[accept.length];
            for (int i = 0; i < accept.length; i++)
                this.accept[i] = Pattern.compile(accept[i]);
        }

        boolean handleFile(String file) {
            for (Pattern pattern: accept)
                if (pattern.matcher(file).find())
                    return true;
            return false;
        }
    }
	
	private final List<Entry> generators  = new ArrayList<>();
	
	private static TreeGeneratorRegistry registry;
	
	public final static TreeGeneratorRegistry getInstance() {
		if (registry == null)
            registry = new TreeGeneratorRegistry();
		return registry;
	}
	
	private TreeGeneratorRegistry() {
	}
	
	public void installGenerator(String name) {
        try {
            Class<?> c = Class.forName(name);
            installGenerator(c.asSubclass(TreeGenerator.class));
        } catch (ClassNotFoundException e) {
            System.err.println("Can not load generator: " + name);
        }
    }

    public void installGenerator(Class<? extends TreeGenerator> clazz) {
        Register annotation = clazz.getAnnotation(Register.class);
        if (annotation == null)
            System.err.println("Missing @Register annotation on generator: " + clazz);
        else
            loadGenerator(annotation, clazz);
    }

    private void loadGenerator(Register annotation, Class<? extends TreeGenerator> clazz) {
        generators.add(new Entry(annotation.id(), annotation.accept(), clazz));
    }

	private TreeGenerator getGenerator(String file) {
		TreeGenerator fallback = null;
		for (Entry e: this.generators) {
            if (e.handleFile(file)) {
                try {
                    return e.generator.newInstance();
                } catch (InstantiationException | IllegalAccessException e1) {
                    System.err.println("Can not instantiate generator: " + e.id);
                    throw new RuntimeException(String.format("Can not instantiate generator: '%s'", file), e1);
                }
            }
        }
		throw new RuntimeException(String.format("No generator found for: '%s'", file));
	}
	
	public TreeContext getTree(String file) throws IOException {
		TreeGenerator p = getGenerator(file);
		return p.generateFromFile(file);
	}
}
