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
    public static void main(String[] args) throws Exception {
        gatherData();
    }

    public static void gatherData() throws Exception {
        Run.initGenerators();
        List<Path> paths = Files.walk(Paths.get("samples")).filter(p -> p.getFileName().toString().matches(".*_v0\\.(java|js|rb)")).collect(Collectors.toList());
        Files.createDirectories(Paths.get("dist/build/tmp/gt_perfs/"));
        for (Path path : paths) {
            Path otherPath = Paths.get(path.toString().replace("_v0.","_v1."));
            String name0 = path.toString().replaceAll("[^a-zA-Z0-9_]", "_");
            String name1 = otherPath.toString().replaceAll("[^a-zA-Z0-9_]", "_");
            TreeContext ctx = getTreeContext(path.toAbsolutePath().toString());
            TreeIoUtils.toXml(ctx).writeTo(new File("dist/build/tmp/gt_perfs/" + name0 + ".xml"));
            ctx = getTreeContext(otherPath.toAbsolutePath().toString());
            TreeIoUtils.toXml(ctx).writeTo(new File("dist/build/tmp/gt_perfs/" + name1 + ".xml"));
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
