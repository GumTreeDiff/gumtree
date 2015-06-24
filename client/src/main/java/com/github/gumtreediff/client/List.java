package com.github.gumtreediff.client;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;

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
//                return MatcherFactories.listFactories().stream().map(
// Class::getEnclosingClass).map(Class::getName).collect(Collectors.toList());
                return null;
            }
        },
        GENERATORS {
            @Override
            Collection<?> list() {
                return null; //Generators.getInstance().listGenerators();
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
                return null; // Run.clients.keySet();
            }
        };

        void print(PrintStream out) {
            this.list().forEach(item -> out.println(item));
        }

        abstract Collection<?> list();
    }

    // This list is generated using (manually) list_properties (it is only an heuristic), some properties may be missing
    public static final String[] properties = new String[] {
            "gumtree.client.experimental (com.github.gumtreediff.client.Run)",
            "gumtree.client.web.port (com.github.gumtreediff.client.diff.WebDiff)",
            "gumtree.generator.experimental (com.github.gumtreediff.gen.TreeGeneratorRegistry)",
            "line.separator (com.github.gumtreediff.io.IndentingXMLStreamWriter)",
            "user.dir (com.github.gumtreediff.client.diff.AbstractDiffClient)"
    };
}
