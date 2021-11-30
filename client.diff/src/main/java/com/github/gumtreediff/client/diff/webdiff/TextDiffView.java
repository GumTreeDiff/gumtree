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
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.File;
import java.io.IOException;

import static org.rendersnake.HtmlAttributesFactory.*;

public class TextDiffView implements Renderable {
    private File srcFile;
    private File dstFile;

    private Diff diff;

    public TextDiffView(File srcFile, File dstFile, Diff diff) {
        this.srcFile = srcFile;
        this.dstFile = dstFile;
        this.diff = diff;
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
                                .h3()
                                    .write("Raw edit script ")
                                    .small().content(String.format("%s -> %s", srcFile.getName(), dstFile.getName()))
                                ._h3()
                                .pre(class_("border p-2")).content(ActionsIoUtils.toText(diff.src, diff.editScript, diff.mappings).toString())
                            ._div()
                        ._div()
                    ._div()
                ._body()
            ._html();
    }

    private static class Header implements Renderable {
        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
             html
                     .head()
                        .meta(charset("utf8"))
                        .meta(name("viewport").content("width=device-width, initial-scale=1.0"))
                        .title().content("GumTree")
                        .macros().stylesheet(WebDiff.BOOTSTRAP_CSS_URL)
                        .macros().javascript(WebDiff.BOOTSTRAP_JS_URL)
                        .macros().javascript("/dist/shortcuts.js")
                     ._head();
        }
    }

    private static class MenuBar implements Renderable {
        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            html
                    .div(class_("col"))
                        .div(class_("btn-toolbar justify-content-end"))
                            .div(class_("btn-group"))
                                .a(class_("btn btn-default btn-sm btn-primary").href("/list")).content("Back")
                                .a(class_("btn btn-default btn-sm btn-danger").href("/quit")).content("Quit")
                            ._div()
                        ._div()
                    ._div();
        }
    }
}
