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

import com.github.gumtreediff.client.Option;
import com.github.gumtreediff.client.Client;
import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.TreeContext;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

public abstract class AbstractDiffClient<O extends AbstractDiffClient.Options> extends Client {

    protected final O opts;
    public static final String SYNTAX = "Syntax: diff [options] baseFile destFile";
    private TreeContext src;
    private TreeContext dst;

    public static class Options implements Option.Context {
        public String matcher;
        public ArrayList<String> generators = new ArrayList<>();
        public String src;
        public String dst;

        @Override
        public Option[] values() {
            return new Option[] {
                    new Option("-m", "The qualified name of the class implementing the matcher.", 1) {
                        @Override
                        protected void process(String name, String[] args) {
                            matcher = args[0];
                        }
                    },
                    new Option("-g", "Preferred generator to use (can be used more than once).", 1) {
                        @Override
                        protected void process(String name, String[] args) {
                            generators.add(args[0]);
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
            out.printf("Current path: %s\n", System.getProperty("user.dir"));
            out.printf("Diff: %s %s\n", src, dst);
        }
    }

    protected abstract O newOptions();

    public AbstractDiffClient(String[] args) {
        super(args);
        opts = newOptions();
        args = Option.processCommandLine(args, opts);

        if (args.length < 2)
            throw new Option.OptionException("arguments required." + SYNTAX, opts);

        opts.src = args[0];
        opts.dst = args[1];

        if (Option.Verbose.verbose) {
            opts.dump(System.out);
        }
    }

    ///////////////////
    // TODO after this line it should be rewrote in a better way
    private Matcher matcher;

    protected Matcher matchTrees() {
        Matchers matchers = Matchers.getInstance();
        if (matcher != null)
            return matcher;
        matcher = (opts.matcher == null)
                ? matchers.getMatcher(getSrcTreeContext().getRoot(), getDstTreeContext().getRoot())
                : matchers.getMatcher(opts.matcher, getSrcTreeContext().getRoot(), getDstTreeContext().getRoot());
        matcher.match();
        return matcher;
    }

    protected TreeContext getSrcTreeContext() {
        if (src == null)
            src = getTreeContext(opts.src);
        return src;
    }

    protected TreeContext getDstTreeContext() {
        if (dst == null)
            dst = getTreeContext(opts.dst);
        return dst;
    }

    private TreeContext getTreeContext(String file) {
        try {
            TreeContext t;
            if (opts.generators.isEmpty())
                t = Generators.getInstance().getTree(file);
            else
                t = Generators.getInstance().getTree(opts.generators.get(0), file);
            return t;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
