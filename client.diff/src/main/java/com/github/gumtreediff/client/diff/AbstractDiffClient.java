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
import com.github.gumtreediff.matchers.ConfigurationOptions;
import com.github.gumtreediff.matchers.GumtreeProperties;
import com.github.gumtreediff.matchers.Matchers;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public abstract class AbstractDiffClient<O extends AbstractDiffClient.DiffOptions> extends Client {
    protected final O opts;
    public static final String SYNTAX = "Syntax: [options] srcFile dstFile";

    public static class DiffOptions implements Option.Context {
        public String matcherId;
        public String treeGeneratorId;
        public String srcPath;
        public String dstPath;
        public GumtreeProperties properties = new GumtreeProperties();
        public String command = null;

        @Override
        public Option[] values() {
            return new Option[] {
                    new Option("-m", "Id of the matcher to use.", 1) {
                        @Override
                        protected void process(String name, String[] args) {
                            matcherId = args[0];
                        }
                    },
                    new Option("-g", "Id of the tree generator to use.", 1) {
                        @Override
                        protected void process(String name, String[] args) {
                            treeGeneratorId = args[0];
                        }
                    },
                    new Option("-x", "Id of the tree generator to use (of the form COMMAND $FILE).", 1) {
                        @Override
                        protected void process(String name, String[] args) {
                            command = args[0];
                        }
                    },
                    new Option("-M", "Add a matcher property (-M property value). Available: "
                            + Arrays.toString(ConfigurationOptions.values()) + ".", 2) {
                        @Override
                        protected void process(String name, String[] args) {
                            properties.put(ConfigurationOptions.valueOf(args[0]), args[1]);
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
            out.printf("Diffed paths: %s %s\n", srcPath, dstPath);
            out.printf("Tree generator id: %s\n", treeGeneratorId);
            out.printf("Matcher id: %s\n", matcherId);
            out.printf("Properties: %s\n", properties.toString());
        }
    }

    protected abstract O newOptions();

    public AbstractDiffClient(String[] args) {
        super(args);
        opts = newOptions();
        args = Option.processCommandLine(args, opts);

        if (args.length < 2)
            throw new Option.OptionException("Two arguments are required. " + SYNTAX, opts);

        opts.srcPath = args[0];
        opts.dstPath = args[1];

        if (Option.Verbose.verbose) {
            opts.dump(System.out);
        }

        if (opts.matcherId != null && Matchers.getInstance().findById(opts.matcherId) == null)
            throw new Option.OptionException("Error loading matcher: " + opts.matcherId);

        if (opts.treeGeneratorId != null && TreeGenerators.getInstance().findById(opts.treeGeneratorId) == null)
            throw new Option.OptionException("Error loading tree generator: " + opts.treeGeneratorId);

        if (!Files.exists(Paths.get(opts.srcPath)))
            throw new Option.OptionException("Error loading file or folder: " + opts.srcPath);

        if (!Files.exists(Paths.get(opts.dstPath)))
            throw new Option.OptionException("Error loading file or folder: " + opts.dstPath);
    }

    public Diff getDiff() throws IOException {
        return getDiff(opts.srcPath, opts.dstPath);
    }

    public Diff getDiff(String src, String dst) throws IOException {
        if (opts.command == null)
            return Diff.compute(src, dst, opts.treeGeneratorId, opts.matcherId, opts.properties);
        else
            return Diff.computeWithCommand(src, dst, opts.command, opts.matcherId, opts.properties);
    }
}
