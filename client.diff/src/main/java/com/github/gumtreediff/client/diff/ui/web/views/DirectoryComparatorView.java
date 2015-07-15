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

package com.github.gumtreediff.client.diff.ui.web.views;

import com.github.gumtreediff.tree.Pair;
import com.github.gumtreediff.io.DirectoryComparator;
import com.github.gumtreediff.tree.Pair;
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
                .render(new BootstrapHeader())
                .body()
                .div(class_("container"))
                .div(class_("row"))
                .div(class_("col-lg-12"))
                .div(class_("panel-group").id("modified-files"))
                .div(class_("panel panel-default"))
                .div(class_("panel-heading"))
                .h4(class_("panel-title"))
                .a(href("#collapse-modified-files").add("data-toggle","collapse").add("data-parent","#modified-files")).content(String.format("Modified files (%d)", comparator.getModifiedFiles().size()))
                ._h4()
                ._div()
                .div(id("collapse-modified-files").class_("panel-collapse collapse in"))
                .div(class_("panel-body"))
                .render_if(new ModifiedFiles(comparator.getModifiedFiles()), comparator.getModifiedFiles().size() > 0)
                ._div()
                ._div()
                ._div()
                ._div()
                ._div()
                ._div()
                .div(class_("row"))
                .div(class_("col-lg-6"))
                .div(class_("panel-group").id("deleted-files"))
                .div(class_("panel panel-default"))
                .div(class_("panel-heading"))
                .h4(class_("panel-title"))
                .a(href("#collapse-deleted-files").add("data-toggle","collapse").add("data-parent","#deleted-files")).content(String.format("Deleted files (%d)", comparator.getDeletedFiles().size()))
                ._h4()
                ._div()
                .div(id("collapse-deleted-files").class_("panel-collapse collapse in"))
                .div(class_("panel-body"))
                .render_if(new UnmodifiedFiles(
                                comparator.getDeletedFiles(), comparator.getSrc()),
                                comparator.getDeletedFiles().size() > 0)
                ._div()
                ._div()
                ._div()
                ._div()
                ._div()
                .div(class_("col-lg-6"))
                .div(class_("panel-group").id("added-files"))
                .div(class_("panel panel-default"))
                .div(class_("panel-heading"))
                .h4(class_("panel-title"))
                .a(href("#collapse-added-files").add("data-toggle","collapse").add("data-parent","#added-files")).content(String.format("Added files (%d)", comparator.getAddedFiles().size()))
                ._h4()
                ._div()
                .div(id("collapse-added-files").class_("panel-collapse collapse in"))
                .div(class_("panel-body"))
                .render_if(new UnmodifiedFiles(
                                comparator.getAddedFiles(), comparator.getDst()),
                                comparator.getAddedFiles().size() > 0)
                ._div()
                ._div()
                ._div()
                ._div()
                ._div()
                ._div()
                ._div()
                .macros().javascript("res/web/jquery.min.js")
                .macros().javascript("res/web/bootstrap.min.js")
                .macros().javascript("res/web/list.js")
                ._body()
                ._html();
    }

    public class ModifiedFiles implements Renderable {

        private List<Pair<File, File>> files;

        public ModifiedFiles(List<Pair<File, File>> files) {
            this.files = files;
        }

        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            HtmlCanvas tbody = html
                    .table(class_("table table-striped table-condensed"))
                    .thead()
                    .tr()
                    .th().content("Source file")
                    .th().content("Destination file")
                    .th().content("Action")
                    ._tr()
                    ._thead()
                    .tbody();
            int id = 0;
            for (Pair<File, File> file : files) {
                tbody
                        .tr()
                        .td().content(comparator.getSrc().relativize(file.getFirst().toPath()).toString())
                        .td().content(comparator.getDst().relativize(file.getSecond().toPath()).toString())
                        .td()
                        .a(class_("btn btn-primary btn-xs").href("/fr/labri/gumtree/client/diff" + id)).content("fr/labri/gumtree/client/diff")
                        .write(" ")
                        .a(class_("btn btn-primary btn-xs").href("/script?id=" + id)).content("script")
                        ._td()
                        ._tr();
                id++;
            }
            tbody
                    ._tbody()
                    ._table();
        }
    }

    public class UnmodifiedFiles implements Renderable {

        private Set<File> files;

        private Path root;

        public UnmodifiedFiles(Set<File> files, Path root) {
            this.files = files;
            this.root = root;
        }

        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            HtmlCanvas tbody = html
                    .table(class_("table table-striped table-condensed"))
                    .thead()
                    .tr()
                    .th().content("File")
                    ._tr()
                    ._thead()
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

}
