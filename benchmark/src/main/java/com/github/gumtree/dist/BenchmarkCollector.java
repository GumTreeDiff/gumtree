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
import java.util.List;
import java.util.stream.Collectors;

public class BenchmarkCollector {

    private final static String OUTPUT_DIR = "benchmark/build/tmp/stresstest/";

    public static void main(String[] args) throws Exception {
        gatherData(args[0]);
    }

    public static void gatherData(String dir) throws Exception {
        Run.initGenerators();
        List<Path> paths = Files.walk(Paths.get(dir)).filter(p -> p.getFileName().toString().matches(".*_v0\\.(java|js|rb|c)")).collect(Collectors.toList());
        Files.createDirectories(Paths.get(OUTPUT_DIR));
        for (Path path : paths) {
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
