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
import com.github.gumtreediff.actions.TreeClassifier;
import com.github.gumtreediff.tree.Tree;
import j2html.tags.Tag;
import j2html.tags.specialized.HtmlTag;

import java.io.File;

import static j2html.TagCreator.*;

public class MonacoDiffView {

    public static HtmlTag build(File srcFile, File dstFile, Diff diff, int id) {
        return html(
            Header.build(),
            body(
                div(
                    div(MenuBar.build()).withClass("row"),
                    div(
                        div(
                            h5(srcFile.getName()),
                            div().withId("left-container").withStyle("height: calc(100% - 80px); border:1px solid grey;")
                        ).withClasses("col-6 h-100"),
                        div(
                            h5(dstFile.getName()),
                            div().withId("right-container").withStyle("height: calc(100% - 80px); border:1px solid grey;")
                        ).withClasses("col-6", "h-100")
                    ).withClasses("row", "h-100")
                ).withClasses("container-fluid", "h-100"),
                script("config = { file: \"" + srcFile.getName() + "\", left: " + getLeftJsConfig(diff, id)
                        + ", right: " + getRightJsConfig(diff, id)
                        + ", mappings: " + getMappingsJsConfig(diff) + "};").withType("text/javascript"),
                script().withSrc("/monaco/min/vs/loader.js").withType("text/javascript"),
                script().withSrc("/dist/monaco.js").withType("text/javascript"),
                script().withSrc("/dist/shortcuts.js").withType("text/javascript")
            ).withClass("h-100").withStyle("overflow: hidden;")
        ).withLang("en").withClass("h-100");
    }

    private static String getLeftJsConfig(Diff diff, int id) {
        TreeClassifier c = diff.createRootNodesClassifier();
        StringBuilder b = new StringBuilder();
        b.append("{");
        b.append("url:").append("\"/left/" + id + "\"").append(",");
        b.append("ranges: [");
        for (Tree t: diff.src.getRoot().preOrder()) {
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

    private static String getRightJsConfig(Diff diff, int id) {
        TreeClassifier c = diff.createRootNodesClassifier();
        StringBuilder b = new StringBuilder();
        b.append("{");
        b.append("url:").append("\"/right/" + id + "\"").append(",");
        b.append("ranges: [");
        for (Tree t: diff.dst.getRoot().preOrder()) {
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

    private static String getMappingsJsConfig(Diff diff) {
        TreeClassifier c = diff.createRootNodesClassifier();
        StringBuilder b = new StringBuilder();
        b.append("[");
        for (Tree t: diff.src.getRoot().preOrder()) {
            if (c.getMovedSrcs().contains(t) || c.getUpdatedSrcs().contains(t)) {
                Tree d = diff.mappings.getDstForSrc(t);
                b.append(String.format("[%s, %s, %s, %s], ", t.getPos(), t.getEndPos(), d.getPos(), d.getEndPos()));
            }
        }
        b.append("]").append(",");
        return b.toString();
    }

    private static void appendRange(StringBuilder b, Tree t, String kind) {
        b.append("{")
                .append("from: ").append(t.getPos())
                .append(",").append("to: ").append(t.getEndPos()).append(",")
                .append("index: ").append(t.getMetrics().depth).append(",")
                .append("kind: ").append("\"" + kind + "\"").append(",")
                .append("tooltip: ").append("\"" + tooltip(t) + "\"").append(",")
                .append("}").append(",");
    }

    private static String tooltip(Tree t) {
        return (t.getParent() != null)
                ? t.getParent().getType() + "/" + t.getType() : t.getType().toString();
    }

    private static class MenuBar {

        public static Tag build() {
            return div(
                div(
                    div(
                        rawHtml("<button class=\"btn btn-primary btn-sm\" id=\"legend\" data-bs-toggle=\"popover\" data-bs-placement=\"bottom\" " +
                                    "data-bs-html=\"true\" data-bs-content=\"<span class='deleted'>&nbsp;&nbsp;</span> deleted<br><span class='inserted'>&nbsp;&nbsp;</span> added<br><span class='moved'>&nbsp;&nbsp;</span> moved<br><span class='updated';>&nbsp;&nbsp;</span> updated<br>\">Legend</button>"),
                        rawHtml("<button class=\"btn btn-primary btn-sm\" id=\"shortcuts\" data-bs-toggle=\"popover\" data-bs-placement=\"bottom\" " +
                                "data-bs-html=\"true\" data-bs-content=\"<b>q</b> quit<br><b>l</b> list<br><b>n</b> next<br><b>t</b> top<br><b>b</b> bottom\">Shortcuts</button>")
                    ).withClass("btn-group mr-2"),
                    div(
                        a("Back").withHref("/list").withClasses("btn", "btn-default", "btn-sm", "btn-primary"),
                        a("Quit").withHref("/quit").withClasses("btn", "btn-default", "btn-sm", "btn-danger")
                    ).withClass("btn-group")
                ).withClasses("btn-toolbar","justify-content-end")
            ).withClass("col");
        }
    }

    private static class Header {

        public static Tag build() {
            return head(
                meta().withCharset("utf8"),
                meta().withName("viewport").withContent("width=device-width, initial-scale=1.0"),
                title("GumTree"),
                link().withRel("stylesheet").withType("text/css").withHref(WebDiff.BOOTSTRAP_CSS_URL),
                link().withRel("stylesheet").withType("text/css").withHref("/dist/monaco.css"),
                script().withType("text/javascript").withSrc(WebDiff.JQUERY_JS_URL),
                script().withType("text/javascript").withSrc(WebDiff.BOOTSTRAP_JS_URL)
            );
        }
    }
}
