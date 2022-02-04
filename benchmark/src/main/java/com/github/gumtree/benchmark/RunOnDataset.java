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
 * Copyright 2020 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtree.benchmark;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.gen.python.PythonTreeGenerator;
import com.github.gumtreediff.io.DirectoryComparator;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

public class RunOnDataset {
    private static final int TIME_MEASURES = 5;

    private static FileWriter OUTPUT;

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println(args.length);
            System.err.println("Wrong command. Expected arguments: INPUT_FOLDER OUTPUT_FILE. Got: "
                    + Arrays.toString(args));
            System.exit(1);
        }
        TreeGenerators.getInstance().install(
                JdtTreeGenerator.class, JdtTreeGenerator.class.getAnnotation(Register.class));
        TreeGenerators.getInstance().install(
                PythonTreeGenerator.class, PythonTreeGenerator.class.getAnnotation(Register.class));
        OUTPUT = new FileWriter(args[1]);
        OUTPUT.append("case;algorithm;t1;t2;t3;t4;t5;size\n");
        DirectoryComparator comparator = new DirectoryComparator(args[0] + "/buggy", args[0] + "/fixed");
        comparator.compare();
        int done = 0;
        int size = comparator.getModifiedFiles().size();
        for (Pair<File, File> pair : comparator.getModifiedFiles()) {
            done++;
            int pct = (int) (((float) done / (float) size) * 100);
            System.out.printf("\r%s %s  Done", displayBar(pct), pct);
            handleCase(pair.first, pair.second);
        }
        OUTPUT.close();
    }

    private static final MatcherConfig[] configurations = new MatcherConfig[] {
            new MatcherConfig("SimpleId", CompositeMatchers.SimpleIdGumtree::new),
            //new MatcherConfig("Simple", () -> new CompositeMatchers.SimpleGumtree()),
            //new MatcherConfig("HybridId", () -> new CompositeMatchers.HybridIdGumtree()),
    };

    private static void handleCase(File src, File dst) throws IOException {
        TreeContext srcT = TreeGenerators.getInstance().getTree(src.getAbsolutePath());
        TreeContext dstT = TreeGenerators.getInstance().getTree(dst.getAbsolutePath());
        for (MatcherConfig config : configurations)
            handleMatcher(src.getName(), config.name, config.matcherFactory.get(), srcT, dstT);
    }

    private static void handleMatcher(String file, String matcher, Matcher m,
            TreeContext src, TreeContext dst) throws IOException {
        long[] times = new long[TIME_MEASURES];
        MappingStore mappings = null;
        for (int i = 0; i < TIME_MEASURES; i++) {
            long startedTime = System.nanoTime();
            mappings = m.match(src.getRoot(), dst.getRoot());
            long elapsedTime = System.nanoTime() - startedTime;
            times[i] = elapsedTime;
        }
        Arrays.sort(times);
        EditScriptGenerator g = new SimplifiedChawatheScriptGenerator();
        EditScript s = g.computeActions(mappings);
        int size = s.size();
        OUTPUT.append(String.format("%s;%s;%d;%d;%d;%d;%d;%d\n", file, matcher,
                times[0], times[1], times[2], times[3], times[4], size));
    }

    private static class MatcherConfig {
        public final String name;
        public final Supplier<Matcher> matcherFactory;

        public MatcherConfig(String name, Supplier<Matcher> matcherFactory) {
            this.name = name;
            this.matcherFactory = matcherFactory;
        }
    }

    private static String displayBar(int i) {
        StringBuilder sb = new StringBuilder();

        int x = i / 2;
        sb.append("|");
        for (int k = 0; k < 50; k++)
            sb.append(String.format("%s", ((x <= k) ? " " : "=")));
        sb.append("|");

        return sb.toString();
    }
}

