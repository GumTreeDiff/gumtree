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
 */

package com.github.gumtree.dist;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.io.ActionsIoUtils;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ActionsCollector {

    private static final String OUTPUT_DIR = "build/tmp/actions/";

    private static final String RES_DIR = "src/jmh/resources/";

    private static final String REF_DIR = "src/main/resources/";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Wrong number of arguments.");
            System.exit(-1);
        }
        if (args[0].equals("collect"))
            collectActions();
        else if (args[0].equals("check"))
            checkActions();
        else {
            System.err.println("Unknown argument: use collect or test.");
            System.exit(-1);
        }
    }

    public static void collectActions() throws Exception {
        Run.initGenerators();
        List<Path> outputPaths = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(RES_DIR))) {
            outputPaths = paths.filter(
                p -> p.getFileName().toString().matches(".*_v0_.*\\.xml")).collect(Collectors.toList());
        }
        Files.createDirectories(Paths.get(OUTPUT_DIR));
        for (Path path : outputPaths) {
            Path otherPath = Paths.get(path.toString().replace("_v0_","_v1_"));
            Path outputPath = Paths.get(path.toString().replace("_v0_","_actions_"));
            TreeContext src = TreeIoUtils.fromXml().generateFromFile(path.toString());
            TreeContext dst = TreeIoUtils.fromXml().generateFromFile(otherPath.toString());
            CompositeMatchers.ClassicGumtree matcher = new CompositeMatchers.ClassicGumtree(
                    src.getRoot(), dst.getRoot(), new MappingStore());
            matcher.match();
            ActionGenerator g = new ActionGenerator(src.getRoot(), dst.getRoot(), matcher.getMappings());
            List<Action> actions = g.generate();

            String res = Paths.get(OUTPUT_DIR, outputPath.getFileName().toString()).toString();
            ActionsIoUtils.toText(src, actions, matcher.getMappings()).writeTo(new FileWriter(res));
        }
    }

    public static void checkActions() throws Exception {
        Run.initGenerators();
        List<Path> outputPaths = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(RES_DIR))) {
            outputPaths = paths.filter(
                p -> p.getFileName().toString().matches(".*_v0_.*\\.xml")).collect(Collectors.toList());
        }
        boolean dirty = false;
        StringBuilder b = new StringBuilder();
        for (Path path : outputPaths) {
            Path otherPath = Paths.get(path.toString().replace("_v0_","_v1_"));
            TreeContext src = TreeIoUtils.fromXml().generateFromFile(path.toString());
            TreeContext dst = TreeIoUtils.fromXml().generateFromFile(otherPath.toString());
            CompositeMatchers.ClassicGumtree matcher = new CompositeMatchers.ClassicGumtree(
                    src.getRoot(), dst.getRoot(), new MappingStore());
            matcher.match();
            ActionGenerator g = new ActionGenerator(src.getRoot(), dst.getRoot(), matcher.getMappings());
            List<Action> actions = g.generate();
            StringWriter w = new StringWriter();
            ActionsIoUtils.toText(src, actions, matcher.getMappings()).writeTo(w);
            Path refPath = Paths.get(path.toString().replace("_v0_","_actions_"));
            String ref = Paths.get(REF_DIR, refPath.getFileName().toString()).toString();
            if (!contentEquals(new StringReader(w.toString()), new FileReader(ref))) {
                dirty = true;
                b.append(String.format("Content not equals for: %s. Now: %d Was: %d\n",
                        ref, countLines(new StringReader(w.toString())), countLines(new FileReader(ref))));
            }
        }
        if (dirty) {
            System.err.println(b.toString());
            System.exit(-1);
        }
    }

    public static boolean contentEquals(Reader cur, Reader ref) throws IOException  {
        try (Reader _cur = cur; Reader _ref = ref) {
            int ch = _cur.read();
            while (-1 != ch) {
                int ch2 = _ref.read();
                if (ch != ch2)
                    return false;
                ch = _cur.read();
            }
            int ch2 = _ref.read();
            return (ch2 == -1);
        }
    }

    public static int countLines(Reader r) throws IOException {
        try (Reader _r = r) {
            char[] c = new char[1024];
            int count = 0;
            int readChars = 0;
            boolean empty = true;
            while ((readChars = _r.read(c)) != -1) {
                empty = false;
                for (int i = 0; i < readChars; ++i)
                    if (c[i] == '\n')
                        ++count;
            }
            return (count == 0 && !empty) ? 1 : count;
        }
    }

}
