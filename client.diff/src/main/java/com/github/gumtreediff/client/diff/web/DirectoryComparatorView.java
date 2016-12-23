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

package com.github.gumtreediff.client.diff.web;

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

public class DirectoryComparatorView implements Renderable {

    private DirectoryComparator comparator;

    public DirectoryComparatorView(DirectoryComparator comparator) throws IOException {
        this.comparator = comparator;
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html
        .render(DocType.HTML5)
        .html(lang("en"))
            .render(new BootstrapHeaderView())
            .body()
                .div(class_("container"))
                    .div(class_("row"))
                        .div(class_("col-lg-12"))
                            .div(class_("panel panel-default"))
                                .div(class_("panel-heading"))
                                    .h4(class_("panel-title"))
                                        .write("Modified files ")
                                        .span(class_("badge")).content(comparator.getModifiedFiles().size())
                                    ._h4()
                                ._div()
                                .div(class_("panel-body"))
                                    .render_if(new ModifiedFiles(comparator.getModifiedFiles()), comparator.getModifiedFiles().size() > 0)
                                ._div()
                            ._div()
                        ._div()
                    ._div()
                    .div(class_("row"))
                        .div(class_("col-lg-6"))
                            .div(class_("panel panel-default"))
                                .div(class_("panel-heading"))
                                    .h4(class_("panel-title"))
                                        .write("Deleted files ")
                                        .span(class_("badge")).content(comparator.getDeletedFiles().size())
                                    ._h4()
                                ._div()
                                .div(class_("panel-body"))
                                    .render_if(new AddedOrDeletedFiles(
                comparator.getDeletedFiles(), comparator.getSrc(), "danger"),
                comparator.getDeletedFiles().size() > 0)
                                ._div()
                            ._div()
                        ._div()
                        .div(class_("col-lg-6"))
                            .div(class_("panel panel-default"))
                                .div(class_("panel-heading"))
                                    .h4(class_("panel-title"))
                                        .write("Added files ")
                                        .span(class_("badge")).content(comparator.getAddedFiles().size())
                                    ._h4()
                                ._div()
                                .div(class_("panel-body"))
                                    .render_if(new AddedOrDeletedFiles(
                comparator.getAddedFiles(), comparator.getDst(), "success"),
                comparator.getAddedFiles().size() > 0)
                                ._div()
                            ._div()
                        ._div()
                    ._div()
                ._div()
                .render(new BootstrapFooterView())
                .macros().javascript("/dist/list.js")
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
            .table(class_("table table-striped table-condensed"))
                .tbody();

            int id = 0;
            for (Pair<File, File> file : files) {
                tbody
                .tr()
                    .td(class_("col-md-10")).content(comparator.getSrc().relativize(file.getFirst().toPath()).toString())
                    .td(class_("col-md-2"))
                        .a(class_("btn btn-primary btn-xs").href("/diff/" + id)).content("diff")
                        .write(" ")
                        .a(class_("btn btn-primary btn-xs").href("/mergely/" + id)).content("mergely")
                        .write(" ")
                        .a(class_("btn btn-primary btn-xs").href("/script/" + id)).content("script")
                    ._td()
                ._tr();
                id++;
            }
            tbody
                ._tbody()
                ._table();
        }

    }

    private class AddedOrDeletedFiles implements Renderable {

        private Set<File> files;

        private Path root;

        private String tdClass;

        private AddedOrDeletedFiles(Set<File> files, Path root, String tdClass) {
            this.files = files;
            this.root = root;
            this.tdClass = tdClass;
        }

        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            HtmlCanvas tbody = html
            .table(class_("table table-condensed"))
                .tbody();
            for (File file : files) {
                tbody
                    .tr()
                        .td(class_(tdClass)).content(root.relativize(file.toPath()).toString())
                    ._tr();
            }
                tbody
                    ._tbody()
                ._table();
        }
    }

}
