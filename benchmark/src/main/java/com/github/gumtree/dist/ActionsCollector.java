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
import java.util.List;
import java.util.stream.Collectors;

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
        List<Path> paths = Files.walk(Paths.get(RES_DIR)).filter(
                p -> p.getFileName().toString().matches(".*_v0_.*\\.xml")).collect(Collectors.toList());
        Files.createDirectories(Paths.get(OUTPUT_DIR));
        for (Path path : paths) {
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
        List<Path> paths = Files.walk(Paths.get(RES_DIR)).filter(
                p -> p.getFileName().toString().matches(".*_v0_.*\\.xml")).collect(Collectors.toList());
        for (Path path : paths) {
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
            StringReader r = new StringReader(w.toString());
            if (!contentEquals(r, new FileReader(ref))) {
                System.err.println("Content not equals for : " + ref);
                System.exit(-1);
            }
        }
    }

    public static boolean contentEquals(Reader input1, Reader input2) throws IOException  {
        if (!(input1 instanceof BufferedReader))
            input1 = new BufferedReader(input1);
        if (!(input2 instanceof BufferedReader))
            input2 = new BufferedReader(input2);
        int ch = input1.read();
        while (-1 != ch) {
            int ch2 = input2.read();
            if (ch != ch2)
                return false;
            ch = input1.read();
        }
        int ch2 = input2.read();
        return (ch2 == -1);
    }

}
