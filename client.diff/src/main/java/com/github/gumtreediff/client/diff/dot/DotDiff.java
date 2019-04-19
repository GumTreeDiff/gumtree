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

package com.github.gumtreediff.client.diff.dot;

import com.github.gumtreediff.client.Register;
import com.github.gumtreediff.client.diff.AbstractDiffClient;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import java.io.StringWriter;
import java.io.Writer;

@Register(description = "A dot diff client", options = AbstractDiffClient.Options.class)
public final class DotDiff extends AbstractDiffClient<AbstractDiffClient.Options> {

    public DotDiff(String[] args) {
        super(args);
    }

    @Override
    public void run() {
        final MappingStore mappings = matchTrees();
        try {
            StringWriter writer = new StringWriter();
            writer.write("digraph G {\n");
            writer.write("node [style=filled];\n");
            writer.write("subgraph cluster_src {\n");
            writeTree(getSrcTreeContext(), writer, mappings);
            writer.write("}\n");
            writer.write("subgraph cluster_dst {\n");
            writeTree(getDstTreeContext(), writer, mappings);
            writer.write("}\n");
            for (Mapping m: mappings) {
                writer.write(String.format("%s -> %s [style=dashed]\n;",
                        getDotId(getSrcTreeContext(), m.first), getDotId(getDstTreeContext(), m.second)));
            }
            writer.write("}\n");
            System.out.println(writer.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeTree(TreeContext context, Writer writer, MappingStore mappings) throws Exception {
        for (ITree tree : context.getRoot().preOrder()) {
            String fillColor = "red";
            if (mappings.isSrcMapped(tree) || mappings.isDstMapped(tree))
                fillColor = "blue";
            writer.write(String.format("%s [label=\"%s\", color=%s];\n",
                    getDotId(context, tree), getDotLabel(context, tree), fillColor));
            if (tree.getParent() != null)
                writer.write(String.format("%s -> %s;\n",
                        getDotId(context, tree.getParent()), getDotId(context, tree)));
        }

    }

    private String getDotId(TreeContext context, ITree tree) {
        return "n_" + context.hashCode() + "_" + tree.hashCode();
    }

    private String getDotLabel(TreeContext context, ITree tree) {
        String label = tree.toString();
        if (label.contains("\"") || label.contains("\\s"))
            label = label
                    .replaceAll("\"", "")
                    .replaceAll("\\s", "")
                    .replaceAll("\\\\", "");
        if (label.length() > 30)
            label = label.substring(0, 30);
        return label;
    }

    @Override
    protected Options newOptions() {
        return new Options();
    }
}