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
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.tree.TreeContext;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

public class Defects4JLauncher {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println(args.length);
            System.err.println("Wrong command. Expected arguments: INPUT_FOLDER OUTPUT_FILE. Got: "
                    + Arrays.toString(args));
            System.exit(1);
        }

        OUTPUT = new FileWriter(args[1]);
        OUTPUT.append("case;algorithm;runtime;size\n");
        File defect4jFolder = new File(args[0]);
        for (File folder: defect4jFolder.listFiles())
            handleFolder(folder);
        OUTPUT.close();
    }

    private static final String[] EXCLUDED_FOLDERS = new String[] {".DS_Store", ".", ".."};

    private static final int MAX_MEASURES = 5;

    private static FileWriter OUTPUT;

    private static MatcherConfig[] configurations = new MatcherConfig[] {
            new MatcherConfig("SimpleId", () -> new CompositeMatchers.SimpleIdGumtree()),
            //new MatcherConfig("Simple", () -> new CompositeMatchers.SimpleGumtree()),
            new MatcherConfig("HybridId", () -> new CompositeMatchers.HybridIdGumtree()),
    };

    private static void handleFolder(File folder) throws IOException {
        if (Arrays.stream(EXCLUDED_FOLDERS).anyMatch(folder.getName()::equals))
            return;
        for (File file: folder.listFiles())
            if (file.isFile()) {
                handleCase(folder);
                break;
            }
            else
                handleFolder(file);
    }

    private static void handleCase(File folder) throws IOException {
        var src = folder.listFiles((File dir, String name) ->  name.endsWith("_s.java"))[0];
        var dst = folder.listFiles((File dir, String name) ->  name.endsWith("_t.java"))[0];
        TreeContext srcT = new JdtTreeGenerator().generateFrom().file(src);
        TreeContext dstT = new JdtTreeGenerator().generateFrom().file(dst);
        for (MatcherConfig config : configurations)
            handleMatcher(src.getName(), config.name, config.matcherFactory.get(), srcT, dstT);
    }

    private static void handleMatcher(String file, String matcher, Matcher m,
            TreeContext src, TreeContext dst) throws IOException {
        long[] times = new long[MAX_MEASURES];
        MappingStore mappings = null;
        for (int i = 0; i < MAX_MEASURES ; i++) {
            long tic = System.nanoTime();
            mappings = m.match(src.getRoot(), dst.getRoot());
            long time = System.nanoTime() - tic;
            times[i] = time;
        }
        Arrays.sort(times);
        EditScriptGenerator g = new SimplifiedChawatheScriptGenerator();
        EditScript s = g.computeActions(mappings);
        int size = s.size();
        long median = (times[times.length / 2] + times[(times.length - 1) / 2]) / 2;
        OUTPUT.append(String.format("%s;%s;%d;%s\n", file, matcher, median, size));
    }

    private static class MatcherConfig {
        public final String name;
        public final Supplier<Matcher> matcherFactory;

        public MatcherConfig(String name, Supplier<Matcher> matcherFactory) {
            this.name = name;
            this.matcherFactory = matcherFactory;
        }
    }
}

