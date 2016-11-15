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
 * Copyright 2016 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2016 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.client;

import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.utils.Couple;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.merge.Pcs;
import com.github.gumtreediff.tree.merge.PcsMerge;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

@Register(description = "A simple tree merger", options = Merge.Options.class)
public class Merge extends Client {

    public static final String SYNTAX = "Syntax: merge [options] baseFile leftFile rightFile";
    protected final Options opts;
    private TreeContext base;
    private TreeContext left;
    private TreeContext right;

    public static class Options implements Option.Context {
        protected String matcher;
        protected ArrayList<String> generators = new ArrayList<>();
        protected String base;
        protected String left;
        protected String right;
        protected String root;

        public String fileName(String name) {
            // FIXME: this code needs to be removed.
            /*
            if (new File(name).isAbsolute())
                return name;
            return root + File.separator + name;
            */
            return name;
        }

        @Override
        public Option[] values() {
            return new Option[]{
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
                    new Option("-r", "Preprent root to all (non absolute) file names", 1)  {
                        @Override
                        protected void process(String name, String[] args) {
                            root = args[0];
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
            out.printf("Merge: %s %s %s\n", base, left, right);
        }
    }

    public Merge(String[] args) {
        super(args);
        opts = new Options();
        args = Option.processCommandLine(args, opts);

        if (args.length < 3)
            throw new Option.OptionException("arguments required." + SYNTAX, opts);

        opts.base = args[0];
        opts.left = args[1];
        opts.right = args[2];

        if (Option.Verbose.verbose) {
            opts.dump(System.out);
        }
    }

    @Override
    public void run() throws Exception {
        base = getTreeContext(opts.base);
        left = getTreeContext(opts.left);
        right = getTreeContext(opts.right);

        TreeContext fakeContext = new TreeContext().merge(base).merge(left).merge(right);

        final Matcher matchLeft = matchTrees(base, left);
        final Matcher matchRight = matchTrees(base, right);

        PcsMerge merge = new PcsMerge(base, left, right, matchLeft, matchRight);

        Set<Couple<Pcs, Pcs>> inconsistencies = merge.computeMerge();
        System.out.printf("Inconsistencies count: %d\n", inconsistencies.size());
        for (Couple<Pcs, Pcs> inconsistency : inconsistencies) {
            System.out.println(Pcs.inspect(inconsistency, fakeContext));
        }
    }

    protected Matcher matchTrees(TreeContext src, TreeContext dst) {
        Matchers matchers = Matchers.getInstance();
        Matcher matcher = (opts.matcher == null)
                ? matchers.getMatcher(src.getRoot(), dst.getRoot())
                : matchers.getMatcher(opts.matcher, src.getRoot(), dst.getRoot());
        matcher.match();
        return matcher;
    }

    private TreeContext getTreeContext(String file) {
        try {
            TreeContext t = Generators.getInstance().getTree(opts.fileName(file));
            return t;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}