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

import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.io.DirectoryComparator;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.rendersnake.HtmlAttributesFactory.*;

public class DirectoryDiffView implements Renderable {
    private DirectoryComparator comparator;

    public DirectoryDiffView(DirectoryComparator comparator) throws IOException {
        this.comparator = comparator;
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
                    .div(class_("row mt-3 mb-3"))
                        .div(class_("col"))
                            .div(class_("card"))
                                .div(class_("card-header"))
                                    .h4(class_("card-title mb-0"))
                                        .write("Modified files ")
                                        .span(class_("badge badge-secondary")).content(comparator.getModifiedFiles().size())
                                    ._h4()
                                ._div()
                                .render_if(new ModifiedFiles(comparator.getModifiedFiles()), comparator.getModifiedFiles().size() > 0)
                            ._div()
                        ._div()
                    ._div()
                    .div(class_("row mb-3"))
                        .div(class_("col"))
                            .div(class_("card"))
                                .div(class_("card-header bg-danger"))
                                    .h4(class_("card-title mb-0"))
                                        .write("Deleted files ")
                                        .span(class_("badge badge-secondary")).content(comparator.getDeletedFiles().size())
                                    ._h4()
                                ._div()
                                .render_if(new AddedOrDeletedFiles(comparator.getDeletedFiles(), comparator.getSrc()),
                comparator.getDeletedFiles().size() > 0)
                            ._div()
                        ._div()
                        .div(class_("col"))
                            .div(class_("card"))
                                .div(class_("card-header bg-success"))
                                    .h4(class_("card-title mb-0"))
                                        .write("Added files ")
                                        .span(class_("badge badge-secondary")).content(comparator.getAddedFiles().size())
                                    ._h4()
                                ._div()
                                .render_if(new AddedOrDeletedFiles(comparator.getAddedFiles(), comparator.getDst()),
                comparator.getAddedFiles().size() > 0)
                            ._div()
                        ._div()
                    ._div()
                ._div()
            ._body()
        ._html();
    }

    private class ModifiedFiles implements Renderable {
        private List<Pair<File, File>> files;

        private ModifiedFiles(List<Pair<File, File>> files) {
            this.files = files;
        }

        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            HtmlCanvas tbody = html
            .table(class_("table card-table table-striped table-condensed mb-0"))
                .tbody();



            int id = 0;
            for (Pair<File, File> file : files) {
                tbody
                .tr()
                    .td().content(comparator.getSrc().toAbsolutePath().relativize(file.first.toPath().toAbsolutePath()).toString())
                    .td()
                        .div(class_("btn-toolbar justify-content-end"))
                            .div(class_("btn-group"))
                                //TODO: integrate this with the -g option
                                .if_(TreeGenerators.getInstance().hasGeneratorForFile(file.first.getAbsolutePath()))
                                    .a(class_("btn btn-primary btn-sm").href("/monaco-diff/" + id)).content("monaco")
                                    .a(class_("btn btn-primary btn-sm").href("/vanilla-diff/" + id)).content("classic")
                                ._if()
                                .a(class_("btn btn-primary btn-sm").href("/monaco-native-diff/" + id)).content("monaco-native")
                                .a(class_("btn btn-primary btn-sm").href("/mergely-diff/" + id)).content("mergely")
                                .a(class_("btn btn-primary btn-sm").href("/raw-diff/" + id)).content("raw")
                            ._div()
                        ._div()
                    ._td()
                ._tr();
                id++;
            }
            tbody
                ._tbody()
                ._table();
        }
    }

    private static class AddedOrDeletedFiles implements Renderable {
        private Set<File> files;
        private Path root;

        private AddedOrDeletedFiles(Set<File> files, Path root) {
            this.files = files;
            this.root = root;
        }

        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            HtmlCanvas tbody = html
            .table(class_("table card-table table-striped table-condensed mb-0"))
                .tbody();
            for (File file : files) {
                tbody
                    .tr()
                        .td().content(root.relativize(file.toPath()).toString())
                    ._tr();
            }
            tbody
                ._tbody()
            ._table();
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
                        .macros().stylesheet(WebDiff.BOOTSTRAP_CSS_URL)
                        .macros().javascript(WebDiff.JQUERY_JS_URL)
                        .macros().javascript(WebDiff.POPPER_JS_URL)
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
                        .a(class_("btn btn-default btn-sm btn-danger").href("/quit")).content("Quit")
                    ._div()
                ._div()
            ._div();
        }
    }
}
