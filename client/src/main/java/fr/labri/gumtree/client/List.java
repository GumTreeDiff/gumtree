package fr.labri.gumtree.client;

import fr.labri.gumtree.gen.TreeGeneratorRegistry;
import fr.labri.gumtree.matchers.MatcherFactories;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Register(description = "List things (matchers, generators, properties, ...)")
public class List extends Client {

    public static final String SYNTAX = "Syntax: list " + Arrays.toString(Listable.values());
    private final Listable item;

    public List(String[] args) {
        super(args);

        if (args.length == 0)
            throw new Option.OptionException(SYNTAX);

        Listable listable = Listable.valueOf(args[0].toUpperCase());
        if (listable == null)
            throw new Option.OptionException(SYNTAX);
        item = listable;
    }

    @Override
    public void run() throws IOException {
        item.print(System.out);
    }

    enum Listable {
        MATCHERS {
            @Override
            Collection<?> list() {
                return MatcherFactories.listFactories().stream().map(Class::getEnclosingClass).map(Class::getName).collect(Collectors.toList());
            }
        },
        GENERATORS {
            @Override
            Collection<?> list() {
                return TreeGeneratorRegistry.getInstance().listGenerators();
            }
        },
        PROPERTIES {
            @Override
            Collection<?> list() {
                return Arrays.asList(properties);
            }
        },
        CLIENTS {
            @Override
            Collection<?> list() {
                return Run.clients.keySet();
            }
        };

        void print(PrintStream out) {
            this.list().forEach(item -> out.println(item));
        }
        abstract Collection<?> list();
    }

    // This list is generated using (manually) list_properties (it is only an heuristic), some properties may be missing
    public static final String[] properties = new String[] {
            "gumtree.client.experimental (fr.labri.gumtree.client.Run)",
            "gumtree.client.web.port (fr.labri.gumtree.client.diff.WebDiff)",
            "gumtree.generator.experimental (fr.labri.gumtree.gen.TreeGeneratorRegistry)",
            "line.separator (fr.labri.gumtree.io.IndentingXMLStreamWriter)",
            "user.dir (fr.labri.gumtree.client.diff.AbstractDiffClient)"
    };
}
