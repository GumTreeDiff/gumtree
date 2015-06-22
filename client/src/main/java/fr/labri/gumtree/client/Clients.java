package fr.labri.gumtree.client;

import fr.labri.gumtree.gen.Registry;

public class Clients extends Registry.NamedRegistry<String, Client, Register> {
    private static Clients registry;

    public static final Clients getInstance() {
        if (registry == null)
            registry = new Clients();
        return registry;
    }

    @Override
    protected String getName(Register annotation, Class<? extends Client> clazz) {
        String name = annotation.name();
        if (Register.no_value.equals(name))
            name = clazz.getSimpleName().toLowerCase();
        return name;
    }

    @Override
    protected ClientEntry newEntry(Class<? extends Client> clazz, Register annotation) {
        return new ClientEntry(clazz, annotation.name(), annotation.description(), annotation.experimental());
    }

    class ClientEntry extends NamedRegistry.NamedEntry {
        final String description;

        public ClientEntry(Class<? extends Client> clazz, String id, String description, boolean experimental) {
            super(id, clazz, defaultFactory(clazz, String[].class), experimental);
            this.description = description;
        }
    }
}