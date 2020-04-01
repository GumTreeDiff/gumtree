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

package com.github.gumtreediff.client.diff.web;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.ITreeClassifier;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.File;
import java.io.IOException;

import static org.rendersnake.HtmlAttributesFactory.*;

public class MonacoDiffView implements Renderable {
    private File fSrc;
    private File fDst;

    private int id;

    private Diff diff;

    public MonacoDiffView(File fSrc, File fDst, TreeContext src, TreeContext dst,
                          Matcher matcher, EditScriptGenerator scriptGenerator, int id) {
        this.fSrc = fSrc;
        this.fDst = fDst;
        this.id = id;
        MappingStore mappings = matcher.match(src.getRoot(), dst.getRoot());
        this.diff = new Diff(src, dst, mappings, scriptGenerator.computeActions(mappings));
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
    html
        .render(DocType.HTML5)
        .html(lang("en"))
            .render(new BootstrapHeaderView())
            .body()
                .div(class_("container-fluid"))
                    .div(class_("row"))
                        .render(new MenuBarView())
                    ._div()
                    .div(class_("row"))
                        .div(class_("col-6"))
                            .h5().content(fSrc.getName())
                            .div(id("left-container").style("width:100%;height:600px;border:1px solid grey"))._div()
                        ._div()
                        .div(class_("col-6"))
                            .h5().content(fDst.getName())
                            .div(id("right-container").style("width:100%;height:600px;border:1px solid grey"))._div()
                        ._div()
                    ._div()
                ._div()
                .macros().script("config = { left: " + getLeftJsConfig()
                                 + ", right: " + getRightJsConfig()
                                 + ", mappings: " + getMappingsJsConfig() + "};")
                .macros().javascript("/monaco/min/vs/loader.js")
                .macros().javascript("/dist/monaco.js")
                .macros().javascript("/dist/script.js")
            ._body()
        ._html();
    }

    private String getLeftJsConfig() {
        ITreeClassifier c = diff.createRootNodesClassifier();
        StringBuilder b = new StringBuilder();
        b.append("{");
        b.append("url:").append("\"/left/" + id + "\"").append(",");
        b.append("ranges: [");
        for (ITree t: diff.src.getRoot().preOrder()) {
            if (c.getMovedSrcs().contains(t))
                appendRange(b, t, "moved");
            if (c.getUpdatedSrcs().contains(t))
                appendRange(b, t, "updated");
            if (c.getDeletedSrcs().contains(t))
                appendRange(b, t, "deleted");
        }
        b.append("]").append(",");
        b.append("}");
        return b.toString();
    }

    private String getRightJsConfig() {
        ITreeClassifier c = diff.createRootNodesClassifier();
        StringBuilder b = new StringBuilder();
        b.append("{");
        b.append("url:").append("\"/right/" + id + "\"").append(",");
        b.append("ranges: [");
        for (ITree t: diff.dst.getRoot().preOrder()) {
            if (c.getMovedDsts().contains(t))
                appendRange(b, t, "moved");
            if (c.getUpdatedDsts().contains(t))
                appendRange(b, t, "updated");
            if (c.getInsertedDsts().contains(t))
                appendRange(b, t, "inserted");
        }
        b.append("]").append(",");
        b.append("}");
        return b.toString();
    }

    private String getMappingsJsConfig() {
        ITreeClassifier c = diff.createRootNodesClassifier();
        StringBuilder b = new StringBuilder();
        b.append("[");
        for (ITree t: diff.src.getRoot().preOrder()) {
            if (c.getMovedSrcs().contains(t) || c.getUpdatedSrcs().contains(t)) {
                ITree d = diff.mappings.getDstForSrc(t);
                b.append(String.format("[%s, %s, %s, %s], ", t.getPos(), t.getEndPos(), d.getPos(), d.getEndPos()));
            }
        }
        b.append("]").append(",");
        return b.toString();
    }

    private void appendRange(StringBuilder b, ITree t, String kind) {
        b.append("{")
                .append("from: ").append(t.getPos())
                .append(",").append("to: ").append(t.getEndPos()).append(",")
                .append("kind: ").append("\"" + kind + "\"").append(",")
                .append("tooltip: ").append("\"" + tooltip(t) + "\"").append(",")
                .append("}").append(",");
    }

    private static String tooltip(ITree t) {
        return (t.getParent() != null)
                ? t.getParent().getType() + "/" + t.getType() : t.getType().toString();
    }
}
