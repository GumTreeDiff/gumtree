package fr.labri.gumtree.matchers;

import fr.labri.gumtree.gen.Registry;
import fr.labri.gumtree.tree.ITree;

public class Matchers extends Registry.NamedRegistry<String, Matcher, Register> {

    private static Matchers registry;
    private Factory<? extends Matcher> defaultMatcherFactory;

    public static final Matchers getInstance() {
        if (registry == null)
            registry = new Matchers();
        return registry;
    }

    private Matchers() {
        install(CompositeMatchers.ClassicGumtree.class);
    }

    private void install(Class<? extends Matcher> clazz) {
        Register a = clazz.getAnnotation(Register.class);
        if (a == null)
            throw new RuntimeException("Expecting @Register annotation on " + clazz.getName());
        install(clazz, a);
    }

    public Matcher getMatcher(String id, ITree src, ITree dst) {
        return get(id, src, dst, new MappingStore());
    }

    public Matcher getMatcher(ITree src, ITree dst) {
        return defaultMatcherFactory.instantiate(new Object[]{src, dst, new MappingStore()});
    }

    @Override
    protected String getName(Register annotation, Class<? extends Matcher> clazz) {
        return annotation.id();
    }

    @Override
    protected NamedEntry newEntry(Class<? extends Matcher> clazz, Register annotation) {
        Factory<? extends Matcher> factory = defaultFactory(clazz, ITree.class, ITree.class, MappingStore.class);
        if (annotation.defaultMatcher())
            defaultMatcherFactory = factory;
        return new NamedEntry(annotation.id(), clazz, factory, annotation.experimental());
    }
}
