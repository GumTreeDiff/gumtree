/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.client;

import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.gen.Registry;
import com.github.gumtreediff.gen.TreeGenerator;
import org.reflections.Reflections;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Run {

    public static class Options implements Option.Context {
        @Override
        public Option[] values() {
            return new Option[]{
                    new Option("-c", "Set global property (-c property value). "
                            + "Properties do not need to be prefixed by gumtree.", 2) {

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

    public static void initGenerators() {
        Reflections reflections = new Reflections("com.github.gumtreediff.gen");

        reflections.getSubTypesOf(TreeGenerator.class).forEach(
                gen -> {
                    com.github.gumtreediff.gen.Register a =
                            gen.getAnnotation(com.github.gumtreediff.gen.Register.class);
                    if (a != null)
                        Generators.getInstance().install(gen, a);
            });
    }

    public static void initClients() {
        Reflections reflections = new Reflections("com.github.gumtreediff.client");

        reflections.getSubTypesOf(Client.class).forEach(
                cli -> {
                    com.github.gumtreediff.client.Register a =
                            cli.getAnnotation(com.github.gumtreediff.client.Register.class);
                if (a != null)
                    Clients.getInstance().install(cli, a);
            });
    }

    static {
        initGenerators();
    }

    public static void startClient(String name, Registry.Factory<? extends Client> client, String[] args) {
        try {
            Client inst = client.newInstance(new Object[]{args});
            try {
                inst.run();
            } catch (Exception e) {
                System.err.printf("** Error while running client %s: %s\n", name, e);
            }
        } catch (InvocationTargetException e) {
            System.err.printf("** Error while parsing option for %s:\n%s\n", name, e.getCause());
        } catch (InstantiationException | IllegalAccessException e) {
            System.err.printf("Can't instantiate client: '%s'\n%s\n", name, e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Options opts = new Options();
        args = Option.processCommandLine(args, opts);

        initClients();

        Registry.Factory<? extends Client> client;
        if (args.length == 0) {
            System.err.println("** No command given.");
            displayHelp(System.err, opts);
        } else if ((client = Clients.getInstance().getFactory(args[0])) == null) {
            System.err.printf("** Unknown sub-command '%s'.\n", args[0]);
            displayHelp(System.err, opts);
        } else {
            String[] a = new String[args.length - 1];
            System.arraycopy(args, 1, a, 0, a.length);
            startClient(args[0], client, a);
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
        for (Registry.Entry cmd: Clients.getInstance().getEntries())
            out.println("* " + cmd);
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
