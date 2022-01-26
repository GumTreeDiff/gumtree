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
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.client.diff.dotdiff;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.actions.TreeClassifier;

import com.github.gumtreediff.client.Register;
import com.github.gumtreediff.client.diff.AbstractDiffClient;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

@Register(description = "A dot diff client", options = AbstractDiffClient.DiffOptions.class)
public final class DotDiff extends AbstractDiffClient<AbstractDiffClient.DiffOptions> {
    private final Diff diff;
    private final TreeClassifier classifier;

    public DotDiff(String[] args) {
        super(args);
        try {
            this.diff = getDiff();
            classifier = diff.createAllNodeClassifier();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() throws Exception {
        StringWriter writer = new StringWriter();
        writer.write("digraph G {\n");
        writer.write("\tnode [style=filled];\n");
        writer.write("\tsubgraph cluster_src {\n");
        writeTree(diff.src, writer);
        writer.write("\t}\n");
        writer.write("\tsubgraph cluster_dst {\n");
        writeTree(diff.dst, writer);
        writer.write("\t}\n");
        for (Mapping m: diff.mappings)
            writer.write(String.format("\t%s -> %s [style=dashed];\n",
                    getDotId(diff.src, m.first), getDotId(diff.dst, m.second)));
        writer.write("}\n");
        System.out.println(writer);
    }

    private void writeTree(TreeContext context, Writer writer) throws Exception {
        for (Tree tree : context.getRoot().preOrder()) {
            String fillColor = getDotColor(tree);
            writer.write(String.format("\t\t%s [label=\"%s\", color=%s];\n",
                    getDotId(context, tree), getDotLabel(tree), fillColor));
            if (tree.getParent() != null)
                writer.write(String.format("\t\t%s -> %s;\n",
                        getDotId(context, tree.getParent()), getDotId(context, tree)));
        }
    }

    private String getDotColor(Tree tree) {
        if (classifier.getDeletedSrcs().contains(tree))
            return "red";
        else if (classifier.getInsertedDsts().contains(tree))
            return "green";
        else if (classifier.getMovedDsts().contains(tree) || classifier.getMovedSrcs().contains(tree))
            return "blue";
        else if (classifier.getUpdatedDsts().contains(tree) || classifier.getUpdatedSrcs().contains(tree))
            return "orange";
        else
            return "lightgrey";
    }

    private String getDotId(TreeContext context, Tree tree) {
        String contextStr = context == diff.src ? "src" : "dst";
        return "n_" + contextStr + "_" + tree.getMetrics().position;
    }

    private String getDotLabel(Tree tree) {
        String label = tree.toString();
        if (label.contains("\"") || label.contains("\\s"))
            label = label
                    .replaceAll("\"", "")
                    .replaceAll("\\s", "")
                    .replaceAll("\\\\", "");
        if (label.length() > 30)
            label = label.substring(0, 20);
        return label;
    }

    @Override
    protected DiffOptions newOptions() {
        return new DiffOptions();
    }
}