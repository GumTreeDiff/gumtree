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

import java.io.PrintStream;
import java.util.ArrayList;

public abstract class Option {
    final String description;
    final String key;
    final int paramCount;

    public interface Context {
        Option[] values();

        static Option[] addValue(Option[] options, Option... newOptions)  {
            Option[] nopts = new Option[options.length + newOptions.length];
            System.arraycopy(options, 0, nopts, 0, options.length);
            System.arraycopy(newOptions, 0, nopts, options.length, newOptions.length);
            return nopts;
        }
    }

    public static class OptionException extends RuntimeException {
        final Context context;

        public OptionException(String message, Context context) {
            this(message, context, null);
        }

        public OptionException(String message) {
            this(message, null, null);
        }

        public OptionException(String message, Throwable cause) {
            this(message, null, cause);
        }

        public OptionException(String message, Context context, Throwable cause) {
            super(message, cause);
            this.context = context;
        }

        public Context getContext() {
            return context;
        }
    }

    public Option(String key, String text) {
        this(key, text, 0);
    }

    public Option(String key, String text, int params) {
        this.key = key;
        this.description = text;
        this.paramCount = params;
    }

    public static String[] processCommandLine(String[] args, Context context) {
        return processCommandLine(args, context.values(), context);
    }

    public static String[] processCommandLine(String[] args, Option[] availableOptions, Context ctx) {
        ArrayList<String> todo = new ArrayList<>(args.length);

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            boolean found = false;
            for (int j = 0; j < availableOptions.length && !found; j++) {
                if (availableOptions[j].hasOption(arg)) {
                    int nbParams = availableOptions[j].paramCount;
                    String[] opts = new String[nbParams];
                    int currentParam = 0;
                    while (currentParam < nbParams) {
                        try {
                            opts[currentParam++] = args[++i];
                        } catch (ArrayIndexOutOfBoundsException e) {
                            throw new OptionException(String.format(
                                    "Option '%s' expects more parameters, using null", arg), ctx);
                        }
                    }
                    availableOptions[j].process(arg, opts);
                    found = true;
                }
            }
            if (!found) {
                todo.add(arg);
            }
        }
        String[] rest = new String[todo.size()];
        todo.toArray(rest);
        return rest;
    }

    protected boolean hasOption(String arg) {
        return key.equals(arg);
    }

    protected abstract void process(String name, String[] args);

    public String formatHelpText() {
        return String.format("%s%s\t%s", key, (paramCount > 0 ? " <" + paramCount + ">" : ""), description);
    }

    @Override
    public String toString() {
        return key;
    }

    public static void displayOptions(PrintStream out, Context ctx) {
        for (Option opt : ctx.values()) {
            out.println(opt.formatHelpText());
        }
    }

    public static class Help extends Option {
        protected final Context context;

        public Help(final Context ctx) {
            super("--help", "Display help (this screen).");
            context = ctx;
        }

        @Override
        public void process(String name, String[] args) {
            displayOptions(System.out, context);
            System.exit(0);
        }
    }

    public static class Text extends Option {
        public Text(String text) {
            super("", text);
        }

        @Override
        public boolean hasOption(String opt) {
            return false;
        }

        @Override
        protected void process(String name, String[] args) {}

        @Override
        public String formatHelpText() {
            return description;
        }
    }

    public static class Verbose extends Option {
        public static boolean verbose = false;

        public Verbose() {
            super("-v", "Verbose mode");
        }

        @Override
        public boolean hasOption(String opt) {
            return super.hasOption(opt) || opt.equals("--verbose");
        }

        @Override
        protected void process(String name, String[] opts) {
            verbose = true;
        }
    }
}
