package fr.labri.gumtree.client;

import fr.labri.Option;
import fr.labri.gumtree.gen.TreeGenerator;
import fr.labri.gumtree.gen.TreeGeneratorRegistry;
import org.reflections.Reflections;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class Run {

    public static class Options implements Option.Context {
        @Override
        public Option[] values() {
            return new Option[]{
                    new Option.Verbose(),
                    new Help(this)
            };
        }
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
                clients.put(Register.NO_VALUE.equals(a.name()) ? cli.getSimpleName().toLowerCase() : a.name(), cli);
            }
        });
    }

    static Map<String, Class<? extends Client>> clients = new HashMap<>();

    static {
        initGenerators();
    }

    public static void startClient(Class<? extends Client> client, String[] args) {
        try {
            Client inst = client.getConstructor(String[].class).newInstance((Object) args);
            inst.run();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            System.err.printf("Can't instantiate client: '%s'\n%s\n", client.getName(), e.getLocalizedMessage());
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        initClients();

        Options opts = new Options();
        args = Option.processCommandLine(args, opts);

        if (args.length == 0) {
            System.err.printf("No command given. ");
            displayHelp(System.err, opts);
        } else if (clients.containsKey(args[0])) {
            Class<? extends Client> c = clients.get(args[0]);
            String[] a = new String[args.length - 1];
            System.arraycopy(args, 1, a, 0, a.length);
            startClient(c, a);
        } else {
            System.err.printf("Unknown sub-command '%s'. ", args[0]);
            displayHelp(System.err, opts);
        }
    }

    public static void displayHelp(PrintStream out, Option.Context ctx) {
        out.println("Available Options:");
        Option.displayOptions(out, ctx);
        out.println("");
        listCommand(out);
    }

    public static void listCommand(PrintStream out) {
        out.println("Available Commands:");
        clients.keySet().forEach(name -> out.printf("\t%s\n", name));
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
