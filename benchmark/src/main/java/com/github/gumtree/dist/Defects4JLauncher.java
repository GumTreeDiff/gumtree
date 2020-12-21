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

package com.github.gumtree.dist;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class Defects4JLauncher {
    public static void main(String[] args) throws IOException {
        System.out.println("case;algorithm;runtime;size");
        File defect4jFolder = new File("../defects4j/");
        for (File folder: defect4jFolder.listFiles())
            handleFolder(folder);
    }

    public static void handleFolder(File folder) throws IOException {
        if (folder.getName().equals(".DS_Store"))
            return;

        for (File file: folder.listFiles())
            if (file.isFile()) {
                handleCase(folder);
                break;
            }
            else
                handleFolder(file);
    }

    public static void handleCase(File folder) throws IOException {
        File src = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("_s.java");
            }
        })[0];
        File dst = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("_t.java");
            }
        })[0];
        TreeContext srcT = new JdtTreeGenerator().generateFrom().file(src);
        TreeContext dstT = new JdtTreeGenerator().generateFrom().file(dst);
        handleMatcher(src.getName(), "SimpleId", new CompositeMatchers.SimpleIdGumtree(), srcT, dstT);
        //handleMatcher(src.getName(), "SimpleIdTheta", new CompositeMatchers.SimpleIdGumtreeTheta(), srcT, dstT);
        //handleMatcher(src.getName(), "Classic", new CompositeMatchers.ClassicGumtree(), srcT, dstT);
    }

    public static void handleMatcher(String file, String matcher, Matcher m, TreeContext src, TreeContext dst) {
        long tic = System.nanoTime();
        MappingStore mappings = m.match(src.getRoot(), dst.getRoot());
        long time = System.nanoTime() - tic;
        EditScriptGenerator g = new SimplifiedChawatheScriptGenerator();
        EditScript s = g.computeActions(mappings);
        System.out.println(file + ";" + matcher + ";" + time + ";" + s.size());
    }
}
