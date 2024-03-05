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
import j2html.tags.Tag;
import j2html.tags.specialized.HtmlTag;

import static j2html.TagCreator.*;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;

public class VanillaDiffView {

    public static HtmlTag build(File srcFile, File dstFile, Diff diff, boolean dump) throws IOException  {
        var rawHtmlDiff = new VanillaDiffHtmlBuilder(srcFile, dstFile, diff);
        rawHtmlDiff.produce();
        return html(
            Header.build(dump),
            body(
                div(
                    div(MenuBar.build()).withClass("row"),
                    div(
                        div(
                            h5(srcFile.getName()),
                            pre(rawHtml(rawHtmlDiff.getSrcDiff())).withClass("pre-scrollable")
                        ).withClass("col-6"),
                        div(
                            h5(dstFile.getName()),
                            pre(rawHtml(rawHtmlDiff.getDstDiff())).withClass("pre-scrollable")
                        ).withClass("col-6")
                    ).withClass("row")
                ).withClass("container-fluid")
            )
        ).withLang("en");
    }

    private static class MenuBar {

        public static Tag build() {
            return div(
                div(
                    div(
                        rawHtml("<button class=\"btn btn-primary btn-sm\" id=\"legend\" data-bs-toggle=\"popover\" data-bs-placement=\"bottom\" " +
                                "data-bs-html=\"true\" data-bs-content=\"<span class='del'>&nbsp;&nbsp;</span> deleted<br><span class='add'>&nbsp;&nbsp;</span> added<br><span class='mv'>&nbsp;&nbsp;</span> moved<br><span class='upd';>&nbsp;&nbsp;</span> updated<br>\">Legend</button>"),
                        rawHtml("<button class=\"btn btn-primary btn-sm\" id=\"shortcuts\" data-bs-toggle=\"popover\" data-bs-placement=\"bottom\" " +
                                "data-bs-html=\"true\" data-bs-content=\"<b>q</b> quit<br><b>l</b> list<br><b>n</b> next<br><b>t</b> top<br><b>b</b> bottom\">Shortcuts</button>")
                    ).withClass("btn-group mr-2"),
                    div(
                        a("Back").withHref("/list").withClasses("btn", "btn-default", "btn-sm", "btn-primary"),
                        a("Quit").withHref("/quit").withClasses("btn", "btn-default", "btn-sm", "btn-danger")
                    ).withClass("btn-group")
                ).withClasses("btn-toolbar", "justify-content-end")
            ).withClass("col");
        }
    }

    private static class Header {

        public static Tag build(boolean dump) throws IOException {
            if (!dump) {
                return head(
                   meta().withCharset("utf8"),
                   meta().withName("viewport").withContent("width=device-width, initial-scale=1.0"),
                   title("GumTree"),
                   link().withRel("stylesheet").withType("text/css").withHref(WebDiff.BOOTSTRAP_CSS_URL),
                   link().withRel("stylesheet").withType("text/css").withHref("/dist/vanilla.css"),
                   script().withType("text/javascript").withSrc(WebDiff.JQUERY_JS_URL),
                   script().withType("text/javascript").withSrc(WebDiff.BOOTSTRAP_JS_URL),
                   script().withType("text/javascript").withSrc("/dist/shortcuts.js"),
                   script().withType("text/javascript").withSrc("/dist/vanilla.js")
                );
            }
            else {
                return head(
                    meta().withCharset("utf8"),
                    meta().withName("viewport").withContent("width=device-width, initial-scale=1.0"),
                    title("GumTree"),
                    link().withRel("stylesheet").withType("text/css").withHref(WebDiff.BOOTSTRAP_CSS_URL),
                    style(readFile("web/dist/vanilla.css")).withType("text/css"),
                    script().withType("text/javascript").withSrc(WebDiff.JQUERY_JS_URL),
                    script().withType("text/javascript").withSrc(WebDiff.BOOTSTRAP_JS_URL),
                    script(readFile("web/dist/shortcuts.js")).withType("text/javascript"),
                    script(readFile("web/dist/vanilla.js")).withType("text/javascript")
                );
            }
        }

        private static String readFile(String resourceName)  throws IOException {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream inputStream = classloader.getResourceAsStream(resourceName);
            InputStreamReader streamReader = new InputStreamReader(inputStream, Charset.defaultCharset());
            BufferedReader reader = new BufferedReader(streamReader);
            String content = "";
            for (String line; (line = reader.readLine()) != null;) {
                content += line + "\n";
            }
            return content;
        }
    }
}
