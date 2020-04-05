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

package com.github.gumtreediff.client.diff;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.client.Option;
import com.github.gumtreediff.client.Client;
import com.github.gumtreediff.gen.TreeGenerators;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class AbstractDiffClient<O extends AbstractDiffClient.Options> extends Client {
    protected final O opts;
    public static final String SYNTAX = "Syntax: [options] srcFile dstFile";

    public static class Options implements Option.Context {
        public String matcher;
        public String treeGenerator;
        public String src;
        public String dst;

        @Override
        public Option[] values() {
            return new Option[] {
                    new Option("-m", "Matcher to use.", 1) {
                        @Override
                        protected void process(String name, String[] args) {
                            matcher = args[0];
                        }
                    },
                    new Option("-g", "Tree generator to use.", 1) {
                        @Override
                        protected void process(String name, String[] args) {
                            treeGenerator = args[0];
                        }
                    },
                    new Option.Help(this) {
                        @Override
                        public void process(String name, String[] args) {
                            System.out.println(SYNTAX);
                            super.process(name, args);
                        }
                    }
            };
        }

        void dump(PrintStream out) {
            out.printf("Active path: %s\n", System.getProperty("user.dir"));
            out.printf("Diffed paths: %s %s\n", src, dst);
        }
    }

    protected abstract O newOptions();

    public AbstractDiffClient(String[] args) {
        super(args);
        opts = newOptions();
        args = Option.processCommandLine(args, opts);

        if (args.length < 2)
            throw new Option.OptionException("Two arguments are required. " + SYNTAX, opts);

        opts.src = args[0];
        opts.dst = args[1];

        if (Option.Verbose.verbose) {
            opts.dump(System.out);
        }

        if (opts.treeGenerator != null && TreeGenerators.getInstance().find(opts.treeGenerator) == null)
            throw new Option.OptionException("Error loading tree generator: " + opts.treeGenerator);

        if (!Files.exists(Paths.get(opts.src)))
            throw new Option.OptionException("Error loading file or folder: " + opts.src);

        if (!Files.exists(Paths.get(opts.dst)))
            throw new Option.OptionException("Error loading file or folder: " + opts.dst);
    }

    protected Diff getDiff() throws IOException {
        return getDiff(opts.src, opts.dst);
    }

    protected Diff getDiff(String src, String dst) throws IOException {
        return Diff.compute(src, dst, opts.treeGenerator, opts.matcher);
    }
}
