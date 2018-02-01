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

import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.TreeContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BenchmarkCollector {

    private static final String OUTPUT_DIR = "build/tmp/trees/";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Wrong number of arguments");
            System.exit(-1);
        }
        collectTrees(args[0]);
    }

    public static void collectTrees(String dir) throws Exception {
        Run.initGenerators();
        List<Path> outputPaths = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(dir))) {
            outputPaths = paths.filter(
                    p -> p.getFileName().toString().matches(".*_v0\\.(java|js|rb|c)")
            ).collect(Collectors.toList());
        }
        Files.createDirectories(Paths.get(OUTPUT_DIR));
        for (Path path : outputPaths) {
            Path otherPath = Paths.get(path.toString().replace("_v0.","_v1."));
            String oldName = path.toString().replaceAll("[^a-zA-Z0-9_]", "_");
            String newName = otherPath.toString().replaceAll("[^a-zA-Z0-9_]", "_");
            TreeContext ctx = getTreeContext(path.toAbsolutePath().toString());
            TreeIoUtils.toXml(ctx).writeTo(new File(OUTPUT_DIR + oldName + ".xml"));
            ctx = getTreeContext(otherPath.toAbsolutePath().toString());
            TreeIoUtils.toXml(ctx).writeTo(new File(OUTPUT_DIR + newName + ".xml"));
        }
    }

    private static TreeContext getTreeContext(String file) {
        try {
            TreeContext t = Generators.getInstance().getTree(file);
            return t;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
