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

package com.github.gumtreediff.client.diff.webdiff;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.actions.ITreeClassifier;
import com.github.gumtreediff.tree.ITree;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.File;
import java.io.IOException;

import static org.rendersnake.HtmlAttributesFactory.*;

public class MonacoDiffView implements Renderable {
    private File srcFile;
    private File dstFile;

    private Diff diff;

    private int id;

    public MonacoDiffView(File fSrc, File fDst, Diff diff, int id) {
        this.srcFile = fSrc;
        this.dstFile = fDst;
        this.diff = diff;
        this.id = id;
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
    html
        .render(DocType.HTML5)
        .html(lang("en"))
            .render(new Header())
            .body()
                .div(class_("container-fluid"))
                    .div(class_("row"))
                        .render(new MenuBar())
                    ._div()
                    .div(class_("row"))
                        .div(class_("col"))
                            .h5().content(srcFile.getName())
                            .div(id("left-container").style("width:100%;height:600px;border:1px solid grey"))._div()
                        ._div()
                        .div(class_("col"))
                            .h5().content(dstFile.getName())
                            .div(id("right-container").style("width:100%;height:600px;border:1px solid grey"))._div()
                        ._div()
                    ._div()
                ._div()
                .macros().script("config = { file: \"" + srcFile.getName() + "\", left: " + getLeftJsConfig()
                                 + ", right: " + getRightJsConfig()
                                 + ", mappings: " + getMappingsJsConfig() + "};")
                .macros().javascript("/monaco/min/vs/loader.js")
                .macros().javascript("/dist/monaco.js")
                .macros().javascript("/dist/shortcuts.js")
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

    private static class MenuBar implements Renderable {

        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            html
            .div(class_("col"))
                .div(class_("btn-toolbar justify-content-end"))
                    .div(class_("btn-group mr-2"))
                        .a(class_("btn btn-primary btn-sm").id("legend").href("#").add("data-toggle", "popover")
                                .add("data-html", "true").add("data-placement", "bottom")
                                .add("data-content", "<span class=&quot;deleted&quot;>&nbsp;&nbsp;</span> deleted<br>"
                                        + "<span class=&quot;inserted&quot;>&nbsp;&nbsp;</span> inserted<br>"
                                        + "<span class=&quot;moved&quot;>&nbsp;&nbsp;</span> moved<br>"
                                        + "<span class=&quot;updated&quot;>&nbsp;&nbsp;</span> updated<br>", false)
                                .add("data-original-title", "Legend").title("Legend").role("button")).content("Legend")
                        .a(class_("btn btn-primary btn-sm").id("shortcuts").href("#").add("data-toggle", "popover")
                                .add("data-html", "true").add("data-placement", "bottom")
                                .add("data-content", "<b>q</b> quit<br><b>l</b> list<br>"
                                        + "<b>t</b> top<br><b>b</b> bottom", false)
                                .add("data-original-title", "Shortcuts").title("Shortcuts").role("button"))
                            .content("Shortcuts")
                    ._div()
                    .div(class_("btn-group"))
                        .a(class_("btn btn-default btn-sm btn-primary").href("/list")).content("Back")
                        .a(class_("btn btn-default btn-sm btn-danger").href("/quit")).content("Quit")
                    ._div()
                ._div()
            ._div();
        }
    }

    private static class Header implements Renderable {
        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            html
                .head()
                    .meta(charset("utf8"))
                    .meta(name("viewport").content("width=device-width, initial-scale=1.0"))
                    .title().content("GumTree")
                    .macros().stylesheet("https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css")
                    .macros().stylesheet("/dist/monaco.css")
                    .macros().javascript("https://code.jquery.com/jquery-3.4.1.min.js")
                    .macros().javascript("https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js")
                    .macros().javascript("https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js")
                ._head();
        }
    }
}
