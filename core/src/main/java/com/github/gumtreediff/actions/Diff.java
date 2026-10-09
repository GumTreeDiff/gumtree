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
 * Copyright 2022 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2022 Raquel Pau <raquelpau@gmail.com>
 */

package com.github.gumtreediff.actions;

import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.matchers.GumtreeProperties;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.TreeContext;

import java.io.IOException;
import java.io.Reader;

/**
 * Class to facilitate the computation of diffs between ASTs.
 */
public class Diff {
    /**
     * The source AST in its context.
     */
    public final TreeContext src;

    /**
     * The destination AST in its context.
     */
    public final TreeContext dst;

    /**
     * The mappings between the two ASTs.
     */
    public final MappingStore mappings;

    /**
     * The edit script between the two ASTs.
     */
    public final EditScript editScript;

    /**
     * Instantiate a diff object with the provided source and destination
     * ASTs, the provided mappings, and the provided editScript.
     */
    public Diff(TreeContext src, TreeContext dst,
                MappingStore mappings, EditScript editScript) {
        this.src = src;
        this.dst = dst;
        this.mappings = mappings;
        this.editScript = editScript;
    }

    /**
     * Compute and return a diff.
     * @param srcFile The path to the source file.
     * @param dstFile The path to the destination file.
     * @param treeGenerator The id of the tree generator to use.
     * @param matcher The id of the the matcher to use.
     * @param properties The set of options.
     * @throws IOException an IO exception is raised in case of IO problems related to the source
     *     or destination file.
     */
    public static Diff compute(String srcFile, String dstFile, String treeGenerator,
                               String matcher, GumtreeProperties properties) throws IOException {
        TreeContext src = TreeGenerators.getInstance().getTree(srcFile, treeGenerator);
        TreeContext dst = TreeGenerators.getInstance().getTree(dstFile, treeGenerator);

        return compute(src, dst, treeGenerator, matcher, properties);
    }

    private static Diff compute(TreeContext src, TreeContext dst, String treeGenerator,
                               String matcher, GumtreeProperties properties) throws IOException {
        Matcher m = Matchers.getInstance().getMatcherWithFallback(matcher);
        m.configure(properties);
        MappingStore mappings = m.match(src.getRoot(), dst.getRoot());
        EditScript editScript = new SimplifiedChawatheScriptGenerator().computeActions(mappings);
        return new Diff(src, dst, mappings, editScript);
    }

    /**
     * Compute and return a diff.
     * @param srcReader The reader to the source file.
     * @param dstReader The reader to the destination file.
     * @param treeGenerator The id of the tree generator to use.
     * @param matcher The id of the the matcher to use.
     * @param properties The set of options.
     * @throws IOException an IO exception is raised in case of IO problems related to the source
     *     or destination file.
     */
    public static Diff compute(Reader srcReader, Reader dstReader, String treeGenerator,
                               String matcher, GumtreeProperties properties) throws IOException {
        TreeContext src = TreeGenerators.getInstance().getTree(srcReader, treeGenerator);
        TreeContext dst = TreeGenerators.getInstance().getTree(dstReader, treeGenerator);
        return compute(src, dst, treeGenerator, matcher, properties);
    }

    /**
     * Compute and return a diff.
     * @param srcFile The path to the source file.
     * @param dstFile The path to the destination file.
     * @param command The executable command in the form: command $FILE.
     * @param matcher The id of the the matcher to use.
     * @param properties The set of options.
     * @throws IOException an IO exception is raised in case of IO problems related to the source
     *     or destination file.
     */
    public static Diff computeWithCommand(String srcFile, String dstFile, String command,
                               String matcher, GumtreeProperties properties) throws IOException {
        TreeContext src = TreeGenerators.getInstance().getTreeFromCommand(srcFile, command);
        TreeContext dst = TreeGenerators.getInstance().getTreeFromCommand(dstFile, command);
        Matcher m = Matchers.getInstance().getMatcherWithFallback(matcher);
        m.configure(properties);
        MappingStore mappings = m.match(src.getRoot(), dst.getRoot());
        EditScript editScript = new SimplifiedChawatheScriptGenerator().computeActions(mappings);
        return new Diff(src, dst, mappings, editScript);
    }

    /**
     * Compute and return a diff.
     * @param srcFile The path to the source file.
     * @param dstFile The path to the destination file.
     * @param treeGenerator The id of the tree generator to use.
     * @param matcher The id of the the matcher to use.
     * @throws IOException an IO exception is raised in case of IO problems related to the source
     *     or destination file.
     */
    public static Diff compute(String srcFile, String dstFile,
                               String treeGenerator, String matcher) throws IOException {
        return compute(srcFile, dstFile, treeGenerator, matcher, new GumtreeProperties());
    }

    /**
     * Compute and return a diff, using the default matcher and tree generators automatically
     * retrieved according to the file extensions.
     * @param srcFile The path to the source file.
     * @param dstFile The path to the destination file.
     * @throws IOException an IO exception is raised in case of IO problems related to the source
     *     or destination file.
     */
    public static Diff compute(String srcFile, String dstFile) throws IOException {
        return compute(srcFile, dstFile, null, null);
    }

    /**
     * Compute and return a all node classifier that indicates which node have
     * been added/deleted/updated/moved.
     */
    public TreeClassifier createAllNodeClassifier() {
        return new AllNodesClassifier(this);
    }

    /**
     * Compute and return a root node classifier that indicates which node have
     * been added/deleted/updated/moved. Only the root note is marked when a whole
     * subtree has been subject to a same operation.
     */
    public TreeClassifier createRootNodesClassifier() {
        return new OnlyRootsClassifier(this);
    }
}
