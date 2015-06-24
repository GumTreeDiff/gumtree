package com.github.gumtreediff.gen;

import com.github.gumtreediff.tree.TreeContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Generators extends Registry<String, TreeGenerator, Register> {

    private static Generators registry;

    public static final Generators getInstance() {
        if (registry == null)
            registry = new Generators();
        return registry;
    }

    private final List<Entry> entries = new ArrayList<>();

    public TreeContext getTree(String file) throws UnsupportedOperationException, IOException {
        TreeGenerator p = get(file);
        if (p == null)
            throw new UnsupportedOperationException("No generator found for file: " + file);
        return p.generateFromFile(file);
    }

    @Override
    public void install(Class<? extends TreeGenerator> clazz, Register annotation) {
        entries.add(newEntry(clazz, annotation));
    }

    @Override
    protected Entry newEntry(Class<? extends TreeGenerator> clazz, Register annotation) {
        return new TreeGeneratorEntry(annotation.id(), annotation.accept(), clazz, annotation.experimental());
    }

    @Override
    protected Entry findEntry(String key) {
        for (Entry e : entries)
            if (e.handle(key))
                return e;
        return null;
    }

    class TreeGeneratorEntry extends Entry {
        final Pattern[] accept;

        public TreeGeneratorEntry(String id, String[] accept,
                                  Class<? extends TreeGenerator> clazz, boolean experimental) {
            super(id, clazz, defaultFactory(clazz), experimental);

            this.accept = new Pattern[accept.length];
            for (int i = 0; i < accept.length; i++)
                this.accept[i] = Pattern.compile(accept[i]);
        }

        @Override
        protected boolean handle(String key) {
            for (Pattern pattern : accept)
                if (pattern.matcher(key).find())
                    return true;
            return false;
        }
    }
}
