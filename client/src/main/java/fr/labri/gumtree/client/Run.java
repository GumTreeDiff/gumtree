package fr.labri.gumtree.client;

import fr.labri.gumtree.gen.Register;
import fr.labri.gumtree.gen.TreeGenerator;
import fr.labri.gumtree.gen.TreeGeneratorRegistry;
import org.reflections.Reflections;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class Run {

    static void initGenerators() {
        Reflections reflections = new Reflections("fr.labri.gumtree.gen");

        reflections.getSubTypesOf(TreeGenerator.class).forEach(gen -> {
            if (gen.isAnnotationPresent(Register.class))
                TreeGeneratorRegistry.getInstance().installGenerator(gen);
        });
    }

    static void initClients() {
        Reflections reflections = new Reflections("fr.labri.gumtree.client");

        reflections.getSubTypesOf(Client.class).forEach(cli -> {
            clients.put(
                cli.isAnnotationPresent(Name.class) ?
                    cli.getAnnotation(Name.class).value() : cli.getSimpleName().toLowerCase(),
                cli);
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
        }
    }

    public static void main(String args[]) {
        initClients();

        if (args.length == 0) {
            listCommand(System.out);
        } else if (clients.containsKey(args[0])) {
            Class<? extends Client> c = clients.get(args[0]);
            String[] a = new String[args.length - 1];
            System.arraycopy(args, 1, a, 0, a.length);
            startClient(c, a);
        } else {
            System.err.printf("Unknown sub-command '%s'. ", args[0]);
            listCommand(System.err);
        }
    }

    public static void listCommand(PrintStream out) {
        out.println("Sub-commands available:");
        clients.keySet().forEach(name -> out.printf("\t%s\n", name));
    }
}
