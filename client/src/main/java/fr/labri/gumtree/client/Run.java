package fr.labri.gumtree.client;

import fr.labri.gumtree.gen.TreeGenerator;
import fr.labri.gumtree.gen.TreeGeneratorRegistry;
import org.reflections.Reflections;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class Run {

    public static boolean USE_EXPERIMENTAL() { // This is a method not a constant otherwise, there won't be any way to set it (due to class loading order)
        return Boolean.parseBoolean(System.getProperty("gumtree.client.experimental", "false"));
    }

    public static class Options implements Option.Context {
        @Override
        public Option[] values() {
            return new Option[]{
                    new Option("-c", "Set global property (-c property value). Properties do not need to be prefixed by gumtree.", 2){

                        @Override
                        protected void process(String name, String[] args) {
                            String key = args[0].startsWith("gumtree.") ? args[0] : "gumtree." + args[0];
                            System.setProperty(key, args[1]);
                        }
                    },
                    new Option.Verbose(),
                    new Help(this)
            };
        }
    }

    private static class Entry {
        public Entry(boolean experimental, String description, Class<? extends Client> clazz, Class<? extends Option.Context> options) {
            this.experimental = experimental;
            this.description = description;
            this.clazz = clazz;
        }

        final boolean experimental;
        final String description;
        final Class<? extends Client> clazz;
    }

    static void initGenerators() {
        Reflections reflections = new Reflections("fr.labri.gumtree.gen");

        reflections.getSubTypesOf(TreeGenerator.class).forEach(gen -> {
            if (gen.isAnnotationPresent(fr.labri.gumtree.gen.Register.class))
                TreeGeneratorRegistry.getInstance().installGenerator(gen);
        });
    }

    static void initClients() {
        Reflections reflections = new Reflections("fr.labri.gumtree.client");

        reflections.getSubTypesOf(Client.class).forEach(cli -> {
            fr.labri.gumtree.client.Register a = cli.getAnnotation(fr.labri.gumtree.client.Register.class);
            if (a != null) {
                clients.put(Register.NO_VALUE.equals(a.name()) ? cli.getSimpleName().toLowerCase() : a.name(),
                        new Entry(a.experimental(), a.description(), cli, a.options()));
            }
        });
    }

    static Map<String, Entry> clients = new HashMap<>();

    static {
        initGenerators();
    }

    public static void startClient(Class<? extends Client> client, String[] args) {
        try {
            Client inst = client.getConstructor(String[].class).newInstance((Object) args);
            try {
                inst.run();
            } catch (Exception e) {
                System.err.printf("** Error while running client %s: %s\n", client.getName(), e);
            }
        } catch (InvocationTargetException e) {
            System.err.printf("** Error while parsing option for %s:\n%s\n", client.getName(), e.getCause());
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            System.err.printf("Can't instantiate client: '%s'\n%s\n", client.getName(), e);
        }
    }

    public static void main(String args[]) {
        initClients();

        Options opts = new Options();
        args = Option.processCommandLine(args, opts);

        Entry client;
        if (args.length == 0) {
            System.err.println("** No command given.");
            displayHelp(System.err, opts);
        } else if ((client = findCmd(args[0])) == null) {
            System.err.printf("** Unknown sub-command '%s'.\n", args[0]);
            displayHelp(System.err, opts);
        } else {
            String[] a = new String[args.length - 1];
            System.arraycopy(args, 1, a, 0, a.length);
            startClient(client.clazz, a);
        }
    }

    public static void displayHelp(PrintStream out, Option.Context ctx) {
        out.println("Available Options:");
        Option.displayOptions(out, ctx);
        out.println("");
        listCommand(out);
    }

    protected static Entry findCmd(String cmd) {
        Entry entry = clients.get(cmd);
        if (entry == null)
            return null;
        if (USE_EXPERIMENTAL() || !entry.experimental)
            return entry;
        return null;
    }

    public static void listCommand(PrintStream out) {
        out.println("Available Commands:");
        boolean exp = USE_EXPERIMENTAL();
        clients.entrySet().forEach(e -> {
                if (exp || !e.getValue().experimental)
                    out.printf("\t%s%s\n", e.getKey(), e.getValue().experimental ? "*" : "");
        });
    }

    static class Help extends Option.Help {
        public Help(Context ctx) {
            super(ctx);
        }

        @Override
        public void process(String name, String[] args) {
            displayHelp(System.out, context);
            System.exit(0);
        }
    }
}
