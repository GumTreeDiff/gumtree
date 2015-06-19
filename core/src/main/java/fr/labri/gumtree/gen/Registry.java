package fr.labri.gumtree.gen;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Registry<K, C, A> {

    private boolean useExperimental = Boolean.parseBoolean(
            System.getProperty(String.format("gumtree.%s.experimental", getClass().getSimpleName()), "false"));

    public C get(K key, Object... args) {
        Factory<? extends C> factory = getFactory(key);
        if (factory != null)
            return factory.instantiate(args);
        return null;
    }

    public Factory<? extends C> getFactory(K key) {
        Entry entry = find(key);
        if (entry != null)
            return entry.factory;
        return null;
    }

    protected Entry find(K key) {
        Entry entry = findEntry(key);
        if (entry == null)
            return null;
        if (useExperimental() || !entry.experimental)
            return entry;
        return null;
    }

    private boolean useExperimental() {
        return useExperimental;
    }

    public abstract void install(Class<? extends C> clazz, A annotation);

    protected abstract Entry newEntry(Class<? extends C> clazz, A annotation);

    protected abstract Entry findEntry(K key);

    protected abstract class Entry {
        final String id;
        final Class<? extends C> clazz;
        final Factory<? extends C> factory;
        final boolean experimental;

        protected Entry(String id, Class<? extends C> clazz, Factory<? extends C> factory, boolean experimental) {
            this.id = id;
            this.clazz = clazz;
            this.factory = factory;
            this.experimental = experimental;
        }

        public C instantiate(Object[] args) {
            try {
                return factory.newInstance(args);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                return null;
            }
        }

        protected abstract boolean handle(K key);
    }

    protected Factory<? extends C> defaultFactory(Class<? extends C> clazz, Class... signature) {
        try {
            Constructor<? extends C> ctor = clazz.getConstructor(signature);
            return (args) -> ctor.newInstance(args);
        } catch (NoSuchMethodException e) {
            System.out.println(Arrays.toString(clazz.getConstructors()));
            throw new RuntimeException(String.format("This is a static bug. Constructor %s(%s) not found",
                    clazz.getName(), Arrays.toString(signature)), e);
        }
    }

    public interface Factory<C> {
        C newInstance(Object[] args) throws IllegalAccessException, InvocationTargetException, InstantiationException;

        default C instantiate(Object[] args) {
            try {
                return newInstance(args);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                return null;
            }
        }
    }

    public abstract static class NamedRegistry<K, C, A> extends Registry<K, C, A> {
        Map<K, NamedEntry> entries = new LinkedHashMap<>();

        protected abstract K getName(A annotation, Class<? extends C> clazz);

        @Override
        public void install(Class<? extends C> clazz, A annotation) {
            K name = getName(annotation, clazz);
            NamedEntry entry = newEntry(clazz, annotation);
            entries.put(name, entry);
        }

        protected abstract NamedEntry newEntry(Class<? extends C> clazz, A annotation);

        @Override
        protected NamedEntry findEntry(K key) {
            NamedEntry e = entries.get(key);
            if (e != null && e.handle(key))
                return e;
            return null;
        }

        protected class NamedEntry extends Entry {
            public NamedEntry(String id, Class<? extends C> clazz, Factory<? extends C> factory, boolean experimental) {
                super(id, clazz, factory, experimental);
            }

            @Override
            protected boolean handle(K key) {
                return true;
            }
        }
    }
}
