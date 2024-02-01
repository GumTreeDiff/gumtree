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

import j2html.tags.Tag;
import j2html.tags.specialized.HtmlTag;

import java.io.File;

import static j2html.TagCreator.*;

public class MonacoNativeDiffView {

    public static HtmlTag build(File srcFile, File dstFile, int id) {
        return html(
            Header.build(),
            body(
                div(
                    div(MenuBar.build()).withClass("row")
                ).withClass("container-fluid"),
                div(
                    div(
                        h5(srcFile.getName() + " -> " + dstFile.getName()),
                        div().withId("container").withStyle("width:100%;height:calc(100% - 80px);border:1px solid grey")
                    ).withClasses("col", "h-100")
                ).withClasses("row", "h-100")
            ).withClass("h-100").withStyle("overflow: hidden;"),
            script("config = { left: " + getLeftJsConfig(id)
                    + ", right: " + getRightJsConfig(id)
                    + "};").withType("text/javascript"),
            script().withSrc("/monaco/min/vs/loader.js").withType("text/javascript"),
            script().withSrc("/dist/monaco-native.js").withType("text/javascript")
        ).withLang("en").withClass("h-100");
    }

    private static String getLeftJsConfig(int id) {
        StringBuilder b = new StringBuilder();
        b.append("{");
        b.append("url:").append("\"/left/" + id + "\"").append(",");
        b.append("}");
        return b.toString();
    }

    private static String getRightJsConfig(int id) {
        StringBuilder b = new StringBuilder();
        b.append("{");
        b.append("url:").append("\"/right/" + id + "\"").append(",");
        b.append("}");
        return b.toString();
    }

    private static class MenuBar  {

        public static Tag build() {
            return div(
                div(
                    div(
                        a("Back").withHref("/list").withClasses("btn", "btn-default", "btn-sm", "btn-primary"),
                        a("Quit").withHref("/quit").withClasses("btn", "btn-default", "btn-sm", "btn-danger")
                    ).withClass("btn-group")
                ).withClasses("btn-toolbar", "justify-content-end")
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
                script().withType("text/javascript").withSrc(WebDiff.JQUERY_JS_URL),
                script().withType("text/javascript").withSrc(WebDiff.BOOTSTRAP_JS_URL),
                script().withType("text/javascript").withSrc("/dist/shortcuts.js")
            );
        }
    }
}
