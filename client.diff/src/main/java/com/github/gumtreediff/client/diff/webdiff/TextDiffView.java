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

package com.github.gumtreediff.client.diff.webdiff;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.io.ActionsIoUtils;
import j2html.tags.Tag;
import j2html.tags.specialized.HtmlTag;

import static j2html.TagCreator.*;

import java.io.File;
import java.io.IOException;

public class TextDiffView {

    public static HtmlTag build(File srcFile, File dstFile, Diff diff) throws IOException {
        return html(
            Header.build(),
            body(
                div(
                    div(MenuBar.build()).withClass("row"),
                    div(
                        div(
                            h3(
                                join("Raw edit script",
                                        small(String.format("%s -> %s", srcFile.getName(), dstFile.getName()))
                            )),
                            pre(ActionsIoUtils.toText(diff.src, diff.editScript,
                                    diff.mappings).toString()).withClasses("border", "p-2")
                        ).withClass("col")
                    ).withClass("row")
                ).withClass("container-fluid")
            )
        );
    }

    private static class Header {

        public static Tag build() {
            return head(
                meta().withCharset("utf8"),
                meta().withName("viewport").withContent("width=device-width, initial-scale=1.0"),
                title("GumTree"),
                link().withRel("stylesheet").withType("text/css").withHref(WebDiff.BOOTSTRAP_CSS_URL),
                script().withType("text/javascript").withSrc(WebDiff.BOOTSTRAP_JS_URL),
                script().withType("text/javascript").withSrc("/dist/shortcuts.js")
            );
        }
    }

    private static class MenuBar  {

        public static Tag build() {
            return div(
                div(
                    div(
                        a("Back").withHref("/list").withClasses("btn btn-default", "btn-sm btn-primary"),
                        a("Quit").withHref("/quit").withClasses("btn btn-default", "btn-sm btn-danger")
                    ).withClass("btn-group")
                ).withClasses("btn-toolbar", "justify-content-end")
            ).withClass("col");
        }
    }
}
