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

package com.github.gumtree.dist.diggit;

import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TreeValidator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DiggitFolderProcessor {

    private static String inputDir;

    private static String outputDir;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Wrong number of arguments");
            System.exit(-1);
        }
        inputDir = args[0];
        outputDir = args[1];

        //TreeContext tc = getTreeContext("/Users/falleri/tmp/dgit/out/diff_extractor/https_github_com_GumTreeDiff_gumtree_git/4e9f7620e156a5159e5a6f1c0b93f5bc246ec7d3/src/client.diff_src_main_java_com_github_gumtreediff_client_diff_WebDiff.java");
        //System.out.println(tc.getRoot().toPrettyTreeString(tc));
        processTrees();
    }

    public static void processTrees() throws Exception {
        Files.walk(Paths.get(inputDir)).filter(Files::isRegularFile).forEach(file ->  {
            String name = file.toString();
            if (name.contains("/src/")) {
                String dst = name.replace("/src/", "/dst/");
                processPair(name, dst);
            }
        });
    }

    public static void processPair(String src, String dst) {
        System.out.println(src);
        TreeContext tcSrc = getTreeContext(src);
        //System.out.println(tcSrc.getRoot().toPrettyTreeString(tcSrc));
        new TreeValidator().validate(tcSrc);
        System.out.println(dst);
        TreeContext tcDst = getTreeContext(dst);
        new TreeValidator().validate(tcDst);
    }

    private static TreeContext getTreeContext(String file) {
        try {
           if (file.endsWith(".java"))
               return new JdtTreeGenerator().generateFromFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
